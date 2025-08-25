package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import io.ballerina.lib.ai.np.compilerplugin.PromptGenerator;
import io.ballerina.lib.ai.np.compilerplugin.provider.Provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public interface ModelProvider extends Provider {
    default String getCodeGenerationSystemPromptPrefix() {
        return PromptGenerator.getSystemPromptPrefix();
    }

    default String getCodeGenerationSystemPromptSuffix() {
        return PromptGenerator.getSystemPromptSuffix();
    }

    default String getUserPrompt(String useCase, JsonArray sourceFiles) {
        return PromptGenerator.getUserPrompt(useCase, sourceFiles);
    }

    default String getRepairPromptForFunctions(String generatedFuncName, JsonArray diagnostics) {
        return PromptGenerator.getRepairPromptForFunctions(generatedFuncName, diagnostics);
    }

    default String getRepairPromptForNaturalExpressions(JsonArray diagnostics) {
        return PromptGenerator.getRepairPromptForNaturalExpressions(diagnostics);
    }

    default String calLlm(HttpClient httpClient, String url,
                         String jsonPayload, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed with status code " + response.statusCode() +
                    " and body: " + response.body());
        }

        return response.body();
    }
}
