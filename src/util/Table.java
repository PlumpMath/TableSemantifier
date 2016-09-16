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
        List<String> kbIds = new ArrayList<>();
        List<String> kbTexts = new ArrayList<>();

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
    //the type of each column described in text -- the supplementary *Text holds the description of each id
    List<List<String>> colTypeIds = new ArrayList<>();
    List<List<String>> colTypeText = new ArrayList<>();
    //the binary relation between every pair of columns described in text
    List<List<String>> binaryRelIds = new ArrayList<>();
    List<List<String>> binaryRelText = new ArrayList<>();

    public Table(int ncols) {
        this.ncols = ncols;
        for(int i=0;i<numcols();i++) {
            colTypeIds.add(new ArrayList<>());
            colTypeText.add(new ArrayList<>());
        }
        for(int i=0;i<numcols()-1;i++) {
            binaryRelIds.add(new ArrayList<>());
            binaryRelText.add(new ArrayList<>());
        }
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

    public void setLinksIds(int r, int c, List<String> linkIds){
        data.get(r).get(c).kbIds = linkIds;
    }

    public void setLinks(int r, int c, List<String> linkIds, List<String> linkTexts){
        data.get(r).get(c).kbIds = linkIds;
        data.get(r).get(c).kbTexts = linkTexts;
    }

    /**
     * Use this routine only if there is no accompanying description for ids is not available*/
    public void setColumnTypeIds(int c, List<String> typeIds){
        colTypeIds.set(c,typeIds);
    }

    public void setColTypes(int c, List<String> typeIds, List<String> typeTexts){
        colTypeIds.set(c, typeIds);
        colTypeText.set(c, typeTexts);
    }

    /**
     * Use this routine only if there is no accompanying description for relations*/
    public void setColumnRelIds(int c, List<String> relIds){
        binaryRelIds.set(c,relIds);
    }

    public void setColumnRels(int c, List<String> relIds, List<String> relTexts){
        binaryRelIds.set(c, relIds);
        binaryRelText.set(c, relTexts);
    }

    //TODO: make this print pretty
    public String prettyPrint(){
        StringBuffer sb = new StringBuffer();
        IntStream.range(0, numcols())
                .forEach(c-> {
                    if (headers != null && headers.size() > c)
                        sb.append(headers.get(c));
                    sb.append(colTypeIds.get(c));
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
            sb.append("Possible relations between column " + c + " and " + (c + 1) + " is " + binaryRelIds.get(c) + "\n");
        });
        return sb.toString();
    }

    public String htmlPrint(){
        StringBuffer sb = new StringBuffer();
        sb.append("<table>\n");
        sb.append("<thead>");
        IntStream.range(0, numcols())
                .forEach(c-> {
                    sb.append("<th>");
                    if (headers != null && headers.size() > c)
                        sb.append(headers.get(c));
                    List<String> ids = colTypeIds.get(c);
                    List<String> texts = colTypeText.get(c);
                    IntStream.range(0,ids.size()).forEach(ci -> sb.append("<br>" + "<a href='" + ids.get(ci) + "'>" + texts.get(ci) + "</a>"));
                    sb.append("</th>");
                });
        sb.append("</thead>\n");

        sb.append("<tbody>\n");
        IntStream.range(0,numrows())
                .forEach(r -> {
                    sb.append("<tr>");
                    IntStream.range(0,numcols())
                            .forEach(c->{
                                TCell cell = data.get(r).get(c);
                                sb.append("<td>");
                                sb.append(cell.text);
                                IntStream.range(0,cell.kbIds.size()).forEach(ci -> sb.append("<br>" + "<a href='"+cell.kbIds.get(ci)+"'>"+cell.kbTexts.get(ci)+"</a>"));
                                sb.append("</td>");
                            });
                    sb.append("</tr>\n");
                });
        sb.append("</tbody>\n");
        sb.append("</table>");

        IntStream.range(0,numcols() - 1).forEach(c -> {
            sb.append("Possible relations between column " + c + " and " + (c + 1) + " is <a href='" + binaryRelIds.get(c) + "'>"+binaryRelText.get(c)+"</a>\n");
        });
        return sb.toString();
    }
}