package inference;
import cc.mallet.grmm.inference.Inferencer;
import cc.mallet.grmm.inference.LoopyBP;
import cc.mallet.grmm.types.*;
import com.google.common.collect.Multimap;
import kb.KB;
import kb.Wikidata;
import opennlp.tools.util.Span;
import util.Table;
import util.Util;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Loosely based on WWT: VLDB'10 research that came out of IITB
 * Given data in the form of table (only one column and two column tables are handled), this class adds semantics to such a table.
 * Each column is annotated with a description, relevant nouns in each cell is disambiguated to a KB and the binary relation (if any) is added between the columns
 * Example 1:
 * Input: ****************
 *        |     Sachin   |
 *        |      Viru    |
 *        |Mr. Dependable|
 *        |      Yuvi    |
 *        |     Dhoni    |
 *        ****************
 * Output: Type: Cricketers that play for India, and the table should now be
 *       ********************
 *       | Sachin Tendulkar |
 *       | Virender Sehwag  |
 *       |   Rahul Dravid   |
 *       |   Yuvraj Singh   |
 *       |     MS Dhoni     |
 *       ********************
 *
 * Example 2:
 * Input: **************|*********************
 *        |   Rowling   | Harry Potter       |
 *        |   Tolkien   | Lord of the Rings  |
 *        |   Martin    | Game of Thrones    |
 *        |   Dickens   | Great Expectations |
 *        |   Mark      | Huckleberry Finn   |
 *        |   Leo       | War and Peace      |
 *        | Conan Doyle | Sherlock Holmes    |
 *        |   William   | Hamlet             |
 *        |   Dante     | Inferno            |
 *        **************|*********************
 * This input annotates the first column with writers, second with Book/series
 * each of the author/book in first column and second should be expanded to their fullest form (a human can easily guess)
 * and the binary relation between the two columns is written by/authored by
 *
 * The cell text can be free text and need not include only entities.
 * For example the text in first row, first column is "I like red color" and text in second column is "Stacy likes blue".
 * The case will be handled to mark the type of the column as color.
 */
public class TableSemantifier {
    static Logger log = Logger.getLogger(TableSemantifier.class.getName());
    static{
        log.setLevel(Level.FINEST);
    }
    static String NA = "NA";

    Table table;
    //E_rc,T_c,B_cc'
    List<List<List<String>>> Erc;
    //list of top ranking candidate types
    List<List<String>> Tc;
    //list of top ranking candidate relations
    List<List<String>> Bcc;

    //This is cache of property of every id that is queried in KB
    Map<String,Multimap<String,String>> allProps = new LinkedHashMap<>();
    KB kb;

    static boolean DEBUG_MODE = true;
    TableSemantifier(Table input) {
        table = input;
        //initialize each of the data elements to the size of the table
        Erc = new ArrayList<>();
        IntStream.range(0,input.numrows()).forEach(r-> {
            Erc.add(new ArrayList<>());
            IntStream.range(0, table.numcols())
                    .forEach(c -> {
                        List<String> lst = new ArrayList<>();
                        lst.add(NA);
                        Erc.get(r).add(lst);
                    });
        });
        Tc = new ArrayList<>();
        //we consider binary relation of one column with the one next to it
        Bcc = new ArrayList<>();
        IntStream.range(0,input.numcols()).forEach(c-> {
            List<String> lst1 = new ArrayList<>(), lst2 = new ArrayList<>();
            lst1.add(NA);lst2.add(NA);
            if(c!=table.numcols()-1)
                Bcc.add(lst1);
            Tc.add(lst2);
        });

        if(DEBUG_MODE)
            kb = Wikidata.initializeFromCache();
        else kb = new Wikidata();
    }

    //copied from TestInference.java in GRMM
    private Factor[] collectAllMarginals (FactorGraph mdl, Inferencer alg)
    {
        int vrt = 0;
        int numVertices = mdl.numVariables ();
        Factor[] collector = new Factor[numVertices];
        for (Iterator it = mdl.variablesSet ().iterator();
             it.hasNext();
             vrt++) {
            Variable var = (Variable) it.next();
            try {
                collector[vrt] = alg.lookupMarginal(var);
                assert collector [vrt] != null
                        : "Query returned null for model " + mdl + " vertex " + var + " alg " + alg;
            } catch (UnsupportedOperationException e) {
                // Allow unsupported inference to slide with warning
                log.warning("Warning: Skipping model " + mdl + " for alg " + alg
                        + "\n  Inference unsupported.");
            }
        }
        return collector;
    }

