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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.lib.ai.np.compilerplugin.provider.Provider;
import io.ballerina.lib.ai.np.compilerplugin.provider.ProviderFactory;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.CONTENT;
import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.FILE_PATH;
import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

/**
 * Methods to generate code at compile-time.
 *
 * @since 0.4.0
 */
public class CodeGenerationUtils {

    public static final String TEMP_DIR_PREFIX = "ballerina-np-codegen-diagnostics-dir-";
    public static final String BALLERINA_TOML_FILE = "Ballerina.toml";

    static String generateCodeForFunction(String originalFuncName,
                                          String generatedFuncName, String prompt, HttpClient client,
                                          JsonArray sourceFiles, ModuleDescriptor moduleDescriptor,
                                          String packageOrgName) {
        try {
            String generatedPrompt = PromptGenerator
                    .generateUseCasePromptForFunctions(originalFuncName, generatedFuncName, prompt);
            GeneratedCode generatedCode = generateCode(client, sourceFiles,
                    generatedPrompt);

            updateSourceFilesWithGeneratedContent(sourceFiles, generatedFuncName, generatedCode);
            return repairCode(generatedFuncName, client, sourceFiles, moduleDescriptor,
                    generatedPrompt, generatedCode, packageOrgName);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to generate code, invalid URI for Copilot");
        } catch (ConnectException e) {
            throw new RuntimeException("Failed to connect to Copilot services");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate code: " + e.getMessage());
        }
    }

    static ExpressionNode generateCodeForNaturalExpression(NaturalExpressionNode naturalExpressionNode,
                                                           HttpClient client, JsonArray sourceFiles,
                                                           SemanticModel semanticModel,
                                                           TypeSymbol expectedType, Document document) {
        try {
            String generatedPrompt = PromptGenerator.generateUseCasePromptForNaturalFunctions(
                    naturalExpressionNode, expectedType, semanticModel);
            GeneratedCode generatedCode = generateCode(client, sourceFiles,
                    generatedPrompt);
            ExpressionNode modifiedExpressionNode = NodeParser.parseExpression(generatedCode.code());
            JsonArray diagnostics =
                    collectConstNaturalExpressionDiagnostics(modifiedExpressionNode, generatedCode, document);
            if (diagnostics.isEmpty()) {
                return modifiedExpressionNode;
            }
            String repairedExpression = repairIfDiagnosticsExistForConstNaturalExpression(client, sourceFiles,
                    generatedPrompt, generatedCode, diagnostics);
            return NodeParser.parseExpression(repairedExpression);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to generate code, invalid URI for Copilot");
        } catch (ConnectException e) {
            throw new RuntimeException("Failed to connect to Copilot services");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate code: " + e.getMessage());
        }
    }

    private static GeneratedCode generateCode(HttpClient client, JsonArray sourceFiles, String generatedPrompt)
            throws URISyntaxException, IOException, InterruptedException {
        Provider provider = ProviderFactory.getProviderInstance();
        try {
            return provider.generateCode(client, generatedPrompt, sourceFiles);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate code using the model: " + e.getMessage());
        }
    }

    private static String repairCode(String generatedFuncName,
                                     HttpClient client, JsonArray sourceFiles, ModuleDescriptor moduleDescriptor,
                                     String generatedPrompt, GeneratedCode generatedCode, String packageOrgName)
            throws IOException, URISyntaxException, InterruptedException {
        String generatedFunctionSrc = repairIfDiagnosticsExist(client, sourceFiles,
                moduleDescriptor, generatedFuncName, generatedPrompt, generatedCode, packageOrgName);
        return repairIfDiagnosticsExist(client, sourceFiles, moduleDescriptor,
                generatedFuncName, generatedPrompt,
                new GeneratedCode(generatedFunctionSrc, generatedCode.functions()), packageOrgName);
    }

    private static Optional<Document> findDocumentByName(Module module, String generateFuncName) {
        String documentName = getGeneratedBalFileName(generateFuncName);
        for (DocumentId docId : module.documentIds()) {
            Document doc = module.document(docId);
            if (doc.name().contains(documentName)) {
                return Optional.of(doc);
            }
        }
        return Optional.empty();
    }

    private static String repairIfDiagnosticsExist(HttpClient client,
                                                   JsonArray sourceFiles, ModuleDescriptor moduleDescriptor,
                                                   String generatedFuncName, String generatedPrompt,
                                                   GeneratedCode generatedCode, String packageOrgName)
            throws IOException, URISyntaxException, InterruptedException {
        ModulePartNode modulePartNode = NodeParser.parseModulePart(generatedCode.code());

        BuildProject project = createProject(sourceFiles, moduleDescriptor);
        Optional<JsonArray> compilerDiagnostics = getDiagnostics(project);
        Module module = project.currentPackage().module(
                project.currentPackage().modules().iterator().next().moduleId());
        JsonArray codeGeneratorDiagnostics = new AllowedConstructValidator(
                project.currentPackage().getCompilation().getSemanticModel(module.moduleId()),
                findDocumentByName(module, generatedFuncName), packageOrgName
        ).checkCodeGenerationDiagnostics(modulePartNode);
        JsonArray allDiagnostics = mergeDiagnostics(compilerDiagnostics,
                codeGeneratorDiagnostics);

        if (allDiagnostics.isEmpty()) {
            return generatedCode.code();
        }

        return repairCode(generatedFuncName, client, sourceFiles,
                generatedPrompt, generatedCode, allDiagnostics);
    }

