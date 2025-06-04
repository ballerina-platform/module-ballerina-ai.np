package io.ballerina.lib.np.compilerplugin;

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinalang.model.tree.expressions.VariableReferenceNode;

class ConfigurableVariableVisitor extends NodeVisitor {
    SemanticModel semanticModel;
    private final JsonArray configVariablesRelatedDiagnostics = new JsonArray();

    public ConfigurableVariableVisitor(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    protected JsonArray getConfigVariablesRelatedDiagnostics(ModulePartNode modulePartNode) {
        visit(modulePartNode);
        return this.configVariablesRelatedDiagnostics;
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        modulePartNode.members().forEach(member -> {
            if (member.kind() != SyntaxKind.FUNCTION_DEFINITION) {
                this.configVariablesRelatedDiagnostics.add(
                        "Error: Invalid code structure detected. Only function definitions are permitted in " +
                        "the generated code. Found disallowed element: " + member.kind().name()
                );
            }
        });
        super.visitSyntaxNode(modulePartNode);
    }

    @Override
    protected void visitSyntaxNode(Node node) {
        if (node instanceof VariableReferenceNode) {
            this.semanticModel.symbol(node).ifPresent(symbol -> {
                if (symbol instanceof VariableSymbol variableSymbol) {
                    variableSymbol.qualifiers().stream()
                            .filter(qualifier -> qualifier == Qualifier.CONFIGURABLE)
                            .findFirst()
                            .ifPresent(qualifier -> {
                                this.configVariablesRelatedDiagnostics.add(
                                    String.format("Error: Config variables cannot be used inside the generated code. " +
                                            "(found: '%s')", variableSymbol.getName())
                                );
                            });
                }
            });
        }

        super.visitSyntaxNode(node);
    }
}