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
public class Wikidata implements KB, Serializable {
    static Logger log = Logger.getLogger(Wikidata.class.getName());
    static String tmpDir = System.getProperty("user.home")+File.separator+"cache";
    static{
        log.setLevel(Level.FINEST);
        if(!new File(tmpDir).exists())
            new File(tmpDir).mkdir();
    }

    static String serFileName = "Wikidata.ser";
    public static Wikidata initializeFromCache(){
        String tmpFile = tmpDir + File.separator + serFileName;
        if(new File(tmpFile).exists()){
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tmpFile));
                Wikidata obj = (Wikidata)ois.readObject();
                ois.close();
                log.info("Successfully read Wikidata object from: "+tmpFile);
                return obj;
            }catch(IOException|ClassNotFoundException e){
                e.printStackTrace();
                log.info("Could not read cache from: "+tmpFile);
            }
        }
        return new Wikidata();
    }

    public void writeSerialized() {
        String tmpFile = tmpDir + File.separator + serFileName;
        if (!new File(tmpFile).exists())
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile));
                oos.writeObject(this);
                oos.close();
                log.info("An instance of the Wikidata object written to: " + tmpFile);
            } catch (IOException e) {
                log.severe("Error while writing the KB object to: " + tmpFile);
                e.printStackTrace();
            }
    }

    Map<String,Object> cache;
    public Wikidata(){
        cache = new LinkedHashMap<>();
    }

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
        String uid = "re-";
        String key = uid+text;
        if(cache.containsKey(key)) return (String[])cache.get(key);

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
            String[] res = ids.toArray(new String[ids.size()]);
            cache.put(key,res);
            return res;
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
        String getUnannotatedText(){
            return value;
        }
    }
    static class SPARQLResult {
        static class Results{
            static class PropValuePair{
                WikidataItem property, value;
            }
            List<PropValuePair> bindings;
        }
        Results results;
    }

    private static SPARQLResult SPARQLQuery(String query) throws URISyntaxException,MalformedURLException,IOException {
        URL url;
        URI uri = new URI(
                "https",
                "query.wikidata.org",
                "/bigdata/namespace/wdq/sparql",
                "query=" + query + "&format=json",
                null);
        url = uri.toURL();

        Gson gson = new Gson();
        //if(log.isLoggable(Level.FINE))
        log.info("Fetching content from: " + url.toString());
        return gson.fromJson(new InputStreamReader(url.openStream()), SPARQLResult.class);
    }

    public Multimap<String, String> getAllFacts(String id) {
        String uid = "gaf-";
        String key = uid+id;
        if(cache.containsKey(key))
            return (Multimap<String,String>)cache.get(key);

        //if(log.isLoggable(Level.FINE))
        log.info("Getting all facts of: " + id);

        String q = "SELECT ?property ?value " +
                "WHERE" +
                "{" +
                "<" + id + "> ?property ?value . " +
                "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }" +
                "}";

        Multimap<String, String> map = LinkedHashMultimap.create();
        try {
            SPARQLResult results = SPARQLQuery(q);
            results.results.bindings.stream().forEach(p -> map.put(p.property.value, p.value.getText()));
            cache.put(key,map);
            return map;
        } catch (IOException|URISyntaxException e) {
            log.warning("Query for properties of id: "+id+" failed either due to an ill-formed query or during parsing.\n Message from exception is: "+e.getMessage());
            e.printStackTrace();
        }
        return map;
    }

    public int getNumberOfEntitiesOfType(String type) {
        String uid="gnoeot-";
        String key = uid + type;
        if(cache.containsKey(key))
            return (Integer)cache.get(key);

        log.info("Getting number of entities that share the type: " + type);
        String[] fs = type.split(":::");
        String prop = fs[0], value = fs[1];
        String q = "SELECT (COUNT(?p) AS ?value) WHERE { \n" +
                "  ?p <"+prop+"> "+(value.startsWith("http://")?("<"+value+">"):(value))+". \n" +
                "}";
        try {
            SPARQLResult results = SPARQLQuery(q);
            Integer res = Integer.parseInt(results.results.bindings.get(0).value.value);
            cache.put(key,res);
            return res;
        } catch(IOException|URISyntaxException e) {
            log.warning("fetching number of entities of type: "+type+" failed either due to an ill-formed query or during parsing.\n Message from exception is: "+e.getMessage());
            return 0;
        }
    }

    public String[] getPropertiesOfLabel(){
        return new String[]{"http://www.w3.org/2000/01/rdf-schema#label","http://www.w3.org/2004/02/skos/core#altLabel"};
    }

    /**
     * Notable work(P800) -> literary works,bibliography,work,works,major works,famous works,significant works,known for
     * This method can handle both property type ids and resource type
     * The labels are restricted to english language
     */
    public String[] generateLemmaOf(String id) {
        String uid = "glo-";
        String key = uid+id;
        if(cache.containsKey(key))
            return (String[])cache.get(key);

        //if the type supplied is concatenation of two ids such as: http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q5
        //that is the type: instance of humans (i.e. all humans), then the second (or last) id is used for retieving properties
        String[] fs = id.split(":::");
        id = fs[fs.length - 1];
        log.fine("Querying lemma for: " + id);

        //it is required to specially handle the case of property as they are not included in the search by default
        //see: https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/queries#Working_with_qualifiers
        boolean propID = id.contains("http://www.wikidata.org/prop/");

        String q;
        if (!propID)
            q = "SELECT ?property ?value WHERE {" +
                    "<" + id + "> rdfs:label ?property." +
                    "  OPTIONAL {" +
                    "    <" + id + "> skos:altLabel ?value." +
                    "    FILTER((LANG(?value)) = \"en\")" +
                    "  }" +
                    "  FILTER((LANG(?property)) = \"en\")" +
                    "}";
        else
            q = "SELECT ?property ?value WHERE {" +
                    "?p wikibase:directClaim <" + id + ">." +
                    "?p rdfs:label ?property." +
                    "  OPTIONAL {" +
                    "    ?p skos:altLabel ?value." +
                    "    FILTER((LANG(?value)) = \"en\")" +
                    "  }" +
                    "  FILTER((LANG(?property)) = \"en\")" +
                    "}";
        Set<String> lemmas = new LinkedHashSet<>();
        try {
            SPARQLResult result = SPARQLQuery(q);
            result.results.bindings.stream().forEach(b -> {
                lemmas.add(b.property.getUnannotatedText());
                lemmas.add(b.value.getUnannotatedText());
            });
        } catch (IOException | URISyntaxException e) {
            log.warning("Error when fetching lemmas for id: " + id + ", either due to an ill-formed query or during parsing.\n Message from exception is: " + e.getMessage());
            e.printStackTrace();
        }
        String[] res = lemmas.toArray(new String[lemmas.size()]);
        cache.put(key,res);
        return res;
    }

    public int getIntersectionOfTypesWithRel(String colType1, String colType2, String br) {
        String uid = "giotwr-";
        String key = uid+colType1+colType2+br;
        if(cache.containsKey(key)){
            return (Integer)cache.get(key);
        }

        String[] fs1 = colType1.split(":::");
        String[] fs2 = colType2.split(":::");
        String prop1 = fs1[0], val1 = fs1[1];
        String prop2 = fs2[0], val2 = fs2[1];

        if(val1.startsWith("http://")) val1 = "<"+val1+">";
        if(val2.startsWith("http://")) val2 = "<"+val2+">";
        String q = "SELECT (COUNT(*) AS ?value) WHERE {" +
                "?x <" + prop1 + "> " + val1 + " ." +
                "?y <" + prop2 + "> " + val2 + " ." +
                "?x <" + br + "> ?y" +
                "}";
        try {
            SPARQLResult result = SPARQLQuery(q);
            Integer res = Integer.parseInt(result.results.bindings.get(0).value.value);
            cache.put(key, new Integer(res));
            return res;
        } catch (IOException | URISyntaxException | NumberFormatException e) {
            log.warning("Error when finding intersection between type: " + colType1 + "and col type: " + colType2 + " with binary relation: " + br + "; either due to an ill-formed query or during parsing.\n Message from exception is: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) {
        KB kb = Wikidata.initializeFromCache();
        Wikidata test = (Wikidata)kb;
        test.cache.entrySet().stream().filter(e->e.getKey().startsWith("giotwr-")).forEach(System.out::println);

        String[] sgsts = kb.resolveEntity("Harry Potter", 20);
        Stream.of(sgsts).forEach(System.out::println);

        kb.getAllFacts("http://www.wikidata.org/entity/Q34660").entries().stream().map(e->e.toString()).forEach(log::info);

        //number of instances of type human
        log.info(kb.getNumberOfEntitiesOfType("http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q5")+"");
        //number of arjuna award winners
        log.info(kb.getNumberOfEntitiesOfType("http://www.wikidata.org/prop/direct/P166:::http://www.wikidata.org/entity/Q671622")+"");
        log.info("#Inferno type: "+kb.getNumberOfEntitiesOfType("http://www.w3.org/2000/01/rdf-schema#label:::\"Inferno\"@ja"));

        Stream.of(kb.generateLemmaOf("http://www.wikidata.org/prop/direct/P800")).forEach(log::info);
        Stream.of(kb.generateLemmaOf("http://www.wikidata.org/entity/Q20")).forEach(log::info);

        //Intersection between entities of type: (occupation-writer) and type: (instance of -- book) that share the relation (notable work)
        log.info(kb.getIntersectionOfTypesWithRel("http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q36180","http://www.wikidata.org/prop/direct/P31:::http://www.wikidata.org/entity/Q571","http://www.wikidata.org/prop/direct/P800")+"");
        log.info(kb.getIntersectionOfTypesWithRel("http://www.wikidata.org/prop/direct/P106:::http://www.wikidata.org/entity/Q36180","http://www.w3.org/2000/01/rdf-schema#label:::\"Inferno\"@cs","http://www.wikidata.org/prop/direct/P800")+"");
        ((Wikidata)kb).writeSerialized();
    }
}
