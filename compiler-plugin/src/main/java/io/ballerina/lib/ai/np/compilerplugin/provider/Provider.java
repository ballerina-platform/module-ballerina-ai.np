package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.Commons.GeneratedCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

public interface Provider {
    GeneratedCode generateFunction(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException, URISyntaxException;

    GeneratedCode generateExpression(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException, URISyntaxException;

    String repairFunctions(HttpClient client, String generatedFunctionName, JsonArray updatedSourceFiles,
                           String repairPrompt, GeneratedCode generatedFunction, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException;

    String repairExpressions(HttpClient client, JsonArray updatedSourceFiles, String repairPrompt,
                             GeneratedCode generatedExpression, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException;

    String getName();
}
