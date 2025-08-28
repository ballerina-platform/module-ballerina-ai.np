package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.lib.ai.np.compilerplugin.Commons.GeneratedCode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Map;

import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.CACHE_CONTROL;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.CLAUDE_MODEL_NAME;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.CONTENT;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MAX_TOKENS;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MESSAGES;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MODEL;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.ROLE;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.SYSTEM;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.TEMPERATURE;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.TEXT;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.TYPE;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.USER;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.updateSourceFilesWithGeneratedCode;

public class AnthropicModelProvider implements ModelProvider {
    private final String apiKey;
    private final String apiURL = "https://api.anthropic.com/v1/messages";
    private final String apiVersion = "2023-06-01";
    private final Map<String, String> headers;

    public AnthropicModelProvider(String apiKey) {
        this.apiKey = apiKey;
        this.headers = Map.of(
            "Content-Type", "application/json",
            "x-api-key", this.apiKey,
            "anthropic-version", this.apiVersion
        );
    }

    @Override
    public GeneratedCode generateFunction(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, apiURL, constructCodeGenerationPayload(
                useCase, sourceFiles, generateSystemMessagesForFunctions()).toString(), headers);
        String generatedText = getResponseTextFromBody(responseBody);
        return new GeneratedCode(generatedText, null);
    }

    @Override
    public GeneratedCode generateExpression(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, apiURL, constructCodeGenerationPayload(
                useCase, sourceFiles, generateSystemMessagesForExpressions()).toString(), headers);
        String generatedText = getResponseTextFromBody(responseBody);
        return new GeneratedCode(generatedText, null);
    }

    @Override
    public String repairFunctions(HttpClient client, String generatedFunctionName, JsonArray updatedSourceFiles,
                                  String repairPrompt, GeneratedCode generatedFunction, JsonArray diagnostics)
            throws IOException, InterruptedException {
        JsonObject payload = constructCodeReparationPayloadForFunctions(
                repairPrompt, generatedFunctionName, generatedFunction, updatedSourceFiles, diagnostics);
        return updateSourceFilesWithGeneratedCode(repairCode(client, payload), generatedFunction, updatedSourceFiles);
    }

    @Override
    public String repairExpressions(HttpClient client, JsonArray updatedSourceFiles,
                                    String repairPrompt, GeneratedCode generatedExpression,
                                    JsonArray diagnostics)
            throws IOException, InterruptedException {
        JsonObject payload = constructCodeReparationPayloadForNaturalExpressions(
                repairPrompt, generatedExpression, updatedSourceFiles, diagnostics);
        return updateSourceFilesWithGeneratedCode(repairCode(client, payload), generatedExpression, updatedSourceFiles);
    }

    @Override
    public String getName() {
        return "Anthropic";
    }

    private String repairCode(HttpClient client, JsonObject payload)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, apiURL, payload.toString(), headers);
        return getResponseTextFromBody(responseBody);
    }

    private JsonObject constructCodeGenerationPayload(String useCase, JsonArray sourceFiles, JsonArray systemPrompt) {
        JsonObject payload = new JsonObject();
        payload.addProperty(MODEL, CLAUDE_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);
        payload.add(SYSTEM, systemPrompt);

        JsonArray messagesArray = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty(ROLE, USER);

        JsonArray contentArray = new JsonArray();
        JsonObject content = new JsonObject();

        content.addProperty(TYPE, TEXT);
        content.addProperty(TEXT, PromptGenerator.getUserPrompt(useCase, sourceFiles));
        content.add(CACHE_CONTROL, getCacheControlOptions());

        contentArray.add(content);
        userMessage.add(CONTENT, contentArray);
        messagesArray.add(userMessage);
        payload.add(MESSAGES, messagesArray);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForFunctions(String generatedPrompt, String generatedFuncName,
                                  GeneratedCode generatedCode, JsonArray sourceFiles,
                                  JsonArray diagnostics) {
        JsonObject payload = new JsonObject();

        payload.addProperty(MODEL, CLAUDE_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);
        payload.add(SYSTEM, generateSystemMessagesForFunctions());
        String userRepairContent = PromptGenerator.getRepairPromptForFunctions(generatedFuncName, diagnostics);
        return ProviderUtils.populateReparationPayload(payload, generatedCode, generatedPrompt, sourceFiles,
                userRepairContent);
    }

    private JsonObject constructCodeReparationPayloadForNaturalExpressions(
            String generatedPrompt, GeneratedCode generatedCode, JsonArray sourceFiles,
            JsonArray diagnostics) {
        JsonObject payload = new JsonObject();

        payload.addProperty(MODEL, CLAUDE_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);
        payload.add(SYSTEM, generateSystemMessagesForExpressions());
        String userRepairContent = PromptGenerator.getRepairPromptForNaturalExpressions(diagnostics);
        return ProviderUtils.populateReparationPayload(payload, generatedCode, generatedPrompt, sourceFiles,
                userRepairContent);
    }

    private JsonArray generateSystemMessagesForFunctions() {
        JsonArray systemMessagesArray = new JsonArray();
        JsonObject systemPromptSuffix = new JsonObject();
        systemPromptSuffix.addProperty(TYPE, TEXT);
        systemPromptSuffix.addProperty(TEXT, PromptGenerator.getSystemPromptForFunction());
        systemPromptSuffix.add(CACHE_CONTROL, getCacheControlOptions());
        systemMessagesArray.add(systemPromptSuffix);

        return systemMessagesArray;
    }

    private JsonArray generateSystemMessagesForExpressions() {
        JsonArray systemMessagesArray = new JsonArray();
        JsonObject systemPromptSuffix = new JsonObject();
        systemPromptSuffix.addProperty(TYPE, TEXT);
        systemPromptSuffix.addProperty(TEXT, PromptGenerator.getSystemPromptForExpression());
        systemPromptSuffix.add(CACHE_CONTROL, getCacheControlOptions());
        systemMessagesArray.add(systemPromptSuffix);

        return systemMessagesArray;
    }

    private JsonObject getCacheControlOptions() {
        JsonObject cacheControl = new JsonObject();
        cacheControl.addProperty(TYPE, "ephemeral");
        return cacheControl;
    }

    private String getResponseTextFromBody(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray contentArray = responseJson.getAsJsonArray(CONTENT);

        if (contentArray == null || contentArray.isEmpty()) {
            throw new RuntimeException("No content found in LLM response");
        }

        JsonObject firstContent = contentArray.get(0).getAsJsonObject();
        return firstContent.get(TEXT).getAsString();
    }
}
