package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static void main(String[] args){
        Map<String,Integer> map = new HashMap<>();
        map.put("Vihari",450);map.put("Someone",120);map.put("hello",3000);
        System.out.println(sortMapByValue(map));
    }
}
