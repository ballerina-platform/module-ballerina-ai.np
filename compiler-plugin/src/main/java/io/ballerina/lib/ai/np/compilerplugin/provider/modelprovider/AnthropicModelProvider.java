package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
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
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "x-api-key", this.apiKey,
                "anthropic-version", this.apiVersion
        );

        String responseBody = calLlm(client, apiURL, constructCodeGenerationPayload(
                prompt, sourceFiles).toString(), headers);
        String generatedText = getResponseTextFromBody(responseBody);
        return new GeneratedCode(generatedText, null);
    }

    @Override
    public String repairCodeForFunctions(HttpClient client, String generatedFuncName, JsonArray updatedSourceFiles,
                                         String generatedPrompt, GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException {
        JsonObject payload = constructCodeReparationPayloadForFunctions(
                generatedPrompt, generatedFuncName, generatedCode, updatedSourceFiles, diagnostics);
        return updateResourcesWithCodeSnippet(repairCode(client, payload), generatedCode, updatedSourceFiles);
    }

    @Override
    public String repairCodeForNaturalExpressions(HttpClient client, JsonArray updatedSourceFiles,
                                                  String generatedPrompt, GeneratedCode generatedCode,
                                                  JsonArray diagnostics)
            throws IOException, InterruptedException {
        JsonObject payload = constructCodeReparationPayloadForNaturalExpressions(
                generatedPrompt, generatedCode, updatedSourceFiles, diagnostics);
        return updateResourcesWithCodeSnippet(repairCode(client, payload), generatedCode, updatedSourceFiles);
    }

    private String repairCode(HttpClient client, JsonObject payload)
            throws IOException, InterruptedException {
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "x-api-key", this.apiKey,
                "anthropic-version", this.apiVersion
        );

        String responseBody = calLlm(client, apiURL, payload.toString(), headers);
        return getResponseTextFromBody(responseBody);
    }

    private JsonObject constructCodeGenerationPayload(String useCase, JsonArray sourceFiles) {
        // Create the main JSON object for the payload
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "claude-sonnet-4-20250514");
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);
        payload.add("system", generateSystemMessages());

        JsonArray messagesArray = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        JsonArray contentArray = new JsonArray();
        JsonObject content = new JsonObject();

        content.addProperty("type", "text");
        content.addProperty("text", getUserPrompt(useCase, sourceFiles));
        content.add("cache_control", getCacheControlOptions());

        contentArray.add(content);
        userMessage.add("content", contentArray);
        messagesArray.add(userMessage);
        payload.add("messages", messagesArray);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForFunctions(String generatedPrompt, String generatedFuncName,
                                  GeneratedCode generatedCode, JsonArray sourceFiles,
                                  JsonArray diagnostics) {
        JsonObject payload = new JsonObject();

        payload.addProperty("model", "claude-sonnet-4-20250514");
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);
        payload.add("system", generateSystemMessages());

        JsonArray messagesArray = generateMessageHistoryForRepairCall(generatedCode, generatedPrompt, sourceFiles);

        JsonObject userRepairMessage = new JsonObject();
        userRepairMessage.addProperty("role", "user");
        userRepairMessage.addProperty("content", getRepairPromptForFunctions(generatedFuncName, diagnostics));

        messagesArray.add(userRepairMessage);
        payload.add("messages", messagesArray);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForNaturalExpressions(
            String generatedPrompt, GeneratedCode generatedCode, JsonArray sourceFiles,
            JsonArray diagnostics) {
        JsonObject payload = new JsonObject();

        payload.addProperty("model", "claude-sonnet-4-20250514");
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);
        payload.add("system", generateSystemMessages());

        JsonArray messagesArray = generateMessageHistoryForRepairCall(generatedCode, generatedPrompt, sourceFiles);
        JsonObject userRepairMessage = new JsonObject();

        userRepairMessage.addProperty("role", "user");
        userRepairMessage.addProperty("content", getRepairPromptForNaturalExpressions(diagnostics));

        messagesArray.add(userRepairMessage);
        payload.add("messages", messagesArray);
        return payload;
    }

    private JsonArray generateMessageHistoryForRepairCall(GeneratedCode generatedCode, String generatedPrompt,
                                                          JsonArray sourceFiles) {
        JsonArray messagesArray = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", getUserPrompt(generatedPrompt, sourceFiles));
        messagesArray.add(userMessage);

        JsonObject assistantMessage = new JsonObject();
        assistantMessage.addProperty("role", "assistant");
        assistantMessage.addProperty("content", generatedCode.code());
        messagesArray.add(assistantMessage);

        return messagesArray;
    }

    private JsonArray generateSystemMessages() {
        JsonArray systemMessagesArray = new JsonArray();

        JsonObject systemPromptPrefix = new JsonObject();
        systemPromptPrefix.addProperty("type", "text");
        systemPromptPrefix.addProperty("text", getCodeGenerationSystemPromptPrefix());
        systemMessagesArray.add(systemPromptPrefix);

        JsonObject systemPromptSuffix = new JsonObject();
        systemPromptSuffix.addProperty("type", "text");
        systemPromptSuffix.addProperty("text", getCodeGenerationSystemPromptSuffix());
        systemPromptSuffix.add("cache_control", getCacheControlOptions());
        systemMessagesArray.add(systemPromptSuffix);

        return systemMessagesArray;
    }

    private JsonObject getCacheControlOptions() {
        JsonObject cacheControl = new JsonObject();
        cacheControl.addProperty("type", "ephemeral");
        return cacheControl;
    }

    private String getResponseTextFromBody(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray contentArray = responseJson.getAsJsonArray("content");

        if (contentArray == null || contentArray.isEmpty()) {
            throw new RuntimeException("No content found in LLM response");
        }

        JsonObject firstContent = contentArray.get(0).getAsJsonObject();
        return firstContent.get("text").getAsString();
    }
}