    private static JsonArray collectConstNaturalExpressionDiagnostics(ExpressionNode expNode,
                                                                      GeneratedCode generatedCode, Document document) {
        JsonArray diagnostics = new JsonArray();
        Iterable<Diagnostic> compilerDiagnostics = expNode.diagnostics();

        compilerDiagnostics.forEach(diagnostic -> {
            JsonObject diagnosticObj = new JsonObject();
            diagnosticObj.addProperty("message", constructProjectDiagnosticMessage(expNode,
                    diagnostic.message(), document));
            diagnostics.add(diagnosticObj);
        });

        JsonArray constantExpressionDiagnostics = new ConstantExpressionValidator(document)
                .checkNonConstExpressions(NodeParser.parseExpression(generatedCode.code()));
        diagnostics.addAll(constantExpressionDiagnostics);
        return diagnostics;
    }

    private static String constructProjectDiagnosticMessage(Node node, String message, Document document) {
        LineRange lineRange = node.location().lineRange();
        LinePosition startLine = lineRange.startLine();
        LinePosition endLine = lineRange.endLine();
        return String.format("ERROR [%s:(%s:%s,%s:%s)] %s.",
                document.name(), startLine.line(), startLine.offset(), endLine.line(), endLine.offset(), message);
    }

    private static String repairIfDiagnosticsExistForConstNaturalExpression(HttpClient client, JsonArray sourceFiles,
                                                                            String generatedPrompt,
                                                                            GeneratedCode generatedCode,
                                                                            JsonArray diagnostics)
            throws IOException, URISyntaxException, InterruptedException {
        return repairCodeForConstNaturalExpressions(client, sourceFiles,
                generatedPrompt, generatedCode, diagnostics);
    }

    private static JsonArray mergeDiagnostics(Optional<JsonArray> compilerDiagnostics,
                                              JsonArray validationDiagnostics) {
        JsonArray merged = new JsonArray();
        compilerDiagnostics.ifPresent(merged::addAll);
        merged.addAll(validationDiagnostics);
        return merged;
    }

    private static String repairCode(String generatedFuncName,
                                     HttpClient client, JsonArray updatedSourceFiles, String generatedPrompt,
                                     GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, URISyntaxException, InterruptedException {
        return ProviderFactory.getProviderInstance().repairCodeForFunctions(
                client, generatedFuncName, updatedSourceFiles, generatedPrompt, generatedCode, diagnostics);
    }

    private static String repairCodeForConstNaturalExpressions(HttpClient client, JsonArray updatedSourceFiles,
                                                               String generatedPrompt, GeneratedCode generatedCode,
                                                               JsonArray diagnostics)
            throws URISyntaxException, IOException, InterruptedException {
        return ProviderFactory.getProviderInstance().repairCodeForNaturalExpressions(client,
                updatedSourceFiles, generatedPrompt, generatedCode, diagnostics);
    }

    private static Optional<JsonArray> getDiagnostics(BuildProject project) {
        JsonObject diagnosticObj;
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();

        if (diagnosticResult.errorCount() == 0) {
            return Optional.empty();
        }

        JsonArray diagnostics = new JsonArray();
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
            if (diagnosticInfo.severity() != DiagnosticSeverity.ERROR) {
                continue;
            }

            diagnosticObj = new JsonObject();
            diagnosticObj.addProperty("message", diagnostic.toString());
            diagnostics.add(diagnosticObj);
        }

        return Optional.of(diagnostics);
    }

    private static BuildProject createProject(JsonArray sourceFiles, ModuleDescriptor moduleDescriptor)
            throws IOException {
        Path tempProjectDir = Files.createTempDirectory(TEMP_DIR_PREFIX + System.currentTimeMillis());
        tempProjectDir.toFile().deleteOnExit();

        Path tempGeneratedDir = Files.createDirectory(tempProjectDir.resolve("generated"));
        tempGeneratedDir.toFile().deleteOnExit();

        for (JsonElement sourceFile : sourceFiles) {
            JsonObject sourceFileObj = sourceFile.getAsJsonObject();
            File file = Files.createFile(
                    tempProjectDir.resolve(Path.of(sourceFileObj.get(FILE_PATH).getAsString()))).toFile();
            file.deleteOnExit();

            try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
                fileWriter.write(sourceFileObj.get(CONTENT).getAsString());
            }
        }

        Path ballerinaTomlPath = tempProjectDir.resolve(BALLERINA_TOML_FILE);
        File balTomlFile = Files.createFile(ballerinaTomlPath).toFile();
        balTomlFile.deleteOnExit();

        try (FileWriter fileWriter = new FileWriter(balTomlFile, StandardCharsets.UTF_8)) {
            fileWriter.write(String.format("""
                            [package]
                            org = "%s"
                            name = "%s"
                            version = "%s"
                            """,
                    moduleDescriptor.org().value(),
                    moduleDescriptor.packageName().value(),
                    moduleDescriptor.version().value()));
        }

        BuildOptions buildOptions = BuildOptions.builder()
                .setExperimental(true)
                .targetDir(ProjectUtils.getTemporaryTargetPath())
                .build();
        return BuildProject.load(tempProjectDir, buildOptions);
    }

    private static void updateSourceFilesWithGeneratedContent(JsonArray sourceFiles, String generatedFuncName,
                                                              GeneratedCode generatedCode) {
        JsonObject sourceFile = new JsonObject();
        sourceFile.addProperty(FILE_PATH, String.format("generated/functions_%s.bal", generatedFuncName));
        sourceFile.addProperty(CONTENT, generatedCode.code());
        sourceFiles.add(sourceFile);
    }

    private static String getGeneratedBalFileName(String generatedFuncName) {
        return String.format("functions_%s.bal", generatedFuncName);
    }
}
