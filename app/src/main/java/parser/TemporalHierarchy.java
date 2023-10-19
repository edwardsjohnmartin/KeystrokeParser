package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TemporalHierarchy {

    // don't reset this value
    private int nextTid = 0;
    public List<Node> tidToNode = new ArrayList<>();

    // idxInLastSnapshot[i] contains the index in the last snapshot (last edit)
    // for the character currently at index i. -1 if the character was just
    // inserted.
    private List<Integer> idxInLastSnapshot = new ArrayList<>();
    private List<List<Integer>> allIdxInLastSnapshot = new ArrayList<>();

    // idxInLastCompilable[i] contains the index in the last compilable snapshot
    // for the character currently at index i. -1 if the character was inserted
    // since the last compilable event.
    private List<List<Integer>> allIdxInLastCompilable = new ArrayList<>();

    // Whether the last code snapshot was compilable
    // (the AST in the last iteration existed).
    public List<Node> trees = new ArrayList<>();
    public List<Boolean> allCompilable = new ArrayList<>();

    public void temporalCorrespondence(int i, int treeNumber, String insertText, String deleteText) {
        // Setup
        final List<Integer> inserted = new ArrayList<>(Collections.nCopies(insertText.length(), -1));

        // Set indicies for the array: [0, 1, 2, 3, ...]
        for (int index = 0; index < this.idxInLastSnapshot.size(); ++index) {
            this.idxInLastSnapshot.set(index, index);
        }

        // beforeInsert/afterInsert are the mappings of all characters
        // preceding/following the insertion and deletion.
        final List<Integer> beforeInsert = this.idxInLastSnapshot.subList(0, i);
        final List<Integer> afterInsert = this.idxInLastSnapshot.subList(i + deleteText.length(),
                this.idxInLastSnapshot.size());

        // Create the new array.
        this.idxInLastSnapshot = Stream.concat(Stream.concat(beforeInsert.stream(), inserted.stream()),
                afterInsert.stream()).toList();
        this.idxInLastSnapshot = new ArrayList<>(this.idxInLastSnapshot);
        this.allIdxInLastSnapshot.add(new ArrayList<>(this.idxInLastSnapshot));
        this.allIdxInLastCompilable.add(new ArrayList<>(this.idxInLastSnapshot));

        // exit if the last tree was compilable
        if (this.allCompilable.size() > 1 && (this.allCompilable.get(this.allCompilable.size() - 2))) {
            return;
        }

        final var length = allIdxInLastCompilable.get(allIdxInLastCompilable.size() - 1).size();
        for (int k = 0; k < length; ++k) {
            // If the character was not inserted, get the index from the
            // idxInLastCompilable from the last snapshot.
            int previousK = this.idxInLastSnapshot.get(k);

            if (previousK != -1) {
                final int newK = this.allIdxInLastCompilable.get(allIdxInLastCompilable.size() - 2).get(previousK);
                this.allIdxInLastCompilable.get(allIdxInLastCompilable.size() - 1).set(k, newK);
            }
        }
    }

    public void temporalHierarchy(Node tree) {
        // Update tid values. trees is the list of all prase trees to this point.
        this.setTids(tree);
        if (!trees.isEmpty()) {
            this.setAllTparents(tree);
        }
        this.trees.add(tree);

        // Set number of edits since last compilable state for each ast node
        // Note: this currently gets the number of edits since the last snapshot only.
        this.setNumEdits(tree);
    }

    private void setTids(Node tree) {
        if (tree == null) {
            return;
        }

        if (tree.tid == -1) {
            tree.tid = this.nextTid;
            this.tidToNode.add(this.nextTid, tree);
            this.nextTid += 1;
        }

        if (tree.children != null) {
            for (Node child : tree.children) {
                this.setTids(child);
            }
        }
    }

    private void setNumEdits(Node tree) {
        // Initial program state
        if (this.allCompilable.size() == 1 && this.allCompilable.get(0)) {
            tree.numInserts = 0;
            tree.numDeletes = 0;
        }

        // New temporal node
        else if (tree.tparent == null) {
            tree.numInserts += tree.length;
        }

        // Last program state is compilable
        else if (this.allCompilable.get(this.allCompilable.size() - 2)) {
            final int oldLength = tree.tparent.length;
            final int newLength = tree.length;

            tree.numInserts += Math.max(0, newLength - oldLength);
            tree.numDeletes += Math.max(0, oldLength - newLength);
        }

        // Last program state is uncompilable
        else {

            // Find the last compilable state and the current state
            int finalState = this.allCompilable.size() - 1;
            int firstState = this.allCompilable.size() - 2;
            while (!this.allCompilable.get(firstState)) {
                firstState -= 1;
            }

            int start = tree.startIndex;
            int end = tree.endIndex;
            // int end = tree.tparent.length + tree.tparent.startIndex - 1;
            int oldSize = tree.tparent.length;

            // Forward pass (start -> end) for deletions
            for (int index = firstState + 1; index <= finalState; ++index) {
                final List<Integer> idxInLastSnapshot = this.allIdxInLastSnapshot.get(index);

                // Shrink the start & end so they fit in the selection
                int startIndex = Math.max(idxInLastSnapshot.indexOf(start), 0);
                int endIndex = idxInLastSnapshot.indexOf(end);
                while (startIndex == -1 && start < end) {
                    start += 1;
                    startIndex = idxInLastSnapshot.indexOf(start);
                }
                while (endIndex == -1 && start < end) {
                    end -= 1;
                    endIndex = idxInLastSnapshot.indexOf(end);
                }

                // Expand the edges so we gobble up any -1s on the edges
                // [0, 1, 2] -> [0, 1, 2, -1, -1]
                while ((startIndex - 1) >= 0 && idxInLastSnapshot.get(startIndex - 1) == -1) {
                    startIndex -= 1;
                }
                while ((endIndex + 1) < idxInLastSnapshot.size() && idxInLastSnapshot.get(endIndex + 1) == -1) {
                    endIndex += 1;
                }

                final List<Integer> selection = idxInLastSnapshot.subList(startIndex, endIndex + 1);
                final int newSize = selection.size();

                tree.numInserts += Math.max(0, newSize - oldSize);
                tree.numDeletes += Math.max(0, oldSize - newSize);

                oldSize = newSize;
                start = startIndex;
                end = endIndex;
            }
        }

        for (Node child : tree.children) {
            this.setNumEdits(child);
        }
    }

    private void setAllTparents(Node tree) {
        // Get the current indicies for the tree node
        int start = tree.startIndex;
        int end = tree.endIndex;

        // Get the start and end indices in the code as it was
        // when the previous ast was created
        int[] indicies = this.previousStartEndIndicies(start, end,
                this.allIdxInLastCompilable.get(this.allIdxInLastCompilable.size() - 1));
        start = indicies[0];
        end = indicies[1];

        // Traverse through the previous tree looking for a valid t-parent
        final Node prevTree = this.trees.get(this.trees.size() - 1);
        this.setTparent(prevTree, tree, start, end);

        /* TODO: broken as this is no longer true with parse trees */
        // // If no t-parent was found but the node has previous coordinates then
        // // it is likely commented out in the previous tree. Iterate backwards
        // // until we either find a t-parent or the original node (-1 coordinates)
        // int k = -1;
        // int l = -1;
        // while (tree.tparent == null && start > -1) {
        // k -= 1;
        // l -= 1;

        // try {
        // while (l >= -this.allCompilable.size() && !this.allCompilable.get(l)) {
        // l -= 1;
        // }
        // } catch (Exception e) {
        // System.out.println("Node " + tree.label);
        // System.out.println("Expected old coordinates " + start + ", " + end);
        // var x = 1 / 0;
        // }

        // indicies = this.previousStartEndIndicies(start, end,
        // this.allIdxInLastCompilable.get(l));
        // start = indicies[0];
        // end = indicies[1];
        // this.setTparent(this.trees.get(k), tree, start, end);
        // }

        // Make recursive call for all of our children
        for (Node child : tree.children) {
            this.setAllTparents(child);
        }
    }

    private void setTparent(Node prevTree, Node tree, int start, int end) {
        /* TODO: not working some reason */
        // final boolean contains = (prevTree.startIndex <= start && prevTree.endIndex
        // >= end);
        // if (!contains) {
        // return;
        // }

        // if the node type is the same, assume it is the parent
        final boolean sameType = Objects.equals(prevTree.label, tree.label);
        if (prevTree.tchild == null && tree.tparent == null && sameType) {
            tree.tparent = prevTree;
            prevTree.tchild = tree;
            tree.numInserts = prevTree.numInserts;
            tree.numDeletes = prevTree.numDeletes;
        }

        // go deeper to see if there is a better fit
        if (prevTree.children.isEmpty()) {
            return;
        }
        for (Node child : prevTree.children) {
            this.setTparent(child, tree, start, end);
        }

    }

    /* Return a tuple of (start, end) */
    private int[] previousStartEndIndicies(int start, int end, List<Integer> lastCompilable) {
        // Use the last snapshot to find the old indicies
        while (start < end && start < lastCompilable.size() && lastCompilable.get(start) == -1) {
            start += 1;
        }
        while (start < end && end > 0 && lastCompilable.get(end) == -1) {
            end -= 1;
        }

        // Get the start/end for the previous snapshot
        // Add `1` to the end as it moves backwards one too much
        final int previousStart = lastCompilable.get(start);
        final int previousEnd = lastCompilable.get(end) + 1;

        if (previousStart == -1 && previousEnd == -1) {
            return new int[] { -1, -1 };
        } else {
            return new int[] { previousStart, previousEnd };
        }
    }
}
