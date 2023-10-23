package parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * IDs are set depth-first from left to right (in order of children).
 */
public class Node {
    public interface Visitor {
        void visit(Node n);
    }

    private static int _nextId = 0;

    private static int nextId() {
        return _nextId++;
    }

    // temporal information
    public int tid = -1;
    public Node tparent = null;
    public Node tchild = null;
    public int numInserts = 0;
    public int numDeletes = 0;

    public int id;
    public final String label;
    public int startIndex;
//    public int endIndex;
    public int length;
    public final List<Node> children;
    public Node parent = null;
//    public int tparentId = -1;
    public boolean reference = false;
    public String text;

    public Node(String label, int startIndex, int endIndex, int length, List<Node> children, String text) {
        this.id = nextId();
        this.label = label;
        if (startIndex > endIndex || endIndex-startIndex+1 != length) {
            throw new RuntimeException("Unexpected start/end indices");
        }
        this.startIndex = startIndex;
//        this.endIndex = endIndex;
        this.length = length;
        this.children = new ArrayList<>(children);
        this.text = text;

        for (Node child : this.children) {
            child.parent = this;
        }
    }

    /**
     * Warning: this copies the ids over as well.
     * 
     * @param copy
     */
    public Node(Node copy) {
        this.id = copy.id;
        this.label = copy.label;
        this.startIndex = copy.startIndex;
//        this.endIndex = copy.endIndex;
        this.length = copy.length;
        this.children = new ArrayList<>();
        this.text = copy.text;
        this.tparent = copy;

        for (Node child : copy.children) {
            Node newChild = new Node(child);
            this.children.add(newChild);
            newChild.parent = this;
        }
    }

    public boolean isEqual(Node node) {
        if (this == node)
            return true;
        boolean e = (id == node.id && startIndex == node.startIndex && length == node.length &&
                getTparentId() == node.getTparentId() && Objects.equals(label, node.label));
        if (!e) {
            System.out.println("not equal: "+node.id);
            return false;
        }
        for (int i = 0; i < children.size(); ++i) {
            if (!children.get(i).isEqual(node.children.get(i))) {
                System.out.println("not equal: "+node.id);
                return false;
            }
        }
        return true;
    }

    public int getEndIndex() {
        return this.startIndex + this.length - 1;
    }

    public void resetIds(int startId) {
        int id = startId;
        resetIdsImpl(id);
    }

    private int resetIdsImpl(int id) {
        for (Node child : children) {
            id = child.resetIdsImpl(id);
        }
//        this.tparentId = this.id;
        this.id = id;
        return id + 1;
    }

    public void replace(Node replacement) {
        if (replacement.getTparentId() == id) {
            throw new RuntimeException("Unexpectedly trying to replace the root.");
        }
        replaceImpl(replacement);
    }

    private void replaceImpl(Node replacement) {
        for (int i = 0; i < children.size(); ++i) {
            Node child = children.get(i);
            if (replacement.getTparentId() == child.id) {
                children.set(i, replacement);
                replacement.parent = this;
//                if (child.startIndex != replacement.startIndex) {
//                    System.out.println("child id: "+child.id+" replacement id: "+replacement.id);
//                    throw new RuntimeException("Start indices unexpectedly different: child="+child.startIndex+" replacement="+replacement.startIndex);
//                }

                // Update ranges
                int addStart = replacement.startIndex - child.startIndex;
                int addLength = replacement.length - child.length;
                replacement.parent.updateRanges(addStart, addLength, replacement);
                return;
            } else {
                child.replaceImpl(replacement);
            }
        }
    }

    private void updateRanges(int addStart, int addLength, Node sourceChild) {
        // The sourceChild is the child that is propagating the change up to this.
        this.startIndex += addStart;
        this.length += addLength;
        int i = children.indexOf(sourceChild);
        for (int j = i + 1; j < children.size(); ++j) {
            children.get(j).updateRangesDown(addLength+addStart);
        }
        if (parent != null) {
            parent.updateRanges(addStart, addLength, this);
        }
    }

