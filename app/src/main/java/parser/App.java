/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package parser;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import parser.antlr.PythonLexer;
import parser.antlr.PythonParser;
import parser.antlr.PythonParser.RootContext;
import tech.tablesaw.api.Table;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        ts();
//         getReconTrees();
    }

    private static void outputGraphViz(Node tree) {
        List<Node> trees = new ArrayList<>();
        trees.add(tree);
        outputGraphViz(trees);
    }

    private static void outputGraphViz(List<Node> trees) {
        Trees.outputGraphViz(trees, "trees.dot");
//        System.out.println("digraph G {");
//        for (int i = 0; i < trees.size(); ++i) {
//            System.out.println(trees.get(i).toDot(String.format("%s", Character.toString((char)(((int)'a')+i)))));
//        }
//        System.out.println("}");
    }

    private static void getReconTrees() throws IOException {

        // run.tree2dot(Parser.createTreeFromFile("src/main/resources/test1/1.py"));
        // run.tree2dot(Parser.createTreeFromFile("src/main/resources/test1/2.py"));
        // run.tree2dot(Parser.createTreeFromFile("src/main/resources/test1/3.py"));

        List<Node> origTrees = new ArrayList<>();
        // origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test1/1.py")));
        // origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test1/2.py")));
//        origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test1/3.py")));
//        origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test1/4.py")));
        origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test2/1.py")));
        origTrees.add(MyVisitor.toSimpleTree(Parser.createTreeFromFile("src/main/resources/test2/2.py")));
        outputGraphViz(origTrees);

//        outputGraphViz(findChanged(origTrees.get(0), origTrees.get(1)));

//        // Set up id2node
//        Map<Integer, Node> id2node = new HashMap<>();
//        Node.Visitor v = n -> id2node.put(n.getId(), n);
//        for (Node root : origTrees) {
//            root.traverse(v);
//        }
//
////        // Test resetIds
////        // orig2.resetIds(orig1.getId()+1);
////
//        // Prune trees
//        List<Node> prunedTrees = new ArrayList<>();
//        prunedTrees.add(origTrees.get(0));
//        prunedTrees.get(0).setIsReference(true);
//        // prunedTrees.add(id2node.get(42));
//        // prunedTrees.get(1).setTparent(id2node.get(13));
//        // prunedTrees.add(id2node.get(80));
//        // prunedTrees.get(2).setTparent(id2node.get(45));
//
//        prunedTrees.add(id2node.get(32));
//        prunedTrees.get(1).setTparent(id2node.get(0));
//
////        outputGraphViz(prunedTrees);
//
//        // Reconstruct from pruned trees
//        List<Node> reconTrees = new ArrayList<>();
//        for (Node pruned : prunedTrees) {
//            if (pruned.isReference()) {
//                reconTrees.add(pruned);
//            } else {
//                Node ref = reconTrees.get(reconTrees.size() - 1);
//                Node copy = new Node(ref);
//                copy.replace(pruned);
//                copy.resetIds(ref.getId() + 1);
//                reconTrees.add(copy);
//            }
//        }
//
//        for (int i = 0; i < origTrees.size(); ++i) {
//            if (!origTrees.get(i).isEqual(reconTrees.get(i))) {
//                System.out.println("digraph G {");
//                System.out.println(origTrees.get(i).toDot("o"));
//                System.out.println(reconTrees.get(i).toDot("r"));
//                System.out.println("}");
//                throw new RuntimeException("Reconstructed tree at index " + i + " is incorrect.");
//            }
//        }
//
////        outputGraphViz(origTrees);
////        outputGraphViz(prunedTrees);
//        outputGraphViz(reconTrees);
    }

//    private static Node findChanged(Node a, Node b) {
//        Node changed = findChangedImpl(a, b);
//        if (changed == null) {
//            // We need to check for anything that might have moved.
//            changed = findStartChangedImpl(a, b);
//        }
//        return changed;
//    }
//
//    // Finds the first node with a label difference in a top-down traversal.
//    private static Node findChangedImpl(Node a, Node b) {
//        if (!a.label.equals(b.label)) {
//            return b;
//        }
//        if (a.label.equals("Terminal")) {
//            if (!a.text.equals(b.text)) {
//                return b;
//            }
//        }
//        Node changed = null;
//        for (int i = 0; i < a.children.size(); ++i) {
//            changed = findChangedImpl(a.children.get(i), b.children.get(i));
//            if (changed != null) {
//                return changed;
//            }
//        }
//        return null;
//    }
//
//    // Finds the first node with a differing start index in an in-order traversal.
//    private static Node findStartChangedImpl(Node a, Node b) {
//        if (!a.label.equals(b.label)) {
//            throw new RuntimeException("No labels should have changed in findStartChangedImpl");
//        }
//        if (a.label.equals("Terminal")) {
//            if (!a.text.equals(b.text)) {
//                throw new RuntimeException("No labels should have changed in findStartChangedImpl");
//            }
//        }
//        Node changed = null;
//        for (int i = 0; i < a.children.size(); ++i) {
//            changed = findStartChangedImpl(a.children.get(i), b.children.get(i));
//            if (changed != null) {
//                return changed;
//            }
//        }
//        if (a.startIndex != b.startIndex) {
//            return b;
//        }
//        return null;
//    }

    private static void ts() {
        var ts = new Tablesaw();

//        final String fileName = "src/main/resources/sample.csv";
        final String fileName = "/Users/edwards/projects/SQLiteToProgSnap2/parse.csv";
        Table dataframe = ts.readFile(fileName);
//        ts.printHeaders(dataframe);

        List<String> keys = ts.createKeys(dataframe);
//        System.out.println("\nUnique keys in file: " + keys.size());
//        System.out.println("\n");

        //--------------------------------------------------------------------
        // Note: This code relies on changes being local. That is, no single
        // event can have both an insertion and a deletion, and all insertions
        // and deletions are contiguous.
        //--------------------------------------------------------------------

//        final String key = "Student1_Assign12_task1.py";
        final String key = "student__main.py";
        Table selection = ts.selectTask(dataframe, key);
        Reconstruction reconstruction = new Reconstruction(selection);
//        int start=173, len=7;
//        int start=0, len=14;
        int start=0, len=26;
//        int start=33, len=4;
//        Trees origTrees = new Trees(reconstruction.trees.subList(start, start+len));
        Trees origTrees = new Trees(reconstruction.trees);

//        Trees origTrees = new Trees(reconstruction.trees);
        origTrees.outputGraphViz("orig.dot");
//        origTrees.outputJSON();

        Trees prunedTrees = origTrees.prune();
        prunedTrees.outputGraphViz("pruned.dot");
        Trees reconTrees = prunedTrees.reconstructFromPruned();
        reconTrees.outputGraphViz("recon.dot");
        origTrees.checkEqual(reconTrees);


//        System.out.println(reconstruction.codeStates.get(n));
//        outputGraphViz(tree);

//        outputGraphViz(findChanged(trees.get(n), trees.get(n+1)));
//        reconstruction.debug();
    }
}
