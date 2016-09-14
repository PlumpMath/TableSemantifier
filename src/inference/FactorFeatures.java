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

    static double computePotential(double[] f, double[] w){
        return Math.exp(IntStream.range(0, f.length).mapToDouble(i -> f[i] * w[i]).sum());
    }

    /**
     * Computes the factor that connects the disambig. link from cell text - phi(r,c,e_rc)*/
    public double linkPotential(int r, int c, String kbId){
        //other possible features can be based on
        //1. page length, number of words, indicates popularity
        //2. should respect the ranking returned by the suggestions API of the KB
        double[] f = new double[1];
        double[] w = new double[]{1.0};
        String cellText = table.getCellText(r,c);
        Multimap<String,String> props = cache.get(kbId);
        if(props!=null) {
            log.severe("KB Id: "+kbId+" not found in cache, making a request");
            props = kb.getAllFacts(kbId);
            cache.put(kbId, props);
        }
        Collection<String> labels = new ArrayList<>();
        Stream.of(kb.getPropertiesOfLabel()).map(props::get).forEach(labels::addAll);

        //No labels => return 0
        if(labels.size()==0) return 0;

        boolean isUserOrTalkPage = labels.stream().filter(s->s.startsWith("User:")|s.startsWith("Talk:")).findAny().isPresent();
        if(isUserOrTalkPage) return 0;

        Collection<String> enLabels = labels.stream().filter(s->s.endsWith("@en")).map(s->s.substring(1,s.length()-3)).collect(Collectors.toList());
        //the score of the label that best matches the cell text
        f[0] = enLabels.stream().mapToDouble(s -> Util.numWordMatch(s, cellText) / (Util.numWords(s) + 1)).max().orElse(0);
        return computePotential(f,w);
    }

    /**
     * Computes the factor that connects the columns tpe - phi(c,t_c)*/
    public double colTypePotential(int c, String type){
        return 0;
    }

    /**
     * Potential of the factor connecting the type of the column and the possible resolution of a cell: -- phi(t_c,e_rc)*/
    public double colTypeLinkPotential(String type, String id){
        return 0;
    }

    /**
     * Potential of the factor connecting the type of two columns and the binary relation they share -- phi(b_cc',t_c,t_c')*/
    public double relTypeColsPotential(String type1, String type2, String rel){
        return 0;
    }

    /**
     * The potential of the factor connecting the links of two cells in the same row and the binary relation the two columns share*/
    public double relLinkPotential(String id1,String id2, String rel){
        return 0;
    }
}
