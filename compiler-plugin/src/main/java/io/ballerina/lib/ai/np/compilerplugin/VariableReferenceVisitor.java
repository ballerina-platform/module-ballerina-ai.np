package io.ballerina.lib.np.compilerplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;

import java.util.List;
import java.util.Optional;

class VariableReferenceVisitor extends NodeVisitor {
    SemanticModel semanticModel;
    Optional<Document> document;
    private final JsonArray unsafeVariableReferencesRelatedDiagnostics = new JsonArray();

    public VariableReferenceVisitor(SemanticModel semanticModel, Optional<Document> document) {
        this.semanticModel = semanticModel;
        this.document = document;
    }

    protected JsonArray checkUnsafeVariableReferences(ModulePartNode modulePartNode) {
        visit(modulePartNode);
        return this.unsafeVariableReferencesRelatedDiagnostics;
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        modulePartNode.members().forEach(member -> {
            if (member.kind() != SyntaxKind.FUNCTION_DEFINITION) {
                addInvalidProgramStructureDiagnostic(member.kind().stringValue());
            }
        });

        if (this.document.isEmpty()) {
            return;
        }
        super.visitSyntaxNode(modulePartNode);
    }

    @Override
    public void visit(SimpleNameReferenceNode simpleNameReferenceNode) {
        this.semanticModel.symbol(this.document.get(), simpleNameReferenceNode.location()
                .lineRange().startLine()).ifPresent(symbol -> {
            if (symbol instanceof VariableSymbol variableSymbol) {
                variableSymbol.qualifiers().stream()
                        .filter(qualifier -> qualifier == Qualifier.CONFIGURABLE)
                        .findFirst()
                        .ifPresent(qualifier -> addConfigVariableReferencesDiagnostics(simpleNameReferenceNode.name()));

                boolean isModuleLevelSymbol = semanticModel.moduleSymbols().contains(variableSymbol);
                if (isModuleLevelSymbol) {
                    addModuleVariableReferencesDiagnostics(simpleNameReferenceNode.name());
                }
            }
        });
    }

    @Override
    public void visit(QualifiedNameReferenceNode qualifiedNameReferenceNode) {
        Optional<Symbol> symbol = semanticModel.symbol(qualifiedNameReferenceNode);
        if (symbol.isEmpty()) {
            return;
        }

        boolean isModuleLevelSymbol = semanticModel.moduleSymbols().contains(symbol.get());
        if (isModuleLevelSymbol) {
            addModuleVariableReferencesDiagnostics(qualifiedNameReferenceNode.identifier());
        }
    }

    private void addModuleVariableReferencesDiagnostics(Token name) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", String.format("Error: Module level variables " +
                "cannot be used inside the generated code. (found: '%s')", name));
        this.unsafeVariableReferencesRelatedDiagnostics.add(diagnostic);
    }

    private void addConfigVariableReferencesDiagnostics(Token name) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", String.format("Error: Config variables cannot be used " +
                "inside the generated code. (found: '%s')", name));
        this.unsafeVariableReferencesRelatedDiagnostics.add(diagnostic);
    }

    private void addInvalidProgramStructureDiagnostic(String kind) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", "Error: Invalid code structure detected." +
                " Only function definitions are permitted in the generated code. Found disallowed element: " + kind);
        this.unsafeVariableReferencesRelatedDiagnostics.add(diagnostic);
    }
}
