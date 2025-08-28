package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.lib.ai.np.compilerplugin.Commons;
import io.ballerina.lib.ai.np.compilerplugin.Commons.GeneratedCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

class ProviderUtils {
    static final String TRIPLE_BACKTICK_BALLERINA = "```ballerina";
    static final String TRIPLE_BACKTICK = "```";
    static final String ROLE = "role";
    static final String SYSTEM = "system";
    static final String USER = "user";
    static final String ASSISTANT = "assistant";
    static final String MESSAGES = "messages";
    static final String CONTENT = "content";
    static final String FILE_PATH = "filePath";
    static final String MESSAGE = "message";
    static final String CHOICES = "choices";
    static final String CLAUDE_MODEL_NAME = "claude-sonnet-4-20250514";
    static final String OPENAI_MODEL_NAME = "claude-sonnet-4-20250514";
    static final String MODEL = "model";
    static final String MAX_TOKENS = "max_tokens";
    static final String TEMPERATURE = "temperature";
    static final String TYPE = "type";
    static final String TEXT = "text";
    static final String CACHE_CONTROL = "cache_control";

    public static String extractBallerinaCodeSnippet(String responseBodyString) {
        return responseBodyString.substring(responseBodyString.indexOf(TRIPLE_BACKTICK_BALLERINA) + 12,
                responseBodyString.lastIndexOf(TRIPLE_BACKTICK));
    }

    public static boolean hasBallerinaCodeSnippet(String responseBodyString) {
        return responseBodyString.contains(TRIPLE_BACKTICK_BALLERINA) && responseBodyString.contains(TRIPLE_BACKTICK);
    }

    public static String updateSourceFilesWithGeneratedCode(String repairResponse, GeneratedCode generatedCode,
                                                            JsonArray sourceFiles) {
        if (hasBallerinaCodeSnippet(repairResponse)) {
            String generatedFunctionSrc = extractBallerinaCodeSnippet(repairResponse);
            sourceFiles.get(sourceFiles.size() - 1).getAsJsonObject().addProperty(CONTENT, generatedFunctionSrc);
            return generatedFunctionSrc;
        }
        return generatedCode.code();
    }

    static String retrieveLangLibs(String langLibsPath) throws IOException {
        String errorMessage = "Failed to retrieve langlibs";
        try (InputStream inputStream = Commons.class.getResourceAsStream(langLibsPath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException(errorMessage);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            throw new IOException(errorMessage);
        }
    }

    public static String getOpenAIResponseTextFromBody(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choicesArray = responseJson.getAsJsonArray(CHOICES);

        if (choicesArray == null || choicesArray.isEmpty()) {
            throw new RuntimeException("No choices found in LLM response");
        }

        JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject(MESSAGE);
        return message.get(CONTENT).getAsString();
    }

    public static JsonObject getOpenAISystemMessageForFunction() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty(ROLE, SYSTEM);
        String systemContent = PromptGenerator.getSystemPromptForFunction();
        systemMessage.addProperty(CONTENT, systemContent);
        return systemMessage;
    }

    public static JsonObject getOpenAISystemMessageForExpression() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty(ROLE, SYSTEM);
        String systemContent = PromptGenerator.getSystemPromptForExpression();
        systemMessage.addProperty(CONTENT, systemContent);
        return systemMessage;
    }

    public static JsonArray generateMessageHistoryForRepairCall(GeneratedCode generatedCode, String generatedPrompt,
                                                          JsonArray sourceFiles) {
        JsonArray messagesArray = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty(ROLE, USER);
        userMessage.addProperty(CONTENT, PromptGenerator.getUserPrompt(generatedPrompt, sourceFiles));
        messagesArray.add(userMessage);

        JsonObject assistantMessage = new JsonObject();
        assistantMessage.addProperty(ROLE, ASSISTANT);
        assistantMessage.addProperty(CONTENT, generatedCode.code());
        messagesArray.add(assistantMessage);

        return messagesArray;
    }

    public static JsonObject populateReparationPayload(JsonObject payload, GeneratedCode generatedCode,
                                                       String generatedPrompt, JsonArray sourceFiles,
                                                       String userRepairContent) {
        JsonArray messagesArray = generateMessageHistoryForRepairCall(
                generatedCode, generatedPrompt, sourceFiles);
        JsonObject userRepairMessage = new JsonObject();
        userRepairMessage.addProperty(ROLE, USER);
        userRepairMessage.addProperty(CONTENT, userRepairContent);

        messagesArray.add(userRepairMessage);
        payload.add(MESSAGES, messagesArray);
        return payload;
    }
}
