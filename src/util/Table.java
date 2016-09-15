package util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by vihari on 14/09/16.
 */
public class Table {
    static Logger log = Logger.getLogger(Table.class.getName());
    public static class TCell {
        String text;
        //annotation
        int st, end;

        //These are some top ranked links in decreasing order of likelihood
        List<String> kbIds;

        TCell(String text) {
            this.text = text;
        }

        public String toString(){
            return text+" "+kbIds;
        }
    }

    int ncols = 0;

    List<String> headers = new ArrayList<>();
    List<List<TCell>> data = new ArrayList<>();

    //semantics related stuff
    //we store multiple options for each semantic related attribue in decresing order of likelihood, though
    //the type of each column described in text
    List<List<String>> colTypes;
    //the binary relation between every pair of columns described in text
    List<List<String>> binaryRels;

    public Table(int ncols) {
        this.ncols = ncols;
        colTypes = new ArrayList<>();
        binaryRels = new ArrayList<>();
        for(int i=0;i<numcols();i++)
            colTypes.add(new ArrayList<>());
        for(int i=0;i<numcols()-1;i++)
            binaryRels.add(new ArrayList<>());
    }

    public List<String> getHeaders(){
        return headers;
    }

    public void addRow(String[] rowData) {
        if (rowData == null || rowData.length != ncols) {
            log.severe("Ignoring invalid row data in table creation!");
            return;
        }
        data.add(Stream.of(rowData).map(TCell::new).collect(Collectors.toList()));
    }

    public void addHeader(String[] headers) {
        if (headers == null || headers.length != ncols) {
            log.severe("Ignoring invalid header data in table creation!");
            return;
        }
        this.headers = Stream.of(headers).collect(Collectors.toList());
    }

    public String getCellText(int r, int c) {
        return data.get(r).get(c).text;
    }

    public int numrows() {
        return data.size();
    }

    public int numcols() {
        return ncols;
    }

    public void setLinksOf(int r, int c, List<String> links){
        data.get(r).get(c).kbIds = links;
    }

    public void setTypesOf(int c, List<String> types){
        colTypes.set(c,types);
    }

    public void setRelsOf(int c, List<String> rels){
        binaryRels.set(c,rels);
    }

    //TODO: make this print pretty
    public String prettyPrint(){
        StringBuffer sb = new StringBuffer();
        IntStream.range(0, numcols())
                .forEach(c-> {
                    if (headers != null && headers.size() > c)
                        sb.append(headers.get(c));
                    sb.append(colTypes.get(c));
                    sb.append("|");
                });
        sb.append("\n");
        IntStream.range(0,numrows())
                .forEach(r -> {
                    IntStream.range(0,numcols())
                        .forEach(c->{
                            sb.append(data.get(r).get(c));
                            sb.append("|");
                        });
                    sb.append("\n");
                });
        IntStream.range(0,numcols()-1).forEach(c -> {
            sb.append("Possible relations between column " + c + " and " + (c + 1) + " is " + binaryRels.get(c) + "\n");
        });
        return sb.toString();
    }
}