package parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import parser.antlr.PythonParser;

import java.util.ArrayList;
import java.util.List;

public class MyVisitor {

    public static Node toSimpleTree(PythonParser.RootContext ctx) {
//        final int id = nextId();
        final String label = ctx.getClass().getSimpleName();
        final int startIndex = ctx.getStart().getStartIndex();
        final int length = ctx.getStop().getStopIndex() - startIndex + 1;

        Node n = toSimpleTreeImpl(ctx, label, startIndex, length);
        n.setIsReference(true);
        return n;
    }

    private static Node toSimpleTreeImpl(ParserRuleContext tree, String parentLabel, int parentStartIndex, int parentLength) {
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < tree.getChildCount(); i++) {
            Node childNode;
            String childLabel;
            int childStartIndex, childLength;

            if (tree.getChild(i) instanceof ParserRuleContext) {
                ParserRuleContext child = (ParserRuleContext)tree.getChild(i);
                childLabel = child.getClass().getSimpleName();
                childStartIndex = child.getStart().getStartIndex();
                childLength = child.getStop().getStopIndex() - childStartIndex + 1;

                childNode = toSimpleTreeImpl(child, childLabel, childStartIndex, childLength);
            } else {
                TerminalNode child = (TerminalNode) tree.getChild(i);
                childLabel = child.toString();
                childStartIndex = ((ParserRuleContext) child.getParent()).getStart().getStartIndex();
                childNode = new Node(childLabel, childStartIndex, childLabel.length(), new ArrayList<>());
            }
            children.add(childNode);
        }
        return new Node(parentLabel, parentStartIndex, parentLength, children);
    }
}
