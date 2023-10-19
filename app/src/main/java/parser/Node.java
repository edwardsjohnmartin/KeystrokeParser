package parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public final int startIndex;
    public final int endIndex;
    public final int length;
    public final List<Node> children;
    public Node parent = null;
    public int tparentId = -1;
    public boolean reference = false;
    public String text;

    public Node(String label, int startIndex, int endIndex, int length, List<Node> children, String text) {
        this.id = nextId();
        this.label = label;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
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
        this.endIndex = copy.endIndex;
        this.length = copy.length;
        this.children = new ArrayList<>();
        this.text = copy.text;

        for (Node child : copy.children) {
            Node newChild = new Node(child);
            this.children.add(newChild);
            newChild.parent = this;
        }
    }

    public void resetIds(int startId) {
        int id = startId;
        resetIdsImpl(id);
    }

    private int resetIdsImpl(int id) {
        for (Node child : children) {
            id = child.resetIdsImpl(id);
        }
        this.id = id;
        return id + 1;
    }

    public void replace(Node replacement) {
        if (replacement.tparentId == id) {
            throw new RuntimeException("Unexpectedly trying to replace the root.");
        }
        replaceImpl(replacement);
    }

    private void replaceImpl(Node replacement) {
        for (int i = 0; i < children.size(); ++i) {
            Node child = children.get(i);
            if (replacement.tparentId == child.id) {
                children.set(i, replacement);
                return;
            } else {
                child.replaceImpl(replacement);
            }
        }
    }

    public int getId() {
        return id;
    }

    public Node getTparent() {
        return tparent;
    }

    public int getTparentId() {
        return tparentId;
    }

    public void setTparent(Node tparent) {
        this.tparent = tparent;
        this.tparentId = tparent.getId();
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
        StringBuffer buf = new StringBuffer();
        nodeToDot(buf);
        return buf.toString();
    }

    private void nodeToDot(StringBuffer buf) {
        if (label != "Terminal") {
            buf.append(
                    String.format("n%d [label = \"%s\\n%d, %d-%d\"];\n", id, label, id, startIndex, endIndex));
        } else {
            buf.append(
                    String.format("n%d [label = \"%s\\n%d, %d-%d\"];\n", id, text, id, startIndex, endIndex));
        }
        // if (this.tparent != null) {
        // buf.append(String.format("n%s->n%s [style=dashed]\n", this.id,
        // this.tparent.id));
        // }
        for (Node child : children) {
            child.nodeToDot(buf);
            buf.append(String.format("n%s->n%s;\n", id, child.id));
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
                + " | End: " + this.endIndex
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
