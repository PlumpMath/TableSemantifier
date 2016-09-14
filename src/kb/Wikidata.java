package kb;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vihari on 12/09/16.
 *
 * TODO: Handle the corner cases and Exception handling
 * TODO: Make a proper use of log levels
 */
public class Wikidata implements KB {
    static Logger log = Logger.getLogger(Wikidata.class.getName());
    static{
        log.setLevel(Level.FINEST);
    }
    static String API_ENDPINT = "https://www.wikidata.org/w/api.php";

    static class Suggestions{
        Query query;
    }
    static class Query{
        List<WikidataId> search;
        int totalhits;
    }
    static class WikidataId {
        //databaseid such as Q3345479
        String title;
        int size, wordcount;
        Date timestamp;
        String getId(){
            //ID is the full URI of the resource
            return "http://www.wikidata.org/entity/"+title;
        }
    }
    //limit value more than 500 will be truncated to 500.
    public String[] resolveEntity(String text, int limit) {
        /**Using wbgetentities module to fetch suggestions is straightforward.
         * https://www.wikidata.org/w/api.php?action=help&modules=wbgetentities
         * But it is surprising limiting; for example if you search for "Dhoni" then only entities that start with entities are suggested and hence missing on MS Dhoni
         * A better alternative is query action with list as search
         * https://www.wikidata.org/w/api.php?action=help&modules=query%2Bsearch
         * https://www.wikidata.org/w/api.php?action=query&list=search&srsearch=sehwag
         * This API searches the full text to and gives the result.
         * 1. Searching the full text could be slow and high volume. Luckily the API ranks the title match hits higher; be cautious not to set the limit too high. Search over title only option of this API does not work.
         * 2. The search also returns hits from Wikinews, Templates and User pages. It is possible to look at snippets and remove such err. pages to some extent.
         * Another alternative is a GET over Special:Search
         * https://www.wikidata.org/w/index.php?title=Special:Search&profile=default&fulltext=Search&search=shahrukh+khan;
         */
        log.info("Resolving text: " + text);
        URL url = null;
        try {
            URI uri = new URI("https",
                    "wikidata.org",
                    "/w/api.php",
                    "action=query&list=search&srwhat=text&srsearch=" + text + "&srlimit=" + limit + "&format=json",
                    null
            );
            url = uri.toURL();
        }catch(URISyntaxException|MalformedURLException ue){
            ue.printStackTrace();
        }
        Gson gson = new Gson();
        Suggestions sgsts = null;
        try {
            //if(log.isLoggable(Level.FINE))
            log.info("Fetching content from "+url.toString());
            sgsts = gson.fromJson(new InputStreamReader(url.openStream()), Suggestions.class);
        }catch(IOException me){
            me.printStackTrace();
            log.info(me.getMessage());
        }
        if(sgsts!=null && sgsts.query.search!=null) {
            List<String> ids = sgsts.query.search.stream().map(WikidataId::getId).collect(Collectors.toList());
            return ids.toArray(new String[ids.size()]);
        }
        return new String[0];
    }

    public String[] getAllAttributes(String id) {
        Set<String> props = getAllFacts(id).keySet();
        return props.toArray(new String[props.size()]);
    }

    static class WikidataItem{
        String type, value;
        //dealing with fields with colon in it --
        //http://stackoverflow.com/questions/14820212/create-json-using-gson-with-a-colon-as-part-of-a-fields-name
        @SerializedName("xml:lang")
        String xmllang;
        String datatype;
        //extracts the short value if the type of item is URI
        //for example http://www.wikidata.org/prop/direct/P31 ==> P31
        String getText(){
            if(type.equals("literal")) {
                if(datatype!=null)
                    value = "\""+value+"\"^^"+datatype;
                else if(xmllang!=null)
                    value = "\""+value+"\"@"+xmllang;
            }
            return value;
        }
    }
    static class SPARQLFactsResult {
        static class Results{
            static class PropValuePair{
                WikidataItem property, value;
            }
            List<PropValuePair> bindings;
        }
        Results results;
    }

    public Multimap<String, String> getAllFacts(String id) {
        //if(log.isLoggable(Level.FINE))
        log.info("Getting all facts of: "+id);

        String q = "SELECT ?property ?value " +
                "WHERE" +
                "{" +
                "<"+id+"> ?property ?value . " +
                "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }" +
                "}";
        URL url = null;
        //String url = SPARL_ENDPOINT+"?query="+q+"&format=json";
        try {
            URI uri = new URI(
                    "https",
                    "query.wikidata.org",
                    "/bigdata/namespace/wdq/sparql",
                    "query="+q+"&format=json",
                    null);
            url = uri.toURL();
        }catch(URISyntaxException|MalformedURLException ue){
            ue.printStackTrace();
        }
        Gson gson = new Gson();
        Multimap<String,String> map = LinkedHashMultimap.create();
        try {
            //if(log.isLoggable(Level.FINE))
                log.info("Fetching content from: "+url.toString());
//            InputStream is = (InputStream)url.getContent();
//            BufferedReader br = new BufferedReader(new InputStreamReader(is));
//            String line;
//            String str = "";
//            while((line=br.readLine())!=null)
//                str += line;
//            //log.info(str);
            SPARQLFactsResult results = gson.fromJson(new InputStreamReader(url.openStream()), SPARQLFactsResult.class);
            results.results.bindings.stream().forEach(p->map.put(p.property.value,p.value.getText()));
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public int getNumberOfEntitiesOfType(String type) {
        log.info("Getting type set size of type: " + type);
        String[] fs = type.split(":::");
        String prop = fs[0], value = fs[1];
        String q = "SELECT (COUNT(?p) AS ?value) WHERE { \n" +
                "  ?p <"+prop+"> <"+value+">. \n" +
                "}";
        URL url = null;
        try {
            URI uri = new URI(
                    "https",
                    "query.wikidata.org",
                    "/bigdata/namespace/wdq/sparql",
                    "query=" + q + "&format=json",
                    null);
            url = uri.toURL();
        }catch(URISyntaxException|MalformedURLException ue){
            ue.printStackTrace();
        }
        try {
            Gson gson = new Gson();
            //if(log.isLoggable(Level.FINE))
                log.info("Fetching content from: "+url.toString());
            SPARQLFactsResult results = gson.fromJson(new InputStreamReader(url.openStream()), SPARQLFactsResult.class);
            return Integer.parseInt(results.results.bindings.get(0).value.value);
        }catch(IOException ie){
            ie.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) {
        KB kb = new Wikidata();
        String[] sgsts = kb.resolveEntity("Harry Potter", 20);
        Stream.of(sgsts).forEach(System.out::println);
        kb.getAllFacts("http://www.wikidata.org/entity/Q34660").entries().stream().map(e->e.toString()).forEach(log::info);
        //number of instances of type human

        //log.info(kb.getNumberOfEntitiesOfType("http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q5")+"");
        //number of arjuna award winners
//        log.info(kb.getNumberOfEntitiesOfType("P166:::Q671622")+"");

    }
}
