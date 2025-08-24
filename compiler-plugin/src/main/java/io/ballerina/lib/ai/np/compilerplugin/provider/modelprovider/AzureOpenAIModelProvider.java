package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Map;

public class AzureOpenAIModelProvider implements ModelProvider {
    private final String apiKey;
    private final String endPointUrl;

    public AzureOpenAIModelProvider(String apiKey, String deploymentId, String serviceUrl, String apiVersion) {
        this.apiKey = apiKey;
        this.endPointUrl = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                serviceUrl, deploymentId, apiVersion);
    }

    public AzureOpenAIModelProvider(String serviceUrl, String apiKey, String deploymentId) {
        this.apiKey = apiKey;
        this.endPointUrl = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                serviceUrl, deploymentId, "2023-05-15");
    }

    @Override
    public GeneratedCode generateCode(HttpClient client, String prompt, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String jsonPayload = "{\"messages\": [{\"role\": \"user\", \"content\": \""
                + prompt + "\"}]}";

        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "api-key", this.apiKey
        );

        String code = calLlm(client, endPointUrl, jsonPayload, headers);
        return new GeneratedCode(code, null);
    }

    @Override
    public String repairCodeForFunctions(HttpClient client, String generatedFuncName, JsonArray updatedSourceFiles,
                                         String generatedPrompt, GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException {
        return null;
    }

    @Override
    public String repairCodeForNaturalExpressions(HttpClient client, JsonArray updatedSourceFiles,
                                                  String generatedPrompt, GeneratedCode generatedCode,
                                                  JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException {
        return null;
    }
}