    public void semantify() {
        fetchCandidateResolutions();
        //we need random variable for each cell in the table, each column type and numcol-1 binary relations the columns may share
        Variable[] allVars = new Variable[table.numcols() * table.numrows() + table.numcols() + table.numcols() - 1];
        for (int r = 0; r < table.numrows(); r++)
            for (int c = 0; c < table.numcols(); c++)
                allVars[r * table.numcols() + c] = new Variable(Erc.get(r).get(c).size());
        int k = table.numcols() * table.numrows();
        for (int c = 0; c < table.numcols(); c++)
            allVars[k++] = new Variable(Tc.get(c).size());
        for (int c = 0; c < table.numcols() - 1; c++)
            allVars[k++] = new Variable(Bcc.get(c).size());

        int tableSize = table.numrows() * table.numcols();

        FactorGraph fg = new FactorGraph(allVars);

        FactorFeatures fgen = new FactorFeatures(table, kb);
        fgen.setCache(allProps);

        //potential over e rvs
        //factors over each of the e variable and type variable - phi1
        IntStream.range(0, table.numrows())
                .forEach(r -> IntStream.range(0, table.numcols())
                        .forEach(c -> {
                            double[] vals = Erc.get(r).get(c).stream().mapToDouble(kbId -> fgen.linkPotential(r, c, kbId)).toArray();
                            fg.addFactor(new TableFactor(allVars[r * table.numcols() + c], vals));
                        }));

        //factors over each of column types - phi2
        IntStream.range(0, table.numcols())
                .forEach(c -> {
                    double[] vals = Tc.get(c).stream().mapToDouble(tc -> fgen.colTypePotential(c, tc)).toArray();
                    fg.addFactor(new TableFactor(allVars[tableSize + c], vals));
                });

        //joint factors over link variable and column type variable - phi3
        IntStream.range(0, table.numrows())
                .forEach(r -> IntStream.range(0, table.numcols())
                        .forEach(c -> {
                            Variable tvar = allVars[tableSize + c];
                            Variable evar = allVars[r * table.numcols() + c];
                            double[] vals = new double[Tc.get(c).size() * Erc.get(r).get(c).size()];
                            IntStream.range(0, Tc.get(c).size())
                                    .forEach(ti -> IntStream.range(0, Erc.get(r).get(c).size())
                                            .forEach(ei ->
                                                            vals[ti * Erc.get(r).get(c).size() + ei] = fgen.colTypeLinkPotential(Tc.get(c).get(ti), Erc.get(r).get(c).get(ei))
                                            ));
                            fg.addFactor(tvar, evar, vals);
                        }));

        //joint factor over column types and their binary relations - phi4
        //relTypeColsPotential accesses network to compute features, hence parallelized
        IntStream.range(0, table.numcols() - 1)
                .forEach(c -> {
                    Variable tvar1 = allVars[tableSize + c];
                    Variable tvar2 = allVars[tableSize + c + 1];
                    Variable brvar = allVars[tableSize + table.numcols() + c];
                    double[] vals = new double[Tc.get(c).size() * Tc.get(c + 1).size() * Bcc.get(c).size()];
                    IntStream.range(0, Tc.get(c).size())
                            .parallel()
                            .forEach(ti -> IntStream.range(0, Tc.get(c + 1).size())
                                    .parallel()
                                    .forEach(ti2 -> IntStream.range(0, Bcc.get(c).size())
                                            .parallel()
                                            .forEach(bi ->
                                                    vals[ti * Tc.get(c + 1).size() * Bcc.get(c).size() + ti2 * Bcc.get(c).size() + bi]
                                                            = fgen.relTypeColsPotential(Tc.get(c).get(ti), Tc.get(c + 1).get(ti2), Bcc.get(c).get(bi)))));
                    fg.addFactor(new TableFactor(new Variable[]{tvar1, tvar2, brvar}, vals));
                });

        //joint factor over link vars and binary relation - phi5
        IntStream.range(0, table.numcols() - 1)
                .forEach(c -> {
                    Variable brvar = allVars[tableSize + table.numcols() + c];
                    IntStream.range(0, table.numrows())
                            .forEach(r -> {
                                Variable er1 = allVars[r * table.numcols() + c];
                                Variable er2 = allVars[r * table.numcols() + c + 1];
                                double[] vals = new double[Erc.get(r).get(c).size() * Erc.get(r).get(c + 1).size() * Bcc.get(c).size()];
                                IntStream.range(0, Erc.get(r).get(c).size())
                                        .forEach(e1 -> IntStream.range(0, Erc.get(r).get(c + 1).size())
                                                .forEach(e2 -> IntStream.range(0, Bcc.get(c).size())
                                                        .forEach(bi ->
                                                                vals[e1 * Erc.get(r).get(c + 1).size() * Bcc.get(c).size() + e2 * Bcc.get(c).size() + bi]
                                                                        = fgen.relLinkPotential(Erc.get(r).get(c).get(e1), Erc.get(r).get(c + 1).get(e2), Bcc.get(c).get(bi)
                                                                ))
                                                ));
                                fg.addFactor(new TableFactor(new Variable[]{er1, er2, brvar}, vals));
                            });
                });

        //cache the results
        if(DEBUG_MODE)
            ((Wikidata)kb).writeSerialized();

        Inferencer inf = LoopyBP.createForMaxProduct();
        inf.computeMarginals(fg);
        Factor[] facts = collectAllMarginals(fg,inf);
        Stream.of(facts).forEach(f->log.info(f.prettyOutputString()+"-"+f.dumpToString()));
        IntStream.range(0,facts.length)
                .forEach(fi->{
                    Factor f = facts[fi];
                    double[] dvs = ((AbstractTableFactor) f).getValues();
                    Double[] vals = new Double[dvs.length];
                    IntStream.range(0,dvs.length).forEach(di->vals[di]=dvs[di]);
                    int[] idxs = Util.getIndicesInSortedArray(vals);
                    if(fi<tableSize) {
                        int r = fi/table.numcols();
                        int c = fi%table.numcols();
                        table.setLinksOf(r, c, IntStream.of(idxs).boxed().limit(5).map(Erc.get(r).get(c)::get).map(s->kb.getLabelOf(s)+"("+s+")").collect(Collectors.toList()));
                    }
                    else if(fi<tableSize+table.numcols()) {
                        int c = fi-tableSize;
                        table.setTypesOf(c, IntStream.of(idxs).boxed().limit(5).map(Tc.get(c)::get).map(t->{
                            String fs[] = t.split(":::");
                            String lprop = "", lval = "";
                            if(fs[0].startsWith("http://"))
                                lprop = kb.getLabelOf(fs[0]);
                            if(fs[1].startsWith("http://"))
                                lval = kb.getLabelOf(fs[1]);
                            return lprop+"("+fs[0]+"):::"+lval+"("+fs[1]+")";
                        }).collect(Collectors.toList()));
                    }
                    else{
                        int c = fi-(tableSize+table.numcols());
                        table.setRelsOf(c, IntStream.of(idxs).boxed().limit(5).map(Bcc.get(c)::get).map(s->kb.getLabelOf(s)+"("+s+")").collect(Collectors.toList()));
                    }
                });
        log.info(table.prettyPrint());
    }

