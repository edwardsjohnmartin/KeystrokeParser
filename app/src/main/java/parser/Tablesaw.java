package parser;

import java.util.ArrayList;
import java.util.List;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

public class Tablesaw {

    public void test() {
        final String fileName = "src/main/resources/sample.csv";
        Table dataframe = this.readFile(fileName);
        this.printHeaders(dataframe);

        List<String> keys = this.createKeys(dataframe);
        System.out.println("\nUnique keys in file: " + keys.size());

        this.printRows(dataframe);
    }

    public Table readFile(final String fileName) {
        ColumnType[] types = {
                ColumnType.INTEGER, // EventID
                ColumnType.STRING, // SubjectID
                ColumnType.STRING, // AssignmentID
                ColumnType.STRING, // CodeStateSection
                ColumnType.STRING, // EventType
                ColumnType.INTEGER, // SourceLocation
                ColumnType.STRING, // EditType
                ColumnType.STRING, // InsertText
                ColumnType.STRING, // DeleteText
                ColumnType.STRING, // X-Metadata
                ColumnType.LONG, // ClientTimestamp
                ColumnType.STRING, // ToolInstances
                ColumnType.STRING, // CodeStateID
        };
        CsvReadOptions options = CsvReadOptions.builder(fileName)
                .maxCharsPerColumn(25000)
                .separator(',')
                .header(true)
                .columnTypes(types)
                .build();

        Table dataframe = Table.read().usingOptions(options);
        dataframe = dataframe.removeColumns("EventID", "X-Metadata", "ToolInstances", "CodeStateID");
        final Selection selection = dataframe.stringColumn("EventType").isEqualTo("File.Edit");
        return dataframe.where(selection).removeColumns("EventType");
    }

    public void writeFile(final Table dataframe, final String fileName) {
        dataframe.write().csv(fileName);
    }

    public void printHeaders(final Table dataframe) {
        System.out.println(dataframe.structure());
    }

    public void printRows(final Table dataframe) {
        System.out.println(dataframe.print());
    }

    public void printRows(final Table dataframe, final int rows) {
        System.out.println(dataframe.print(rows));
    }

    public List<String> createKeys(final Table dataframe) {
        ArrayList<String> keys = new ArrayList<>();

        for (Row row : dataframe) {
            String student = row.getString("SubjectID");
            String assignment = row.getString("AssignmentID");
            String task = row.getString("CodeStateSection");
            String key = String.format("%s_%s_%s", student, assignment, task);

            if (!keys.contains(key))
                keys.add(key);
        }

        System.out.println(keys);

        return keys;
    }

}
