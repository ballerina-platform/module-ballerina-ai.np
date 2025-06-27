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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;

import java.util.Optional;

/**
 * Validates the external imports and variable references in the generated code.
 *
 * @since 0.3.0
 */
class CodeGenerationValidator extends NodeVisitor {

    private static final String BALLERINA = "ballerina";
    private static final String BALLERINAX = "ballerinax";
    private SemanticModel semanticModel;
    private Optional<Document> document;
    private final JsonArray diagnostics = new JsonArray();
    private final String packageOrgName;

    public CodeGenerationValidator(SemanticModel semanticModel, Optional<Document> document, String packageOrgName) {
        this.semanticModel = semanticModel;
        this.document = document;
        this.packageOrgName = packageOrgName;
    }

    protected JsonArray checkCodeGenerationDiagnostics(ModulePartNode modulePartNode) {
        visit(modulePartNode);
        return this.diagnostics;
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
    public void visit(ImportDeclarationNode importDeclarationNode) {
         importDeclarationNode.orgName().ifPresent(importOrgNameNode -> {
             String orgName = importOrgNameNode.orgName().text();
             if (packageOrgName.equals(orgName)) {
                 return;
             }

             if (!orgName.startsWith(BALLERINA) && !orgName.startsWith(BALLERINAX)) {
                 this.diagnostics.add(
                         addExternalImportDiagnostic(importDeclarationNode)
                 );
             }
         });
    }

    @Override
    public void visit(SimpleNameReferenceNode simpleNameReferenceNode) {
        this.semanticModel.symbol(this.document.get(), simpleNameReferenceNode.location()
                .lineRange().startLine()).ifPresent(symbol -> {
            if (!(symbol instanceof VariableSymbol variableSymbol)) {
                return;
            }
            if (isConfigVariable(variableSymbol)) {
                addConfigVariableReferencesDiagnostics(simpleNameReferenceNode.name());
                return;
            }

            if (isModuleLevelSymbol(symbol)) {
                addModuleVariableReferencesDiagnostics(simpleNameReferenceNode.name());
            }
        });
    }

    @Override
    public void visit(QualifiedNameReferenceNode qualifiedNameReferenceNode) {
        Optional<Symbol> symbolOpt = semanticModel.symbol(qualifiedNameReferenceNode);
        if (symbolOpt.isEmpty()) {
            return;
        }

        Symbol symbol = symbolOpt.get();
        if (isModuleLevelSymbol(symbol) && !isConfigVariable(symbol)) {
            addModuleVariableReferencesDiagnostics(qualifiedNameReferenceNode.identifier());
        }
    }

    private boolean isConfigVariable(Symbol symbol) {
        if (symbol instanceof VariableSymbol variableSymbol) {
            Optional<Qualifier> qualifiersOpt = variableSymbol.qualifiers().stream()
                    .filter(qualifier -> qualifier == Qualifier.CONFIGURABLE)
                    .findFirst();
            if (qualifiersOpt.isEmpty()) {
                return false;
            }

            return true;
        }
        return false;
    }

    private void addModuleVariableReferencesDiagnostics(Token name) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = name.location();
        diagnostic.addProperty("message", String.format("Module level variables " +
                "cannot be used inside the generated code. (found: '%s') in location(start line: %s, end line: %s)",
                name, location.lineRange().startLine().line(), location.lineRange().endLine().line()));
        this.diagnostics.add(diagnostic);
    }

    private void addConfigVariableReferencesDiagnostics(Token name) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = name.location();
        diagnostic.addProperty("message", String.format("Config variables cannot be used " +
                "inside the generated code. (found: '%s') in location(start line: %s, end line: %s)",
                name, location.lineRange().startLine().line(), location.lineRange().endLine().line()));
        this.diagnostics.add(diagnostic);
    }

    private void addInvalidProgramStructureDiagnostic(String kind) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("message", "Invalid code structure detected." +
                " Only function definitions are permitted in the generated code. Found disallowed element: " + kind);
        this.diagnostics.add(diagnostic);
    }

    private static JsonObject addExternalImportDiagnostic(ImportDeclarationNode importNode) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = importNode.location();
        diagnostic.addProperty("message", String.format("Disallowed import '%s' detected in " +
                        "location(start line: %s, end line: %s)," +
                        " only 'ballerina/' or 'ballerinax/' packages are permitted", importNode.toSourceCode(),
                location.lineRange().startLine().line(), location.lineRange().endLine().line()));
        return diagnostic;
    }

    private boolean isModuleLevelSymbol(Symbol symbol) {
        return semanticModel.moduleSymbols().contains(symbol);
    }
}
