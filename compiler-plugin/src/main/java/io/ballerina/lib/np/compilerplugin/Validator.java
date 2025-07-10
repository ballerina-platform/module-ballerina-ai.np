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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

import static io.ballerina.lib.np.compilerplugin.Commons.ORG_NAME;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode
        .CODE_GEN_WITH_CODE_ANNOT_NOT_YET_SUPPORTED;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode.CONST_NATURAL_EXPR_NOT_YET_SUPPORTED;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.DiagnosticCode.NON_JSON_EXPECTED_TYPE_NOT_YET_SUPPORTED;
import static io.ballerina.lib.np.compilerplugin.DiagnosticLog.reportError;

/**
 * Natural programming function signature validator.
 *
 * @since 0.3.0
 */
public class Validator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final String CODE_ANNOTATION = "code";

    private final CodeModifier.AnalysisData analysisData;
    private Optional<TypeSymbol> jsonOrErrorType = Optional.empty();

    Validator(CodeModifier.AnalysisData analysisData) {
        this.analysisData = analysisData;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        SemanticModel semanticModel = ctx.semanticModel();
        Types types = semanticModel.types();

        Package currentPackage = ctx.currentPackage();
        ModuleId moduleId = ctx.moduleId();
        Module module = currentPackage.module(moduleId);
        Document document = module.document(ctx.documentId());

        Node node = ctx.node();
        if (node instanceof NaturalExpressionNode naturalExpressionNode) {
            validateNaturalExpression(semanticModel, types, document, naturalExpressionNode, ctx);
            return;
        }

        if (node instanceof AnnotationNode annotationNode) {
            validateCompileTimeCodeGenAnnotation(semanticModel, annotationNode, ctx);
        }
    }

    private void validateNaturalExpression(SemanticModel semanticModel,
                                           Types types, Document document,
                                           NaturalExpressionNode naturalExpressionNode,
                                           SyntaxNodeAnalysisContext ctx) {
        if (naturalExpressionNode.constKeyword().isPresent()) {
            reportError(ctx, this.analysisData, naturalExpressionNode.location(), CONST_NATURAL_EXPR_NOT_YET_SUPPORTED);
            return;
        }

        validateExpectedType(naturalExpressionNode.location(),
                semanticModel.expectedType(document, naturalExpressionNode.lineRange().startLine()).get(),
                types, ctx);
    }

    private void validateCompileTimeCodeGenAnnotation(SemanticModel semanticModel, AnnotationNode annotationNode,
                                                      SyntaxNodeAnalysisContext ctx) {
        Node node = annotationNode.annotReference();
        if (!(node instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) ||
                !CODE_ANNOTATION.equals(qualifiedNameReferenceNode.identifier().text())) {
            return;
        }

        if (isLangNaturalModule(semanticModel.symbol(node).get().getModule().get())) {
            reportError(ctx, this.analysisData, annotationNode.location(), CODE_GEN_WITH_CODE_ANNOT_NOT_YET_SUPPORTED);
        }
    }

    private void validateExpectedType(Location location, TypeSymbol expectedType, Types types,
                                      SyntaxNodeAnalysisContext ctx) {
        if (!expectedType.subtypeOf(getJsonOrErrorType(types))) {
            reportError(ctx, this.analysisData, location, NON_JSON_EXPECTED_TYPE_NOT_YET_SUPPORTED);
        }
    }

    private TypeSymbol getJsonOrErrorType(Types types) {
        if (this.jsonOrErrorType.isPresent()) {
            return this.jsonOrErrorType.get();
        }

        TypeSymbol jsonOrErrorType = types.builder().UNION_TYPE.withMemberTypes(types.JSON, types.ERROR).build();
        this.jsonOrErrorType = Optional.of(jsonOrErrorType);
        return jsonOrErrorType;
    }

    private static boolean isLangNaturalModule(ModuleSymbol moduleSymbol) {
        ModuleID moduleId = moduleSymbol.id();
        return ORG_NAME.equals(moduleId.orgName()) && "lang.natural".equals(moduleId.moduleName());
    }
}
