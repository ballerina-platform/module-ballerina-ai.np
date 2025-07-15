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
package io.ballerina.lib.ai.np.compilerplugin;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;

/**
 * Class containing common constants and functionality.
 *
 * @since 0.3.0
 */
class Commons {
    private Commons() {
    }

    static final String BALLERINA_ORG_NAME = "ballerina";
    static final String AI_MODULE_NAME = "ai";

    static final String CODE_ANNOTATION = "code";

    static final String BAL_EXT = ".bal";
    static final String FILE_PATH = "filePath";
    static final String CONTENT = "content";

    static boolean isCodeAnnotation(AnnotationNode annotationNode, SemanticModel semanticModel) {
        Node node = annotationNode.annotReference();
        if (!(node instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) ||
                !CODE_ANNOTATION.equals(qualifiedNameReferenceNode.identifier().text())) {
            return false;
        }

        return isLangNaturalModule(semanticModel.symbol(node).get().getModule().get());
    }

    static boolean isLangNaturalModule(ModuleSymbol moduleSymbol) {
        ModuleID moduleId = moduleSymbol.id();
        return BALLERINA_ORG_NAME.equals(moduleId.orgName()) && "lang.natural".equals(moduleId.moduleName());
    }
}
