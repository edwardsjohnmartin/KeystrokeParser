package parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import parser.antlr.PythonParser;
import parser.antlr.PythonParserBaseVisitor;

public class MyVisitor extends PythonParserBaseVisitor<Void> {
    @Override public Void visitRoot(PythonParser.RootContext ctx) {
        visitNode(ctx);
//        return visitChildren(ctx);
        return null;
    }

    private void visitNode(ParserRuleContext tree) {
//        System.out.println(tree.getChild(0));
        String parentString = tree.getClass().getSimpleName() + tree.getStart().getStartIndex();
        String childString;
        for (int i = 0; i < tree.getChildCount(); i++) {
            if (tree.getChild(i) instanceof ParserRuleContext) {
                ParserRuleContext child = (ParserRuleContext)tree.getChild(i);
                childString = child.getClass().getSimpleName() + child.getStart().getStartIndex();
//                System.out.println(nodeString);
                visitNode(child);
            } else {
                TerminalNode child = (TerminalNode) tree.getChild(i);
                childString = child.toString() + ((ParserRuleContext) child.getParent()).getStart().getStartIndex();
//                System.out.println(nodeString);
            }
            System.out.println("\"" + parentString + "\"" + "->" + "\"" + childString + "\"");
        }
    }
}