    void fetchCandidateResolutions() {
        int LIMIT = 5;
        IntStream.range(0, table.numcols())
                .parallel()
                .forEach(c -> IntStream.range(0, table.numrows())
                                .parallel()
                                .forEach(r -> {
                                    String text = table.getCellText(r, c);
                                    Span[] nouns = new Span[]{new Span(0, text.length())};//NLPUtils.getNounChunks();
                                    List<String> resols = new ArrayList<>();
                                    Stream.of(nouns)
                                            .map(sp -> text.substring(sp.getStart(), sp.getEnd()))
                                            .forEach(noun -> {
                                                String[] ids = kb.resolveEntity(noun, LIMIT);
                                                Stream.of(ids).forEach(resols::add);
                                            });
                                    Erc.get(r).get(c).addAll(resols);
                                })
                );

        //only the top few candidates with good support are generated
        List<Map<String, Integer>> typeMap = new ArrayList<>(table.numcols());
        List<Map<String, Integer>> relMap = new ArrayList<>(table.numcols());
        IntStream.range(0,table.numcols()).forEach(c->{
            if(c!=table.numcols()-1)
                relMap.add(new LinkedHashMap<>());
            typeMap.add(c, new LinkedHashMap<>());
        });
        //mapping from the KB id to its full properties
        //accumulate type and relation candidates
        IntStream.range(0, table.numrows())
                .parallel()
                .forEach(r -> IntStream.range(0, table.numcols())
                                .parallel()
                                .forEach(c -> Erc.get(r).get(c)
                                        .stream()
                                        .parallel()
                                        .forEach(id -> {
                                            Multimap<String, String> propValues = kb.getAllFacts(id);
                                            allProps.put(id,propValues);
                                            propValues.entries().stream()
                                                    .map(e -> e.getKey() + ":::" + e.getValue())
                                                    .forEach(type -> typeMap.get(c).put(type, typeMap.get(c).getOrDefault(type, 0) + 1));
                                            if (c != table.numcols() - 1) {
                                                propValues.entries().stream()
                                                        .filter(e -> Erc.get(r).get(c + 1).contains(e.getValue()))
                                                        .forEach(e -> relMap.get(c).put(e.getKey(), relMap.get(c).getOrDefault(e.getKey(), 0) + 1)
                                                        );
                                            }
                                        }))
                );

        IntStream.range(0, table.numcols())
                .forEach(c -> {
                    if (c != table.numcols() - 1) {
                        Map<String, Integer> srelMap = Util.sortMapByValue(relMap.get(c));
                        srelMap.entrySet().stream().limit(2*LIMIT).forEach(e -> Bcc.get(c).add(e.getKey()));
                        //if(log.isLoggable(Level.FINE)) {
                        log.info("Candidate binary relations between column " + c + " and " + (c + 1));
                        srelMap.entrySet().stream().map(Object::toString).limit(20).forEach(log::info);
                        //}
                    }
                    Map<String, Integer> stm = Util.sortMapByValue(typeMap.get(c));
                    stm.entrySet().stream().limit(2*LIMIT).forEach(e -> Tc.get(c).add(e.getKey()));

                    //if(log.isLoggable(Level.FINE)) {
                    log.info("Candidate types for column " + c);
                    stm.entrySet().stream().map(Object::toString).limit(20).forEach(log::info);
                    //}
                });
    }

