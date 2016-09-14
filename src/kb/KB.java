package kb;

import com.google.common.collect.Multimap;

/**
 * Created by vihari on 12/09/16.
 * Knowledge base connector endpoint
 * NO method should that implements this interface should return a null value
 */
public interface KB {
    /**
     * Suggest possible page completions of the text provided
     * The behaviour should simulate an auto-suggest.
     * @param text - partial text describing entity that is being looked up. The text can be a variant, or random substring of title of interest
     * @param limit - the number of results is capped at limit
     * @return array of ids of candidate resolutions*/
    String[] resolveEntity(String text, int limit);

    /**
     * @param id of the database record
     * @return properties defined over this record*/
    String[] getAllAttributes(String id);

    /**
     * All the facts that are denoted as attribute-value pairs in the database of a given recod
     * Properties or value are not unique -- for example there can be many values for property: "notable work"
     * @param id of the database record
     * @return all the relation-object pairs in a map*/
    Multimap<String,String> getAllFacts(String id);

    /**
     * Given a type of entity, returns the number of unique entities of this type in the database*/
    int getNumberOfEntitiesOfType(String type);

    /**
     * Get the property that corresponds to label of the resource
     * For example the label value in Wikidata corresponds to: http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2004/02/skos/core#altLabel"
     * such as in --  http://www.w3.org/2000/01/rdf-schema#label="Joanne K. Rowling"@en*/
    String[] getPropertiesOfLabel();
}
