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

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Class containing common constants and functionality.
 *
 * @since 0.3.0
 */
public class CommonUtils {
    private CommonUtils() {
    }

    public static final String BALLERINA_ORG_NAME = "ballerina";
    public static final String AI_MODULE_NAME = "ai";
    public static final String CODE_ANNOTATION = "code";
    public static final String FILE_PATH = "filePath";
    public static final String CONTENT = "content";
    public record GeneratedCode(String code, JsonArray functions) { }

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

    static String retrieveLangLibs(String langLibsPath) throws IOException {
        try (InputStream inputStream = CommonUtils.class.getResourceAsStream(langLibsPath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Failed to retrieve langlibs");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            throw new IOException("Failed to retrieve langlibs");
        }
    }
}
