package util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
        String dbId;

        TCell(String text) {
            this.text = text;
        }
    }

    int ncols = 0;

    List<String> headers = new ArrayList<>();
    List<List<TCell>> data = new ArrayList<>();

    //semantics related stuff
    //the type of each column described in text
    String[] colTypes;
    //the binary relation between every pair of columns described in text
    String[][] binaryRels;

    public Table(int ncols) {
        this.ncols = ncols;
        colTypes = new String[ncols];
        binaryRels = new String[ncols][ncols];
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
}