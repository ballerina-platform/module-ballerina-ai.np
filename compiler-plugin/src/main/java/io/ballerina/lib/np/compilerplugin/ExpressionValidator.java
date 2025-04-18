/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.np.compilerplugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

import static io.ballerina.lib.np.compilerplugin.Commons.MODULE_NAME;
import static io.ballerina.lib.np.compilerplugin.Commons.ORG_NAME;
import static io.ballerina.lib.np.compilerplugin.Commons.VERSION;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode.EXPECTED_A_SUBTYPE_OF_NP_MODEL;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode.NON_JSON_EXPECTED_TYPE_NOT_YET_SUPPORTED;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode.UNEXPECTED_ARGUMENTS;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.reportError;

/**
 * Natural programming function signature validator.
 *
 * @since 0.3.0
 */
public class ExpressionValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final CodeModifier.AnalysisData analysisData;
    private static final String MODEL_TYPE = "Model";

    ExpressionValidator(CodeModifier.AnalysisData analysisData) {
        this.analysisData = analysisData;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        SemanticModel semanticModel = ctx.semanticModel();
        Optional<Symbol> modelSymbol = semanticModel.types().getTypeByName(ORG_NAME, MODULE_NAME, VERSION, MODEL_TYPE);
        TypeSymbol jsonType = semanticModel.types().JSON;

        Package currentPackage = ctx.currentPackage();
        ModuleId moduleId = ctx.moduleId();
        Module module = currentPackage.module(moduleId);
        Document document = module.document(ctx.documentId());

        Node node = ctx.node();
        if (node instanceof NaturalExpressionNode naturalExpressionNode) {
            validateNaturalExpression(semanticModel, document, naturalExpressionNode, jsonType, modelSymbol, ctx);
            return;
        }

        validateLlmCallExpression((FunctionCallExpressionNode) node, jsonType, ctx);
    }

    private void validateNaturalExpression(SemanticModel semanticModel,
                                           Document document,
                                           NaturalExpressionNode naturalExpressionNode,
                                           TypeSymbol jsonType, Optional<Symbol> modelSymbol,
                                           SyntaxNodeAnalysisContext ctx) {
        validateArguments(ctx, semanticModel, naturalExpressionNode.parenthesizedArgList(), modelSymbol);
        validateExpectedType(naturalExpressionNode.location(),
                semanticModel.expectedType(document, naturalExpressionNode.lineRange().startLine()), jsonType, ctx);
    }

    private void validateArguments(SyntaxNodeAnalysisContext ctx,
                                   SemanticModel semanticModel,
                                   Optional<ParenthesizedArgList> parenthesizedArgListOptional,
                                   Optional<Symbol> modelSymbol) {
        if (parenthesizedArgListOptional.isEmpty()) {
            return;
        }

        ParenthesizedArgList parenthesizedArgList = parenthesizedArgListOptional.get();
        SeparatedNodeList<FunctionArgumentNode> argList = parenthesizedArgList.arguments();
        int argListSize = argList.size();

        if (argListSize == 0) {
            return;
        }

        if (argListSize > 1) {
            reportError(ctx, this.analysisData, parenthesizedArgList.location(), UNEXPECTED_ARGUMENTS, argListSize);
        }

        if (modelSymbol.isEmpty()) {
            return;
        }

        FunctionArgumentNode arg0 = argList.get(0);
        Optional<TypeSymbol> argType = semanticModel.typeOf(arg0.lineRange());
        if (argType.isEmpty()) {
            return;
        }

        TypeSymbol symbol = argType.get();
        if (!symbol.subtypeOf(((TypeDefinitionSymbol) modelSymbol.get()).typeDescriptor())) {
            reportError(ctx, this.analysisData, arg0.location(), EXPECTED_A_SUBTYPE_OF_NP_MODEL, symbol.signature());
        }
    }

    private void validateLlmCallExpression(FunctionCallExpressionNode functionCall, TypeSymbol jsonType,
                                           SyntaxNodeAnalysisContext ctx) {
    }

    private void validateExpectedType(Location location, Optional<TypeSymbol> expectedType, TypeSymbol jsonType,
                                      SyntaxNodeAnalysisContext ctx) {
        TypeSymbol expectedTypeSymbol = expectedType.get();
        if (expectedTypeSymbol instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
            expectedTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();
        }

        if (!(expectedTypeSymbol instanceof UnionTypeSymbol unionTypeSymbol)) {
            if (expectedTypeSymbol.typeKind() != TypeDescKind.ERROR && expectedTypeSymbol.subtypeOf(jsonType)) {
                reportError(ctx, this.analysisData, location, NON_JSON_EXPECTED_TYPE_NOT_YET_SUPPORTED);
            }
            return;
        }

        for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
            if (memberTypeDescriptor instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
                memberTypeDescriptor = typeReferenceTypeSymbol.typeDescriptor();
            }

            if (memberTypeDescriptor.typeKind() != TypeDescKind.ERROR && !memberTypeDescriptor.subtypeOf(jsonType)) {
                reportError(ctx, this.analysisData, location, NON_JSON_EXPECTED_TYPE_NOT_YET_SUPPORTED);
            }
        }
    }
}
