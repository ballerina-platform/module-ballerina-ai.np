package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Map;

public class AnthropicModelProvider implements ModelProvider {
    private final String apiKey;
    private final String apiURL = "https://api.anthropic.com/v1/messages";
    private final String apiVersion = "2023-06-01";

    public AnthropicModelProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public GeneratedCode generateCode(HttpClient client, String prompt, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String jsonPayload = "{\"model\": \"claude-3-opus-20240229\"," +
                "\"max_tokens\": 1024," +
                "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";

        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "x-api-key", this.apiKey,
                "anthropic-version", this.apiVersion
        );

        String code = calLlm(client, apiURL, jsonPayload, headers);
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
