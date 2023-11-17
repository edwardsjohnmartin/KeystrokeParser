package parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import parser.antlr.PythonParser;

import java.util.ArrayList;
import java.util.List;

public class MyVisitor {

    public static Node toSimpleTree(PythonParser.RootContext ctx, final String src) {
        // final int id = nextId();
        final String label = ctx.getClass().getSimpleName();
        final int startIndex = ctx.getStart().getStartIndex();
        final int stopIndex = ctx.getStop().getStopIndex();
        final int length = stopIndex - startIndex + 1;

        // skip EOFs
        if ("<EOF>".equals(ctx.getText())) {
            return null;
        }

        Node n = toSimpleTreeImpl(ctx, label, startIndex, stopIndex, length, src);
        n.setIsReference(true);
        return n;
    }

    private static Node toSimpleTreeImpl(ParserRuleContext tree,
                                         String parentLabel,
                                         int parentStartIndex,
                                         int parentStopIndex,
                                         int parentLength,
                                         final String src) {

        List<Node> children = new ArrayList<>();

        for (int i = 0; i < tree.getChildCount(); i++) {

            if (tree.getChild(i) instanceof ParserRuleContext) {
                ParserRuleContext child = (ParserRuleContext) tree.getChild(i);
                final String childLabel = child.getClass().getSimpleName();
                final int childStartIndex = child.getStart().getStartIndex();
                final int childStopIndex = child.getStop().getStopIndex();
                final int childLength = childStopIndex - childStartIndex + 1;

                final Node childNode = toSimpleTreeImpl(child, childLabel, childStartIndex, childStopIndex,
                        childLength, src);
                children.add(childNode);
            } else {
                TerminalNode child = (TerminalNode) tree.getChild(i);

                // skip EOFs
                if ("<EOF>".equals(child.toString()) || "".equals(child.toString())) {
                    continue;
                }

                final String childLabel = "Terminal";
                // final int childStartIndex = ((ParserRuleContext)
                // child.getParent()).getStart().getStartIndex();
                int childStartIndex = child.getSymbol().getStartIndex();
                int childStopIndex = child.getSymbol().getStopIndex();

                // hack? TODO: investigate
                if (childStartIndex > childStopIndex) {
                    childStartIndex = childStopIndex;
                }

                final Node childNode = new Node(childLabel, childStartIndex, childStopIndex, child.toString().length(),
                        new ArrayList<>(), src);
                children.add(childNode);
            }
        }

        return new Node(parentLabel, parentStartIndex, parentStopIndex, parentLength, children, src);
    }
}
