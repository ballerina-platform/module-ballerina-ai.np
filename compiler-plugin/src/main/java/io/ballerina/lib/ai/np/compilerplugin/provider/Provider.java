package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.CONTENT;

public interface Provider {
    String TRIPLE_BACKTICK_BALLERINA = "```ballerina";
    String TRIPLE_BACKTICK = "```";

    GeneratedCode generateCode(HttpClient client, String prompt, JsonArray sourceFiles)
            throws IOException, InterruptedException, URISyntaxException;

    String repairCodeForFunctions(HttpClient client, String generatedFuncName, JsonArray updatedSourceFiles,
                                  String generatedPrompt, GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException;

    String repairCodeForNaturalExpressions(HttpClient client, JsonArray updatedSourceFiles, String generatedPrompt,
                                           GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException;

    default String extractBallerinaCodeSnippet(String responseBodyString) {
        return responseBodyString.substring(responseBodyString.indexOf(TRIPLE_BACKTICK_BALLERINA) + 12,
                responseBodyString.lastIndexOf(TRIPLE_BACKTICK));
    }

    default boolean hasBallerinaCodeSnippet(String responseBodyString) {
        return responseBodyString.contains(TRIPLE_BACKTICK_BALLERINA) && responseBodyString.contains(TRIPLE_BACKTICK);
    }

    default String updateResourcesWithCodeSnippet(String repairResponse, GeneratedCode generatedCode,
                                                  JsonArray sourceFiles) {
        if (hasBallerinaCodeSnippet(repairResponse)) {
            String generatedFunctionSrc = extractBallerinaCodeSnippet(repairResponse);
            sourceFiles.get(sourceFiles.size() - 1).getAsJsonObject().addProperty(CONTENT, generatedFunctionSrc);
            return generatedFunctionSrc;
        }
        return generatedCode.code();
    }
}
