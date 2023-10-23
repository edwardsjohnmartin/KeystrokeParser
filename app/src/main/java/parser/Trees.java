package parser;

import java.util.ArrayList;
import java.util.List;

public class Trees {
    private final List<Node> trees;// = new ArrayList<>();

    public Trees(Reconstruction reconstruction) {
        int num = 7;
        this.trees = reconstruction.trees.subList(173, 173+num);
    }

    public Trees(List<Node> trees) {
        this.trees = trees;
    }

    public void add(Node tree) {
        this.trees.add(tree);
    }

    private static Node findChanged(Node a, Node b) {
        Node changed = findChangedImpl(a, b);
        if (changed == null) {
            // We need to check for anything that might have moved.
            changed = findStartChangedImpl(a, b);
        }
        return changed;
    }

    // Finds the first node with a label difference in a top-down traversal.
    private static Node findChangedImpl(Node a, Node b) {
        if (!a.label.equals(b.label)) {
            return b;
        }
        if (a.label.equals("Terminal")) {
            if (!a.text.equals(b.text)) {
                return b;
            }
        }
        Node changed = null;
        for (int i = 0; i < a.children.size(); ++i) {
            changed = findChangedImpl(a.children.get(i), b.children.get(i));
            if (changed != null) {
                return changed;
            }
        }
        return null;
    }

    // Finds the first node with a differing start index in an in-order traversal.
    private static Node findStartChangedImpl(Node a, Node b) {
        if (!a.label.equals(b.label)) {
            throw new RuntimeException("No labels should have changed in findStartChangedImpl");
        }
        if (a.label.equals("Terminal")) {
            if (!a.text.equals(b.text)) {
                throw new RuntimeException("No labels should have changed in findStartChangedImpl");
            }
        }
        Node changed = null;
        for (int i = 0; i < a.children.size(); ++i) {
            changed = findStartChangedImpl(a.children.get(i), b.children.get(i));
            if (changed != null) {
                return changed;
            }
        }
        if (a.startIndex != b.startIndex) {
            return b;
        }
        return null;
    }


    public Trees prune() {
        Node reference = trees.get(0);
        reference.setIsReference(true);
        List<Node> prunedTrees = new ArrayList<>();
        prunedTrees.add(reference);

        for (int i = 0; i < trees.size()-1; ++i) {
            int previ = i;
            int curi = i+1;
            Node prev = trees.get(previ);
            Node cur = trees.get(curi);
            prunedTrees.add(findChanged(prev, cur));
        }
        return new Trees(prunedTrees);
    }

    public Trees reconstructFromPruned() {
        List<Node> prunedTrees = this.trees;
        List<Node> reconTrees = new ArrayList<>();
        for (int i = 0; i < prunedTrees.size(); ++i) {
//        for (Node pruned : prunedTrees) {
            Node pruned = prunedTrees.get(i);
            System.out.println(i);
            if (pruned.isReference()) {
                reconTrees.add(pruned);
            } else {
                Node ref = reconTrees.get(reconTrees.size() - 1);
                Node copy = new Node(ref);
                copy.replace(pruned);
                copy.resetIds(ref.getId() + 1);
                reconTrees.add(copy);
            }
        }
        return new Trees(reconTrees);
    }

    public void outputGraphViz() {
        outputGraphViz(trees);
    }

    public static void outputGraphViz(List<Node> trees) {
        System.out.println("digraph G {");
        for (int i = 0; i < trees.size(); ++i) {
            System.out.println(trees.get(i).toDot(String.format("%s", Character.toString((char)(((int)'a')+i)))));
        }
        System.out.println("}");
    }
}
