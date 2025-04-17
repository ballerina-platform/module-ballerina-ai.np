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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IncludedRecordParameterNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.Optional;

/**
 * Class containing common constants and functionality.
 *
 * @since 0.3.0
 */
class Commons {
    static final String ORG_NAME = "ballerina";
    static final String MODULE_NAME = "np";
    static final String NATURAL_FUNCTION_ANNOT = "NaturalFunction";

    static boolean hasNaturalFunctionAnnotation(ExternalFunctionBodyNode functionBody, String modulePrefix) {
        return hasAnnotation(functionBody, modulePrefix, NATURAL_FUNCTION_ANNOT);
    }

    static boolean isRuntimeNaturalExpression(ExpressionNode expressionNode) {
        return expressionNode instanceof NaturalExpressionNode naturalExpressionNode &&
                naturalExpressionNode.constKeyword().isEmpty();
    }

    static boolean hasAnnotation(ExternalFunctionBodyNode functionBody, String modulePrefix,
                                 String annotation) {
        final String annotationRef = modulePrefix + ":" + annotation;
        return functionBody.annotations().stream().
                anyMatch(annotationNode -> annotationNode.annotReference().toString().trim()
                        .equals(annotationRef));
    }

    static Optional<ModuleSymbol> findNPModule(SemanticModel semanticModel, ModulePartNode rootNode) {
        for (ImportDeclarationNode importDeclarationNode : rootNode.imports()) {
            Optional<Symbol> symbolOptional = semanticModel.symbol(importDeclarationNode);
            if (symbolOptional.isEmpty()) {
                continue;
            }

            Symbol symbol = symbolOptional.get();
            if (symbol instanceof ModuleSymbol moduleSymbol && isNPModule(moduleSymbol)) {
                return Optional.of(moduleSymbol);
            }
        }
        return Optional.empty();
    }

    static boolean isNPModule(ModuleSymbol moduleSymbol) {
        ModuleID moduleId = moduleSymbol.id();
        return ORG_NAME.equals(moduleId.orgName()) && MODULE_NAME.equals(moduleId.moduleName());
    }

    static Node getParameterType(ParameterNode parameter, SyntaxKind kind) {
        return switch (kind) {
            case REQUIRED_PARAM -> ((RequiredParameterNode) parameter).typeName();
            case DEFAULTABLE_PARAM -> ((DefaultableParameterNode) parameter).typeName();
            case INCLUDED_RECORD_PARAM -> ((IncludedRecordParameterNode) parameter).typeName();
            default -> ((RestParameterNode) parameter).typeName();
        };
    }

    static String getParameterName(ParameterNode parameter, SyntaxKind kind) {
        return switch (kind) {
            case REQUIRED_PARAM -> ((RequiredParameterNode) parameter).paramName().get().text();
            case DEFAULTABLE_PARAM -> ((DefaultableParameterNode) parameter).paramName().get().text();
            case INCLUDED_RECORD_PARAM -> ((IncludedRecordParameterNode) parameter).paramName().get().text();
            default -> ((RestParameterNode) parameter).paramName().get().text();
        };
    }
}
