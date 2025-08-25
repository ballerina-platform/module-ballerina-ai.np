package io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
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
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + this.apiKey
        );

        String responseBody = calLlm(client, serviceUrl, constructCodeGenerationPayload(
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
                "Authorization", "Bearer " + this.apiKey
        );

        String responseBody = calLlm(client, serviceUrl, payload.toString(), headers);
        return getResponseTextFromBody(responseBody);
    }

    private JsonObject constructCodeGenerationPayload(String useCase, JsonArray sourceFiles) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", this.model);
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(getSystemMessage());

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", getUserPrompt(useCase, sourceFiles));
        messagesArray.add(userMessage);

        payload.add("messages", messagesArray);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForFunctions(String generatedPrompt, String generatedFuncName,
                                                                  GeneratedCode generatedCode, JsonArray sourceFiles,
                                                                  JsonArray diagnostics) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", this.model);
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);

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
        payload.addProperty("model", this.model);
        payload.addProperty("max_tokens", 4096 * 4);
        payload.addProperty("temperature", 0);

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
        messagesArray.add(getSystemMessage());

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

    private JsonObject getSystemMessage() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        String systemContent = getCodeGenerationSystemPromptPrefix() + "\n" + getCodeGenerationSystemPromptSuffix();
        systemMessage.addProperty("content", systemContent);
        return systemMessage;
    }

    private String getResponseTextFromBody(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choicesArray = responseJson.getAsJsonArray("choices");

        if (choicesArray == null || choicesArray.isEmpty()) {
            throw new RuntimeException("No choices found in LLM response");
        }

        JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        return message.get("content").getAsString();
    }
}
