package parser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
//    public int tid = -1;
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
    private final String text;

    private final String src;
    // The starting character index of the next node. This is used in
    // pruning and reconstruction.
    public int nextStartIndex = -1;

    public Node(String label, int startIndex, int endIndex, int length, List<Node> children, String src) {
        this.id = nextId();
        this.label = label;
        if (startIndex > endIndex || endIndex-startIndex+1 != length) {
            throw new RuntimeException("Unexpected start/end indices");
        }
        this.startIndex = startIndex;
//        this.endIndex = endIndex;
        this.length = length;
        this.children = new ArrayList<>(children);
        String text = null;
        if (Objects.equals(label, "Terminal")) {
            text = src.substring(startIndex, startIndex+length);
        }
        this.text = text;
        this.src = src;

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
        this.src = null;
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

    public String getSource() {
        return this.src.substring(startIndex, startIndex+length);
    }

    /**
     * Post-order traversal
     * @param visitor
     */
    public void postOrder(Visitor visitor) {
        for (Node child:children) {
            child.postOrder(visitor);
        }
        visitor.visit(this);
    }

    /**
     * Visits all nodes that have a start index strictly
     * after the end index of this. So, the parent of this
     * is not included, but siblings that come after this
     * are included.
     * @param visitor
     */
    public void strictlyAfter(Visitor visitor) {
        if (parent == null) {
            return;
        }
        int i = parent.children.indexOf(this)+1;
        for (; i < parent.children.size(); ++i) {
            parent.children.get(i).postOrder(visitor);
        }
        parent.strictlyAfter(visitor);
    }

    /**
     * Returns a list of nodes in depth-first order.
     * @return
     */
    public List<Node> getDepthFirst() {
        List<Node> nodes = new ArrayList<>();
        postOrder((Node n) -> {nodes.add(n);});
        return nodes;
    }

    public Node find(int id) {
        List<Node> nodes = new ArrayList<>();
        postOrder((Node n) -> {
           if (n.id == id) {
               nodes.add(n);
           }
        });
        if (nodes.isEmpty()) return null;
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        throw new RuntimeException("Multiple nodes with id " + id);
    }

    // Iterates through each node and ensures that if each node has
    // a temporal parent, it exists in the tparentTree.
    public void checkTParents(final Node tparentTree) {
        postOrder((Node n) -> {
            final int tp = n.getTparentId();
            if (tp != -1) {
                if (tparentTree.find(tp) == null) {
                    throw new RuntimeException(String.format("%d not found in tree", tp));
                }
            }
        });
    }

    public int getEndInclusiveIndex() {
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
        replaceImpl(replacement, null);
    }

    private Node lastChild() {
        if (children.isEmpty()) return null;
        return children.get(children.size()-1);
    }

    private class ReplacementResult {
        /**
         * How much the start index changed for the node being replaced.
         */
        public final int startOffset;
        /**
         * How much the length changed for the node being replaced.
         */
        public final int lengthOffset;
        /**
         * How much subsequent nodes need to update their start offset by.
         */
        public final int offset;
        public ReplacementResult(int startOffset, int lengthOffset) {
            if (startOffset != 0 && lengthOffset != 0) {
                throw new RuntimeException("length and startIndex both unexpectedly changed");
            }
            if (startOffset == 0 && lengthOffset == 0) {
                throw new RuntimeException("nothing unexpectedly changed");
            }

            this.startOffset = startOffset;
            this.lengthOffset = lengthOffset;
            this.offset = startOffset + lengthOffset;
        }
    }

    private ReplacementResult replaceImpl(Node replacement, ReplacementResult result) {
        if (result != null) {
            // A node ahead of this changed. Update the offset.
            this.startIndex += result.offset;
            for (Node child:children) {
                child.replaceImpl(replacement, result);
            }
        } else {
            boolean firstChild = false;
            for (int i = 0; i < children.size(); ++i) {
                Node child = children.get(i);
                if (replacement.getTparentId() == child.id) {
                    // We found the node to replace. Do the replacement.
                    children.set(i, replacement);
                    replacement.parent = this;

                    result = new ReplacementResult(replacement.startIndex - child.startIndex,
                            replacement.length - child.length);
                } else {
                    result = child.replaceImpl(replacement, result);
                }
                if (result != null && i == 0) {
                    firstChild = true;
                }
            }
            // Changes from unrolling
            if (result != null) {
                if (firstChild && result.startOffset != 0) {
                    // If the start offset of our first child changed then update our start offset.
                    this.startIndex += result.startOffset;
                } else {
                    this.length += result.offset;
                    result = new ReplacementResult(0, result.offset);
                }
            }
        }
        return result;
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

    private Node getNext() {
        if (parent == null) {
            return null;
        }
        final int i = parent.children.indexOf(this);
        if (i < parent.children.size()-1) {
            return parent.children.get(i+1);
        }
        return parent.getNext();
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

    public void computeNextStartIndex() {
        Node n = getNext();
        if (n != null) {
            this.nextStartIndex = n.startIndex;
        } else {
            this.nextStartIndex = -1;
        }
    }

    // -------------------------------------------------------
    // Reconstruction
    // -------------------------------------------------------
//    public Node reconstruct(Node source, Node tree) {
//        return null;
//    }

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
        if (this.nextStartIndex > -1) {
            json.put("nextStartIndex", this.nextStartIndex);
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
        if (!Objects.equals(label, "Terminal")) {
            buf.append(
                    String.format("%s%d [label = \"%s\\n%d, %d-%d, tp=%d\"];\n", prefix, id, label, id, startIndex, getEndInclusiveIndex(), getTparentId()));
        } else {
            buf.append(
                    String.format("%s%d [label = \"%s\\n%d, %d-%d, tp=%d\"];\n", prefix, id, text, id, startIndex, getEndInclusiveIndex(), getTparentId()));
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
        return this.text != null ? this.text : this.label;
    }

    private String debugNode(String offset) {
        String reString = offset
                + "Label: `" + this.label + "`"
                + " | Name: `" + this.toString() + "`"
                + " | Start: " + this.startIndex
                + " | End: " + this.getEndInclusiveIndex()
                + " | Inserts: " + this.numInserts
                + " | Deletes: " + this.numDeletes;
//                + " | tid: " + tid;

        reString += " | tpid: ";
        if (this.tparent != null) {
//            reString += tparent.tid;
            reString += tparent.id;
        } else {
            reString += "NaN";
        }

        reString += " | tchild: ";
        if (this.tchild != null) {
//            reString += tchild.tid;
            reString += tchild.id;
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
