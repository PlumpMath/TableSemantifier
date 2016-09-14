package util;

import java.util.*;
import java.util.stream.Collectors;
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

    //returns the number of exact word matches between a and b
    public static int numWordMatch(String a, String b){
        if(a==null||b==null||a.length()==0||b.length()==0)
            return 0;
        String[] aWords = a.split("\\s+"), bWords = b.split("\\s+");
        return Stream.of(aWords).filter(Arrays.asList(bWords)::contains).collect(Collectors.toList()).size();
    }

    public static void main(String[] args){
        Map<String,Integer> map = new HashMap<>();
        map.put("Vihari",450);map.put("Someone",120);map.put("hello",3000);
        System.out.println(sortMapByValue(map));
    }
}