    private void updateRangesDown(int add) {
        this.startIndex += add;
//        if (this.startIndex > this.endIndex) {
//            throw new RuntimeException("updateRangesDown causing a problem");
//        }
        for (Node child : this.children) {
            child.updateRangesDown(add);
        }
    }

    public int getId() {
        return id;
    }

    public Node getTparent() {
        return tparent;
    }

    public int getTparentId() {
//        return tparentId;
        return (tparent != null) ? tparent.getId() : -1;
    }

    public void setTparent(Node tparent) {
        this.tparent = tparent;
//        this.tparentId = tparent.getId();
    }

    public boolean isReference() {
        return this.reference;
    }

    public void setIsReference(boolean reference) {
        this.reference = reference;
    }

    public void traverse(Visitor visitor) {
        visitor.visit(this);
        for (Node child : this.children) {
            child.traverse(visitor);
        }
    }

    // -------------------------------------------------------
    // Reconstruction
    // -------------------------------------------------------
    public Node reconstruct(Node source, Node tree) {
        return null;
    }

    // -------------------------------------------------------
    // JSON
    // -------------------------------------------------------

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", label);
        json.put("startIndex", startIndex);
        if (this.reference) {
            json.put("reference", true);
        }
        if (this.tparent != null) {
            json.put("tparent", this.tparent.id);
        }

        JSONArray jsonChildren = new JSONArray();
        for (Node child : children) {
            jsonChildren.put(child.toJSON());
        }

        JSONObject object = jsonChildren.toJSONObject(listNumberArray(jsonChildren.length()));
        json.put("children", object);

        return json;
    }

    private static JSONArray listNumberArray(int max) {
        JSONArray res = new JSONArray();
        for (int i = 0; i < max; i++) {
            res.put(String.valueOf(i));
        }
        return res;
    }

    // -------------------------------------------------------
    // GraphViz Dot
    // -------------------------------------------------------
    public String toDot() {
        return toDot("n");
    }

    public String toDot(String prefix) {
        StringBuffer buf = new StringBuffer();
        nodeToDot(buf, prefix);
        return buf.toString();
    }

    private void nodeToDot(StringBuffer buf, String prefix) {
        if (label != "Terminal") {
            buf.append(
                    String.format("%s%d [label = \"%s\\n%d, %d-%d, tp=%d\"];\n", prefix, id, label, id, startIndex, getEndIndex(), getTparentId()));
        } else {
            buf.append(
                    String.format("%s%d [label = \"%s\\n%d, %d-%d, tp=%d\"];\n", prefix, id, text, id, startIndex, getEndIndex(), getTparentId()));
        }
        // if (this.tparent != null) {
        // buf.append(String.format("n%s->n%s [style=dashed]\n", this.id,
        // this.tparent.id));
        // }
        for (Node child : children) {
            child.nodeToDot(buf, prefix);
            buf.append(String.format("%s%s->%s%s;\n", prefix, id, prefix, child.id));
        }
    }

    public String toString() {
        return this.text;
    }

    private String debugNode(String offset) {
        String reString = offset
                + "Label: `" + this.label + "`"
                + " | Name: `" + this.toString() + "`"
                + " | Start: " + this.startIndex
                + " | End: " + this.getEndIndex()
                + " | Inserts: " + this.numInserts
                + " | Deletes: " + this.numDeletes
                + " | tid: " + tid;

        reString += " | tpid: ";
        if (this.tparent != null) {
            reString += tparent.tid;
        } else {
            reString += "NaN";
        }

        reString += " | tchild: ";
        if (this.tchild != null) {
            reString += tchild.tid;
        } else {
            reString += "NaN";
        }

        return reString;
    }

    private String debugTree(String offset) {
        String reString = this.debugNode(offset);

        for (Node child : children) {
            reString += "\n";
            reString += child.debugTree(offset + "  ");
        }

        return reString;
    }

    public String debugNode() {
        return debugNode("");
    }

    public String debugTree() {
        return debugTree("");
    }

}
