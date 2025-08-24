package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Map;

public class OpenAIModelProvider implements ModelProvider {
    private final String apiKey;
    private final String serviceUrl = "https://api.openai.com/v1/chat/completions";
    private final String model = "gpt-4";

    public OpenAIModelProvider(String apiKey) {
        this.apiKey = apiKey;
    }
    @Override
    public GeneratedCode generateCode(HttpClient client, String prompt, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String jsonPayload = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \""
                + prompt + "\"}]}";

        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + this.apiKey
        );

        String code = calLlm(client, serviceUrl, jsonPayload, headers);
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
