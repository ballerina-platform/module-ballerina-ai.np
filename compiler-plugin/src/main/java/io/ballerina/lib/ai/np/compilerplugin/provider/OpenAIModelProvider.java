package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.lib.ai.np.compilerplugin.Commons.GeneratedCode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Map;

import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.CONTENT;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MAX_TOKENS;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MESSAGES;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.MODEL;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.OPENAI_MODEL_NAME;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.ROLE;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.TEMPERATURE;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.USER;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.updateSourceFilesWithGeneratedCode;

public class OpenAIModelProvider implements ModelProvider {
    private final String serviceUrl = "https://api.openai.com/v1/chat/completions";
    private final Map<String, String> headers;

    public OpenAIModelProvider(String apiKey) {
         this.headers = Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer " + apiKey
        );
    }

    @Override
    public GeneratedCode generateFunction(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, serviceUrl, constructCodeGenerationPayload(
                useCase, sourceFiles, ProviderUtils.getOpenAISystemMessageForFunction()).toString(), headers);
        String generatedText = ProviderUtils.getOpenAIResponseTextFromBody(responseBody);
        return new GeneratedCode(generatedText, null);
    }

    @Override
    public GeneratedCode generateExpression(HttpClient client, String useCase, JsonArray sourceFiles)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, serviceUrl, constructCodeGenerationPayload(
                useCase, sourceFiles, ProviderUtils.getOpenAISystemMessageForExpression()).toString(), headers);
        String generatedText = ProviderUtils.getOpenAIResponseTextFromBody(responseBody);
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
        return "OpenAI";
    }

    private String repairCode(HttpClient client, JsonObject payload)
            throws IOException, InterruptedException {
        String responseBody = sendRequest(client, serviceUrl, payload.toString(), headers);
        return ProviderUtils.getOpenAIResponseTextFromBody(responseBody);
    }

    private JsonObject constructCodeGenerationPayload(String useCase, JsonArray sourceFiles, JsonObject systemMessage) {
        JsonObject payload = new JsonObject();
        payload.addProperty(MODEL, OPENAI_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty(ROLE, USER);
        userMessage.addProperty(CONTENT, PromptGenerator.getUserPrompt(useCase, sourceFiles));
        messagesArray.add(userMessage);

        payload.add(MESSAGES, messagesArray);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForFunctions(String generatedPrompt, String generatedFuncName,
                                                                  GeneratedCode generatedCode, JsonArray sourceFiles,
                                                                  JsonArray diagnostics) {
        JsonObject payload = new JsonObject();
        payload.addProperty(MODEL, OPENAI_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);
        String userRepairContent = PromptGenerator.getRepairPromptForFunctions(generatedFuncName, diagnostics);
        return ProviderUtils.populateReparationPayload(payload, generatedCode, generatedPrompt, sourceFiles,
                userRepairContent);
    }

    private JsonObject constructCodeReparationPayloadForNaturalExpressions(
            String generatedPrompt, GeneratedCode generatedCode, JsonArray sourceFiles,
            JsonArray diagnostics) {
        JsonObject payload = new JsonObject();
        payload.addProperty(MODEL, OPENAI_MODEL_NAME);
        payload.addProperty(MAX_TOKENS, 4096 * 4);
        payload.addProperty(TEMPERATURE, 0);
        String userRepairContent = PromptGenerator.getRepairPromptForNaturalExpressions(diagnostics);
        return ProviderUtils.populateReparationPayload(payload, generatedCode, generatedPrompt, sourceFiles,
                userRepairContent);
    }
}