    public static void main(String[] args) {
        Table books = new Table(2);
        books.addRow(new String[]{"JK Rowling", "Harry Potter"});
        books.addRow(new String[]{"Tolkien", "Lord of the Rings"});
        books.addRow(new String[]{"Martin", "Game of Thrones"});
        books.addRow(new String[]{"Dickens", "Great Expectations"});
        books.addRow(new String[]{"Mark", "Huckleberry Finn"});
        books.addRow(new String[]{"Leo", "War and Peace"});
        books.addRow(new String[]{"Conan Doyle", "Sherlock Holmes"});
        books.addRow(new String[]{"William", "Hamlet"});
        books.addRow(new String[]{"Dante", "Inferno"});

        Table greek = new Table(1);
        greek.addRow(new String[]{"Alpha"});
        greek.addRow(new String[]{"Beta"});
        greek.addRow(new String[]{"Gamma"});

        Table cricket = new Table(1);
        cricket.addRow(new String[]{"Sachin"});
        cricket.addRow(new String[]{"Sehwag"});
        cricket.addRow(new String[]{"Mr. Dependable"});
        cricket.addRow(new String[]{"Yuvi"});
        cricket.addRow(new String[]{"Dhoni"});

        long st = System.currentTimeMillis();
        TableSemantifier semantifier = new TableSemantifier(books);
        semantifier.semantify();
        //semantifier.fetchCandidateResolutions();
        log.info("Time elapsed: " + (System.currentTimeMillis() - st) + "ms");
    }
}
