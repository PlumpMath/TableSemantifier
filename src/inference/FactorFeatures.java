package inference;

import com.google.common.collect.Multimap;
import kb.KB;
import util.Table;
import util.Util;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by vihari on 14/09/16.
 */
public class FactorFeatures {
    Logger log = Logger.getLogger(FactorFeatures.class.getName());
    //The cache of property-value pairs in KB for each id
    //KB id -> multimap of property-value pairs
    Map<String,Multimap<String,String>> cache;
    KB kb;
    Table table;

    /**
     * @param kb - The KB connector object. It will be queried if a factor feature cannot be computed from the cache alone and/or if the id is missing in cache
     * @param table - The table that we are working on*/
    public FactorFeatures(Table table, KB kb){
        this.kb = kb;
        this.table = table;
    }

    public void setCache(Map<String,Multimap<String,String>> cache){
        this.cache = cache;
    }

    private Multimap<String,String> requestDataWithCache(String kbId){
        Multimap<String,String> props = cache.get(kbId);
        if(props==null) {
            //severe because this is not expected to happen
            log.severe("KB Id: "+kbId+" not found in cache, making a request");
            props = kb.getAllFacts(kbId);
            cache.put(kbId, props);
        }
        return props;
    }

    static double computePotential(double[] f, double[] w){
        return Math.exp(IntStream.range(0, f.length).mapToDouble(i -> f[i] * w[i]).sum());
    }


    final static double DEF_VALUE = 1;

    static boolean isNA(String str){
        return TableSemantifier.NA.equals(str);
    }

    /**
     * Computes the factor that connects the disambig. link from cell text - phi(r,c,e_rc)*/
    public double linkPotential(int r, int c, String kbId){
        //other possible features can be based on
        //1. page length, number of words, indicates popularity
        //2. should respect the ranking returned by the suggestions API of the KB
        double[] f = new double[1];
        double[] w = new double[]{1.0};

        //emit 0 when kbId is NA (equivalent of null)
        if(isNA(kbId))
            return DEF_VALUE;

        String cellText = table.getCellText(r,c);
        Multimap<String,String> props = requestDataWithCache(kbId);
        Collection<String> labels = new ArrayList<>();
        Stream.of(kb.getPropertiesOfLabel()).map(props::get).forEach(labels::addAll);

        //No labels => return 0
        if(labels.size()==0) return DEF_VALUE;

        boolean isUserOrTalkPage = labels.stream().filter(s->s.startsWith("User:")|s.startsWith("Talk:")).findAny().isPresent();
        if(isUserOrTalkPage) return 0;

        Collection<String> enLabels = labels.stream().filter(s->s.endsWith("@en")).map(s->s.substring(1,s.length()-3)).collect(Collectors.toList());
        //the score of the label that best matches the cell text
        f[0] = enLabels.stream().mapToDouble(s -> Util.numWordMatch(s, cellText) / (Util.numWords(s) + 1)).max().orElse(0);

        return computePotential(f,w);
    }

    /**
     * Computes the factor that connects the columns tpe - phi(c,t_c)*/
    public double colTypePotential(int c, String type) {
        double[] w = new double[]{1};
        double[] f = new double[1];

        List<String> headers = table.getHeaders();

        if (isNA(type))
            return DEF_VALUE;

        String hc;
        //return 0 if no header
        if (headers == null || headers.size() <= c || (hc = headers.get(c)) == null || hc.length() == 0)
            return DEF_VALUE;

        String[] typeLemmas = kb.generateLemmaOf(type);
        f[0] = Stream.of(typeLemmas).mapToDouble(tl -> Util.numWordMatch(tl, hc) / Util.numWords(tl)).max().orElse(0);

        return computePotential(f,w);
    }

    /**
     * TODO: This feature is specific to how we emit the type from Wikidata. Better if the dependency is removed
     * Potential of the factor connecting the type of the column and the possible resolution of a cell: -- phi(t_c,e_rc)
     * @param type - The type of the column
     * @param id - The KB id of the link from cell to KB resource*/
    public double colTypeLinkPotential(String type, String id){
        double[] w = new double[]{1};
        double[] f = new double[1];

        if(isNA(type)||isNA(id))
            return DEF_VALUE;

        Multimap<String,String> props = requestDataWithCache(id);
        if(props==null) {
            log.severe("Something's wrong! Cannot fetch properties of KB id: "+id);
            return DEF_VALUE;
        }

        //because a type defined by Wikidata is prop and value appended by ":::"
        String[] fs = type.split(":::");
        String prop = fs[0], value = fs[1];
        if(!props.containsEntry(prop,value))
            return DEF_VALUE;

        int ne = kb.getNumberOfEntitiesOfType(type);
        log.info("Num Entities of type: "+type+" is "+ne);

        if(ne>=100)
            f[0] = 1.0/Math.log(kb.getNumberOfEntitiesOfType(type));
        else f[0] = 0;

        return computePotential(f,w);
    }

    /**
     * Potential of the factor connecting the type of two columns and the binary relation they share -- phi(b_cc',t_c,t_c')
     * @param type1 - type of column 1
     * @param type2 - type of column 2
     * @param rel - binary relation that column 1 and 2 are likely to share
     * */
    public double relTypeColsPotential(String type1, String type2, String rel){
        double[] w = new double[]{1};
        double[] f = new double[1];

        if(isNA(type1) || isNA(type2) || isNA(rel))
            return DEF_VALUE;

        //number of entities of type1 that share the relation rel with entities of type2
        double inter = kb.getIntersectionOfTypesWithRel(type1,type2,rel);
        f[0] = inter/((kb.getNumberOfEntitiesOfType(type1)+1)*(kb.getNumberOfEntitiesOfType(type2)+1));
        return computePotential(f,w);
    }

    /**
     * The potential of the factor connecting the links of two cells in the same row and the binary relation the two columns share*/
    public double relLinkPotential(String id1,String id2, String rel){
        if(isNA(id1)||isNA(id2)||isNA(rel))
            return DEF_VALUE;

        double[] f = new double[1];
        double[] w = new double[]{1};

        Multimap<String,String> props1 = requestDataWithCache(id1);
        if(props1.containsEntry(rel,id2))
            f[0] = 1;
        else f[0] = 0;

        return computePotential(f,w);
    }

    public static void main(String[] args){
        System.out.println(computePotential(new double[]{1},new double[]{1})==Math.exp(1));
    }
}
