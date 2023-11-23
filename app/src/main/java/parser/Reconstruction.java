package parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import parser.antlr.PythonParser;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

/* TODO: files that end with a [TAB] cannot compile with Antlr */
public class Reconstruction {
    public int length;

    public List<Node> trees = new ArrayList<>();
//    public List<String> errors = new ArrayList<>();
    public List<String> codeStates = new ArrayList<>();

    private TemporalHierarchy correspondance = new TemporalHierarchy();

    public Reconstruction(Table dataframe) {
        this.reconstruct(dataframe);
        this.length = trees.size();

        // this.debug();
    }

    private void reconstruct(Table dataframe) {
        String state = "";
        this.codeStates.add(state);
        this.createCorrespondance(0, state, "");

        for (Row row : dataframe) {
            int i = row.getInt("SourceLocation");
            String insertText = row.getString("InsertText");
            String deleteText = row.getString("DeleteText");

            if ("".equals(insertText) && "".equals(deleteText)) {
                System.out.println("Insert and Delete are empty");
                System.out.println(row.toString());
                return;
            }

            final String lhs = state.substring(0, i);
            final String rhs = state.substring(i + deleteText.length());
            state = lhs + insertText + rhs;
            this.codeStates.add(state);

            this.createCorrespondance(i, insertText, deleteText);
        }
    }

    private void createCorrespondance(int i, String insertText, String deleteText) {

        String errorMessage = null;
        String src = codeStates.get(codeStates.size() - 1);
        Node tree;
        try {
            PythonParser.RootContext ctx = Parser.createTree(src);
            tree = MyVisitor.toSimpleTree(ctx, src);
        } catch(Exception e) {
            tree = Trees.NODE_UNCOMPILABLE;
        }

        if (tree == null) {
            throw new RuntimeException("Tree is null");
        }

        correspondance.allCompilable.add(tree != Trees.NODE_UNCOMPILABLE);
        correspondance.temporalCorrespondence(i, this.trees.size(), insertText, deleteText);
        if (tree != Trees.NODE_UNCOMPILABLE) {
            correspondance.temporalHierarchy(tree, this.codeStates);
        }

//        if (tree == null) {
//            // Tree is empty -- there is no code
//            tree = new Node(Trees.NODE_TYPE_EMPTY, 0, src.length(), new ArrayList<>(), src);
//        }

//        // Check for bugs
//        Node tparent = null;
//        for (Node n:this.trees) {
//            if (n != null) tparent = n;
//        }
//        if (tree != null && tparent != null)
//            tree.checkTParents(tparent);

        this.trees.add(tree);
    }

    public void writeStates(String key) {
        new File("output/" + key).mkdirs();

        for (int i = 0; i < codeStates.size(); ++i) {
            String state = this.codeStates.get(i);

            try {
                FileWriter writer = new FileWriter("output/" + key + "/" + i + ".py");
                writer.write(state);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Node> getTidToNodes() {
        return this.correspondance.tidToNode;
    }

    public int size() {
        return this.length;
    }

    public void debug() {
        for (int i = 0; i < trees.size(); ++i) {
            final Node tree = this.trees.get(i);

            if (tree == null) {
                System.out.println("```\n" + this.codeStates.get(i) + "\n```");
//                System.out.println(this.errors.get(i));
                System.out.println("\n\n\n");
            } else {
                System.out.println("```\n" + this.codeStates.get(i) + "\n```");
                System.out.println(tree.debugTree());
                System.out.println("\n\n\n");
            }
        }
    }
}
