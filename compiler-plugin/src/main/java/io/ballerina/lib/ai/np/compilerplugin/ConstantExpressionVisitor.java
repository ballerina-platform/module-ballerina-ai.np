package io.ballerina.lib.np.compilerplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.ConditionalExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;

import java.util.List;
import java.util.Optional;

class ConstantExpressionVisitor extends NodeVisitor {
    SemanticModel semanticModel;
    private final JsonArray constantExpressionDiagnostics = new JsonArray();

    public ConstantExpressionVisitor(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    protected JsonArray checkNonConstExpressions(ModulePartNode modulePartNode) {
        visit(modulePartNode);
        return this.constantExpressionDiagnostics;
    }

    @Override
    protected void visitSyntaxNode(Node node) {
        if (!isConstExpressionNode(node)) {
            addConstantReferencesDiagnostics(node.toString());
        }

        super.visitSyntaxNode(node);
    }

    private void addConstantReferencesDiagnostics(String name) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", String.format("Error: Generated code should only " +
                "contains constant expressions. (found: '%s')", name));
        this.constantExpressionDiagnostics.add(diagnostic);
    }

    private boolean isConstExpressionNode(Node node) {
        if (!(node instanceof ExpressionNode expressionNode)) {
            return true;
        }

        if (expressionNode instanceof BasicLiteralNode) {
            return true;
        } else if (expressionNode instanceof SimpleNameReferenceNode
                || expressionNode instanceof QualifiedNameReferenceNode) {
            Optional<Symbol> symbol = this.semanticModel.symbol(expressionNode);
            return symbol.isEmpty() || !(symbol.get() instanceof ConstantSymbol) ;
        } else if (expressionNode instanceof TemplateExpressionNode templateExpressionNode) {
            return checkNestedConstExpr(templateExpressionNode.content().stream().toList());
        } else if (expressionNode instanceof ConditionalExpressionNode conditionalExpressionNode) {
            return isConstExpressionNode(conditionalExpressionNode.lhsExpression())
                    && isConstExpressionNode(conditionalExpressionNode.middleExpression())
                    && isConstExpressionNode(conditionalExpressionNode.endExpression());
        } else if (expressionNode instanceof UnaryExpressionNode unaryExpressionNode) {
            return isConstExpressionNode(unaryExpressionNode.expression());
        } else if (expressionNode instanceof BinaryExpressionNode binaryExpressionNode) {
            return isConstExpressionNode(binaryExpressionNode.lhsExpr())
                    && isConstExpressionNode(binaryExpressionNode.rhsExpr());
        } else if (expressionNode instanceof TypeCastExpressionNode typeCastExpressionNode) {
            return isConstExpressionNode(typeCastExpressionNode.expression());
        } else if (expressionNode instanceof TypeTestExpressionNode typeTestExpressionNode) {
            return isConstExpressionNode(typeTestExpressionNode.expression());
        } else if (expressionNode instanceof ListConstructorExpressionNode listConstructorExpressionNode) {
            return checkNestedConstExpr(listConstructorExpressionNode.expressions().stream().toList());
        }

        return false;
    }

    private boolean checkNestedConstExpr(List<Node> nodes) {
        for (Node node : nodes) {
            if (!(node instanceof ExpressionNode)) {
                continue;
            }

            if (!isConstExpressionNode(node)) {
                return false;
            }
        }
        return true;
    }
}
