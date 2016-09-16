package util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by vihari on 14/09/16.
 */
public class Util {
    //sorts values in ascending to descending order
    public static <K, V extends Comparable<? super V>> Map<K,V> sortMapByValue(Map<K,V> map){
        Map<K,V> res = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEachOrdered(e -> res.put(e.getKey(), e.getValue()));
        return res;
    }

    public static int numWords(String a){
        if(a == null || a.length()==0)
            return 0;
        return a.split("\\s+").length;
    }

    static List<String> stopWords = Arrays.asList("but", "be", "with", "such", "then", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "there", "in", "that", "they", "was", "it", "an", "the", "as", "at", "these", "to", "of","is" );
    /**
     * Weeds out stop words in the supplied text*/
    public static String[] filterSWs(String[] words){
        List<String> arr = Stream.of(words).filter(w -> !stopWords.contains(w)).collect(Collectors.toList());
        return arr.toArray(new String[arr.size()]);
    }

    //returns the number of exact word matches between a and b
    public static int numWordMatch(String a, String b){
        if(a==null||b==null||a.length()==0||b.length()==0)
            return 0;
        a = a.toLowerCase();b=b.toLowerCase();
        String[] aWords = filterSWs(a.split("\\W+")), bWords = filterSWs(b.split("\\W+"));
        return Stream.of(aWords).filter(Arrays.asList(bWords)::contains).collect(Collectors.toList()).size();
    }

    //The indices correspond to values from large to small values
    public static <T extends Comparable<? super T>> int[] getIndicesInSortedArray(T[] arr){
        return IntStream.range(0,arr.length).boxed().sorted((i, j) -> -arr[i].compareTo(arr[j])).mapToInt(Integer::intValue).toArray();
    }

    public static void main(String[] args){
        Map<String,Integer> map = new HashMap<>();
        map.put("Vihari",450);map.put("Someone",120);map.put("hello",3000);
        System.out.println(sortMapByValue(map));

        int[] idx = getIndicesInSortedArray(new Double[]{100.0,20.0,-0.2,0.2,5.0});
        for(int i=0;i<idx.length;i++)
            System.out.print(idx[i]+",");
        System.out.println();

        System.out.println(numWordMatch("hello is the some", "is the mac") == 0);

        System.out.println(numWordMatch("\"Indian cricketer\"@en=2","cricketer india"));
    }
}
