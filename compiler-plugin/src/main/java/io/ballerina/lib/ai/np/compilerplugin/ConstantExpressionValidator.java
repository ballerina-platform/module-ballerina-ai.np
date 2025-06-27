/*
 *  Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.lib.np.compilerplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.AnnotAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ByteArrayLiteralNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ConditionalExpressionNode;
import io.ballerina.compiler.syntax.tree.DistinctTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ErrorConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ImplicitAnonymousFunctionExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.InterpolationNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.LetExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MapTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.NilLiteralNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalFieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.QueryActionNode;
import io.ballerina.compiler.syntax.tree.QueryExpressionNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SingletonTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TableConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.TransactionalExpressionNode;
import io.ballerina.compiler.syntax.tree.TrapExpressionNode;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeofExpressionNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.XMLFilterExpressionNode;
import io.ballerina.compiler.syntax.tree.XMLStepExpressionNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Validates that the expressions in the generated code are constant expressions.
 *
 * @since 0.3.0
 */
class ConstantExpressionValidator extends NodeVisitor {
    private final JsonArray constantExpressionDiagnostics = new JsonArray();

    protected JsonArray checkNonConstExpressions(Node node) {
        visitSyntaxNode(node);
        return filterUniqueMessages(this.constantExpressionDiagnostics);
    }

