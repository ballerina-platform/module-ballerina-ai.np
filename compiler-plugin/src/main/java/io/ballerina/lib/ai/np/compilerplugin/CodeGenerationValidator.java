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
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
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
            addDiagnosticsForVariableReference(symbol, simpleNameReferenceNode.name());
        });
    }

    @Override
    public void visit(QualifiedNameReferenceNode qualifiedNameReferenceNode) {
        this.semanticModel.symbol(this.document.get(), qualifiedNameReferenceNode.identifier().location()
                .lineRange().startLine()).ifPresent(symbol -> {
            addDiagnosticsForVariableReference(symbol, qualifiedNameReferenceNode);
        });
    }

    private boolean isConfigVariable(Symbol symbol) {
        return symbol instanceof VariableSymbol variableSymbol &&
            variableSymbol.qualifiers().stream()
                    .anyMatch(qualifier -> qualifier == Qualifier.CONFIGURABLE);
    }

    private void addModuleVariableReferencesDiagnostics(Node node) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = node.location();
        diagnostic.addProperty("message", String.format("ERROR [%s:(%s:%s,%s:%s)] Module level variables " +
                "cannot be used inside the generated code. (found: '%s')",
            document.get().name(), location.lineRange().startLine().line(),
            location.lineRange().startLine().offset(), location.lineRange().endLine().line(),
            location.lineRange().endLine().offset(), node.toSourceCode()));
        this.diagnostics.add(diagnostic);
    }

    private void addConfigVariableReferencesDiagnostics(Node node) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = node.location();
        diagnostic.addProperty("message", String.format("ERROR [%s:(%s:%s,%s:%s)] Config variables" +
                " cannot be used inside the generated code. (found: '%s')",
            document.get().name(), location.lineRange().startLine().line(),
            location.lineRange().startLine().offset(), location.lineRange().endLine().line(),
            location.lineRange().endLine().offset(), node.toSourceCode()));
        this.diagnostics.add(diagnostic);
    }

    private JsonObject addExternalImportDiagnostic(ImportDeclarationNode importNode) {
        JsonObject diagnostic = new JsonObject();
        NodeLocation location = importNode.location();
        diagnostic.addProperty("message", String.format("ERROR [%s:(%s:%s,%s:%s)] Disallowed import '%s'" +
                " detected in location, only 'ballerina/' or 'ballerinax/' " +
                "packages are permitted", document.get().name(), location.lineRange().startLine().line(),
            location.lineRange().startLine().offset(), location.lineRange().endLine().line(),
            location.lineRange().endLine().offset(), importNode.toSourceCode()));
        return diagnostic;
    }

    private boolean isModuleLevelSymbol(Symbol symbol) {
        return semanticModel.moduleSymbols().contains(symbol);
    }

    private void addDiagnosticsForVariableReference(Symbol symbol, Node node) {
        if (!(symbol instanceof VariableSymbol variableSymbol)) {
            return;
        }
        if (isConfigVariable(variableSymbol)) {
            addConfigVariableReferencesDiagnostics(node);
            return;
        }

        if (isModuleLevelSymbol(symbol)) {
            addModuleVariableReferencesDiagnostics(node);
        }
    }
}
