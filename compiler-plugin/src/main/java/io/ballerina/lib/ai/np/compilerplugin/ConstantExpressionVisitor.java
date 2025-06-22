package io.ballerina.lib.np.compilerplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ConditionalExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.LetExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.NilLiteralNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.OptionalFieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.QueryActionNode;
import io.ballerina.compiler.syntax.tree.QueryExpressionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SingletonTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TableConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TrapExpressionNode;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

class ConstantExpressionVisitor extends NodeVisitor {
    SemanticModel semanticModel;
    Document document;
    private final JsonArray constantExpressionDiagnostics = new JsonArray();

    public ConstantExpressionVisitor(SemanticModel semanticModel, Document document) {
        this.semanticModel = semanticModel;
        this.document = document;
    }

    protected JsonArray checkNonConstExpressions(ExpressionNode expressionNode) {
        checkExpressionNode(expressionNode);
        return filterUniqueMessages(this.constantExpressionDiagnostics);
    }

    private void addConstantReferencesDiagnostics(String name) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", String.format("Error: Generated code should only " +
                "contains constant expressions. (found: '%s')", name));
        this.constantExpressionDiagnostics.add(diagnostic);
    }

    private boolean checkNonConstExpressionNode(ExpressionNode expressionNode) {
        if (expressionNode instanceof BasicLiteralNode || expressionNode instanceof NilLiteralNode
                || expressionNode instanceof SingletonTypeDescriptorNode) {
            return true;
        } else if (expressionNode instanceof SimpleNameReferenceNode
                || expressionNode instanceof QualifiedNameReferenceNode) {
            Optional<Symbol> symbol =  this.semanticModel
                    .symbol(this.document, (expressionNode instanceof SimpleNameReferenceNode ?
                            (SimpleNameReferenceNode) expressionNode : (QualifiedNameReferenceNode) expressionNode)
                            .location().lineRange().startLine());
            return symbol.isEmpty() || !(symbol.get() instanceof ConstantSymbol);
        } else if (expressionNode instanceof TemplateExpressionNode templateExpressionNode) {
            return checkExpressionNode(templateExpressionNode.content().stream().toList());
        } else if (expressionNode instanceof ConditionalExpressionNode conditionalExpressionNode) {
            return checkNonConstExpressionNode(conditionalExpressionNode.lhsExpression())
                    && checkNonConstExpressionNode(conditionalExpressionNode.middleExpression())
                    && checkNonConstExpressionNode(conditionalExpressionNode.endExpression());
        } else if (expressionNode instanceof UnaryExpressionNode unaryExpressionNode) {
            return checkNonConstExpressionNode(unaryExpressionNode.expression());
        } else if (expressionNode instanceof BinaryExpressionNode binaryExpressionNode) {
            return checkExpressionNode(binaryExpressionNode.lhsExpr())
                    && checkExpressionNode(binaryExpressionNode.rhsExpr());
        } else if (expressionNode instanceof TypeCastExpressionNode typeCastExpressionNode) {
            return checkNonConstExpressionNode(typeCastExpressionNode.expression());
        } else if (expressionNode instanceof TypeTestExpressionNode typeTestExpressionNode) {
            return checkNonConstExpressionNode(typeTestExpressionNode.expression());
        } else if (expressionNode instanceof ListConstructorExpressionNode listConstructorExpressionNode) {
            return checkExpressionNode(listConstructorExpressionNode.expressions().stream().toList());
        } else if (expressionNode instanceof MappingConstructorExpressionNode listConstructorExpressionNode) {
            return checkExpressionNode(listConstructorExpressionNode.fields().stream().toList());
        } else if (expressionNode instanceof TableConstructorExpressionNode tableConstructorExpressionNode) {
            return checkExpressionNode(tableConstructorExpressionNode.rows().stream().toList());
        } else if (expressionNode instanceof TupleTypeDescriptorNode typeDescriptorNode) {
            return checkExpressionNode(typeDescriptorNode.memberTypeDesc().stream().toList());
        } else if (expressionNode instanceof IndexedExpressionNode indexedExpressionNode) {
            return checkExpressionNode(indexedExpressionNode.keyExpression().stream().toList())
                    && checkNonConstExpressionNode(indexedExpressionNode.containerExpression());
        } else if (expressionNode instanceof BracedExpressionNode bracedExpressionNode) {
            return checkNonConstExpressionNode(bracedExpressionNode.expression());
        } else if (expressionNode instanceof CheckExpressionNode checkExpressionNode) {
            return checkNonConstExpressionNode(checkExpressionNode.expression());
        } else if (expressionNode instanceof FieldAccessExpressionNode fieldAccessExpressionNode) {
            return checkNonConstExpressionNode(fieldAccessExpressionNode.expression());
        } else if (expressionNode instanceof LetExpressionNode letExpressionNode) {
            return checkNonConstExpressionNode(letExpressionNode.expression());
        } else if (expressionNode instanceof OptionalFieldAccessExpressionNode optionalFieldAccessExpressionNode) {
            return checkNonConstExpressionNode(optionalFieldAccessExpressionNode.expression());
        } else if (expressionNode instanceof QueryActionNode queryActionNode) {
            return checkExpressionNode(queryActionNode.blockStatement());
        } else if (expressionNode instanceof QueryExpressionNode queryExpressionNode) {
            return checkExpressionNode(queryExpressionNode.queryPipeline())
                    && checkExpressionNode(queryExpressionNode.resultClause());
        } else if (expressionNode instanceof TrapExpressionNode trapExpressionNode) {
            return checkNonConstExpressionNode(trapExpressionNode.expression());
        }

        return false;
    }

    private boolean checkExpressionNode(Node node) {
        if (!(node instanceof ExpressionNode expressionNode)) {
            if (node instanceof Token) {
                return true;
            }

            NonTerminalNode nonTerminalNode = (NonTerminalNode) node;
            Iterator var3 = nonTerminalNode.children().iterator();

            boolean isConst = true;
            while (var3.hasNext()) {
                Node child = (Node) var3.next();
                if (!(child instanceof ExpressionNode expNode)) {
                    checkExpressionNode(child);
                    continue;
                }

                if (!checkNonConstExpressionNode(expNode)) {
                    addConstantReferencesDiagnostics(node.toSourceCode());
                    isConst = false;
                }
            }
            return isConst;
        }

        if (!checkNonConstExpressionNode(expressionNode)) {
            addConstantReferencesDiagnostics(node.toSourceCode());
            return false;
        } else {
            return true;
        }
    }

    private boolean checkExpressionNode(List<? extends Node> nodes) {
        for (Node node : nodes) {
            checkExpressionNode(node);
        }
        return true;
    }

    private JsonArray filterUniqueMessages(JsonArray originalArray) {
        if (originalArray.isEmpty()) {
            return originalArray;
        }

        Set<String> seenMessages = new HashSet<>();
        JsonArray uniqueArray = new JsonArray();

        IntStream.range(0, originalArray.size()).mapToObj(i -> originalArray.get(i).getAsJsonObject()).forEach(obj -> {
            String message = obj.getAsJsonPrimitive("message").getAsString();
            if (!seenMessages.contains(message)) {
                seenMessages.add(message);
                uniqueArray.add(obj);
            }
        });

        return uniqueArray;
    }
}