    private void addConstantReferencesDiagnostics(String name) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", String.format("Error: Generated code should only " +
                "contains constant expressions. (found: '%s')", name));
        this.constantExpressionDiagnostics.add(diagnostic);
    }

    @Override
    public void visit(AnnotAccessExpressionNode annotAccessExpressionNode) {
        addConstantReferencesDiagnostics(annotAccessExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ArrayTypeDescriptorNode arrayTypeDescriptorNode) {
        addConstantReferencesDiagnostics(arrayTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        // Allowed in constant expressions.
    }

    @Override
    public void visit(BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
        // Allowed in constant expressions.
    }

    @Override
    public void visit(BinaryExpressionNode binaryExpressionNode) {
        visitSyntaxNode(binaryExpressionNode.lhsExpr());
        visitSyntaxNode(binaryExpressionNode.rhsExpr());
    }

    @Override
    public void visit(BracedExpressionNode bracedExpressionNode) {
        visitSyntaxNode(bracedExpressionNode.expression());
    }

    @Override
    public void visit(ByteArrayLiteralNode byteArrayLiteralNode) {
        byteArrayLiteralNode.content().ifPresent(content -> visit(content));
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        addConstantReferencesDiagnostics(checkExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ConditionalExpressionNode conditionalExpressionNode) {
        visitSyntaxNode(conditionalExpressionNode.lhsExpression());
        visitSyntaxNode(conditionalExpressionNode.middleExpression());
        visitSyntaxNode(conditionalExpressionNode.endExpression());
    }

    @Override
    public void visit(DistinctTypeDescriptorNode distinctTypeDescriptorNode) {
        addConstantReferencesDiagnostics(distinctTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(ErrorConstructorExpressionNode errorConstructorExpressionNode) {
        addConstantReferencesDiagnostics(errorConstructorExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ExplicitAnonymousFunctionExpressionNode explicitAnonymousFunctionExpressionNode) {
        addConstantReferencesDiagnostics(explicitAnonymousFunctionExpressionNode.functionKeyword().toSourceCode()
                + explicitAnonymousFunctionExpressionNode.functionSignature().toSourceCode()
                + explicitAnonymousFunctionExpressionNode.functionBody().toSourceCode());
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
       addConstantReferencesDiagnostics(explicitNewExpressionNode.toSourceCode());
    }

    @Override
    public void visit(FieldAccessExpressionNode fieldAccessExpressionNode) {
        visitSyntaxNode(fieldAccessExpressionNode.expression());
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        addConstantReferencesDiagnostics(functionCallExpressionNode.toSourceCode());
    }

    @Override
    public void visit(FunctionTypeDescriptorNode functionTypeDescriptorNode) {
        addConstantReferencesDiagnostics(functionTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(ImplicitAnonymousFunctionExpressionNode implicitAnonymousFunctionExpressionNode) {
        addConstantReferencesDiagnostics(implicitAnonymousFunctionExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        addConstantReferencesDiagnostics(implicitNewExpressionNode.toSourceCode());
    }

    @Override
    public void visit(IndexedExpressionNode indexedExpressionNode) {
        visitSyntaxNode(indexedExpressionNode.containerExpression());
        indexedExpressionNode.keyExpression().forEach(node -> visitSyntaxNode(node));
    }

    @Override
    public void visit(IntersectionTypeDescriptorNode intersectionTypeDescriptorNode) {
        addConstantReferencesDiagnostics(intersectionTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(LetExpressionNode letExpressionNode) {
        addConstantReferencesDiagnostics(letExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ListConstructorExpressionNode listConstructorExpressionNode) {
        listConstructorExpressionNode.expressions().forEach(exp -> visitSyntaxNode(exp));
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingConstructorExpressionNode) {
        mappingConstructorExpressionNode.fields().forEach(field -> visitSyntaxNode(field));
    }

    @Override
    public void visit(MapTypeDescriptorNode mapTypeDescriptorNode) {
        addConstantReferencesDiagnostics(mapTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(MethodCallExpressionNode methodCallExpressionNode) {
        visitSyntaxNode(methodCallExpressionNode.expression());
        methodCallExpressionNode.arguments().forEach(arg -> visitSyntaxNode(arg));
    }

    @Override
    public void visit(SimpleNameReferenceNode nameReferenceNode) {
        addConstantReferencesDiagnostics(nameReferenceNode.toSourceCode());
    }

    @Override
    public void visit(QualifiedNameReferenceNode nameReferenceNode) {
        addConstantReferencesDiagnostics(nameReferenceNode.toSourceCode());
    }

    @Override
    public void visit(NaturalExpressionNode naturalExpressionNode) {
        addConstantReferencesDiagnostics(naturalExpressionNode.toSourceCode());
    }

    @Override
    public void visit(NilLiteralNode nilLiteralNode) {
        // Allowed in constant expressions.
    }

    @Override
    public void visit(ObjectConstructorExpressionNode objectConstructorExpressionNode) {
        addConstantReferencesDiagnostics(objectConstructorExpressionNode.toSourceCode());
    }

    @Override
    public void visit(ObjectTypeDescriptorNode objectTypeDescriptorNode) {
        addConstantReferencesDiagnostics(objectTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(OptionalFieldAccessExpressionNode optionalFieldAccessExpressionNode) {
        visitSyntaxNode(optionalFieldAccessExpressionNode.expression());
        visitSyntaxNode(optionalFieldAccessExpressionNode.fieldName());
    }

    @Override
    public void visit(QueryExpressionNode queryExpressionNode) {
        addConstantReferencesDiagnostics(queryExpressionNode.toSourceCode());
    }

    @Override
    public void visit(QueryActionNode queryActionNode) {
        addConstantReferencesDiagnostics(queryActionNode.toSourceCode());
    }

    @Override
    public void visit(RecordTypeDescriptorNode recordTypeDescriptorNode) {
        addConstantReferencesDiagnostics(recordTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(SingletonTypeDescriptorNode singletonTypeDescriptorNode) {
        // Allowed
    }

    @Override
    public void visit(StreamTypeDescriptorNode streamTypeDescriptorNode) {
        addConstantReferencesDiagnostics(streamTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(TableConstructorExpressionNode tableConstructorExpressionNode) {
        tableConstructorExpressionNode.keySpecifier().ifPresent(node -> visitSyntaxNode(node));
        tableConstructorExpressionNode.rows().forEach(row -> visitSyntaxNode(row));
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        templateExpressionNode.content().forEach(node -> {
            if (node instanceof InterpolationNode interpolationNode) {
                visitSyntaxNode(interpolationNode.expression());
            }
        });
    }

    @Override
    public void visit(TransactionalExpressionNode transactionalExpressionNode) {
        addConstantReferencesDiagnostics(transactionalExpressionNode.toSourceCode());
    }

    @Override
    public void visit(TrapExpressionNode trapExpressionNode) {
        addConstantReferencesDiagnostics(trapExpressionNode.toSourceCode());
    }

    @Override
    public void visit(TupleTypeDescriptorNode tupleTypeDescriptorNode) {
        addConstantReferencesDiagnostics(tupleTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(TypeCastExpressionNode typeCastExpressionNode) {
        visitSyntaxNode(typeCastExpressionNode.expression());
    }

    @Override
    public void visit(TypeofExpressionNode typeofExpressionNode) {
        visitSyntaxNode(typeofExpressionNode.expression());
    }

    @Override
    public void visit(TypeTestExpressionNode typeTestExpressionNode) {
        visitSyntaxNode(typeTestExpressionNode.expression());
    }

    @Override
    public void visit(UnaryExpressionNode unaryExpressionNode) {
        visitSyntaxNode(unaryExpressionNode.expression());
    }

    @Override
    public void visit(UnionTypeDescriptorNode unionTypeDescriptorNode) {
        addConstantReferencesDiagnostics(unionTypeDescriptorNode.toSourceCode());
    }

    @Override
    public void visit(XMLFilterExpressionNode xmlFilterExpressionNode) {
        addConstantReferencesDiagnostics(xmlFilterExpressionNode.toSourceCode());
    }

    @Override
    public void visit(XMLStepExpressionNode xmlStepExpressionNode) {
        addConstantReferencesDiagnostics(xmlStepExpressionNode.toSourceCode());
    }

    public void visit(ExpressionNode expressionNode) {
        super.visitSyntaxNode(expressionNode);
    }

    public void visitSyntaxNode(Node node) {
        if (node instanceof ExpressionNode) {
            node.accept(this);
            return;
        }
        super.visitSyntaxNode(node);
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
