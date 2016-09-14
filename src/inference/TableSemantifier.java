package inference;
import cc.mallet.grmm.types.FactorGraph;
import cc.mallet.grmm.types.TableFactor;
import cc.mallet.grmm.types.Variable;
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

    Table table;
    //E_rc,T_c,B_cc'
    List<List<List<String>>> Erc;
    //list of top ranking candidate types
    List<List<String>> Tc;
    //list of top ranking candidate relations
    List<List<String>> Bcc;

    //This is cache of property of every id that is queried in KB
    Map<String,Multimap<String,String>> allProps = new LinkedHashMap<>();

    TableSemantifier(Table input) {
        table = input;
        //initialize each of the data elements to the size of the table
        Erc = new ArrayList<>();
        IntStream.range(0,input.numrows()).forEach(r-> {
            Erc.add(new ArrayList<>());
            IntStream.range(0, table.numcols())
                    .forEach(c -> {
                        List<String> lst = new ArrayList<>();
                        lst.add("NA");
                        Erc.get(r).add(lst);
                    });
        });
        Tc = new ArrayList<>();
        //we consider binary relation of one column with the one next to it
        Bcc = new ArrayList<>();
        IntStream.range(0,input.numcols()).forEach(c-> {
            List<String> lst1 = new ArrayList<>(), lst2 = new ArrayList<>();
            lst1.add("NA");lst2.add("NA");
            if(c!=table.numcols()-1)
                Bcc.add(lst1);
            Tc.add(lst2);
        });
    }

    public void semantify(){
        fetchCandidateResolutions();
        //we need random variable for each cell in the table, each column type and numcol-1 binary relations the columns may share
        Variable[] allVars = new Variable[table.numcols()*table.numrows()+table.numcols()+table.numcols()-1];
        for(int r=0;r<table.numrows();r++)
            for(int c=0;c<table.numcols();c++)
                allVars[r*table.numcols()+c]=new Variable(Erc.get(r).get(c).size());
        int k=table.numcols()*table.numrows();
        for(int c=0;c<table.numcols();c++)
            allVars[k++] = new Variable(Tc.get(c).size());
        for(int c=0;c<table.numcols()-1;c++)
            allVars[k++] = new Variable(Tc.get(c).size());

        FactorGraph mdl = new FactorGraph (allVars);
        //factors over each of the e variable and type variable
        for(int r=0;r<table.numrows();r++)
            for(int c=0;c<table.numcols();c++) {

                mdl.addFactor(new TableFactor(allVars[r*table.numcols()+c],new double[0]));
            }
    }

    void fetchCandidateResolutions() {
        KB kb = new Wikidata();
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
                                                log.info("Prop values for: " + id + " -- " + propValues.values());
                                                log.info("Next col. candidates: " + Erc.get(r).get(c + 1));
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
        long st = System.currentTimeMillis();
        TableSemantifier semantifier = new TableSemantifier(books);
        semantifier.fetchCandidateResolutions();
        log.info("Time elapsed: " + (System.currentTimeMillis() - st) + "ms");
    }
}
