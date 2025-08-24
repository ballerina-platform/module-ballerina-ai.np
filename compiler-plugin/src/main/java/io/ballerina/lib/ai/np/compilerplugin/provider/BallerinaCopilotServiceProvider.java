package io.ballerina.lib.ai.np.compilerplugin.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.lib.ai.np.compilerplugin.CommonUtils.GeneratedCode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

public class BallerinaCopilotServiceProvider implements Provider {
    private final String copilotUrl;
    private final String copilotAccessToken;

    public BallerinaCopilotServiceProvider(String copilotUrl, String copilotAccessToken) {
        this.copilotUrl = copilotUrl;
        this.copilotAccessToken = copilotAccessToken;
    }

    @Override
    public GeneratedCode generateCode(HttpClient client, String prompt, JsonArray sourceFiles)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest codeGenerationRequest = HttpRequest.newBuilder()
                .uri(new URI(copilotUrl + "/code"))
                .header("Authorization", "Bearer " + copilotAccessToken)
                .POST(HttpRequest.BodyPublishers
                        .ofString(constructCodeGenerationPayload(prompt, sourceFiles).toString())).build();
        Stream<String> lines = client.send(codeGenerationRequest, HttpResponse.BodyHandlers.ofLines()).body();
        return extractGeneratedFunctionCode(lines);
    }

    @Override
    public String repairCodeForFunctions(HttpClient client, String generatedFuncName, JsonArray updatedSourceFiles,
                                  String generatedPrompt, GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException {
        JsonObject payload = constructCodeReparationPayload(
                generatedPrompt, generatedFuncName, generatedCode, updatedSourceFiles, diagnostics);
        return updateResourcesWithCodeSnippet(repairCode(client, payload), generatedCode, updatedSourceFiles);
    }

    @Override
    public String repairCodeForNaturalExpressions(HttpClient client, JsonArray updatedSourceFiles,
                                          String generatedPrompt, GeneratedCode generatedCode, JsonArray diagnostics)
            throws IOException, InterruptedException, URISyntaxException {
        JsonObject payload = constructCodeReparationPayloadForConstNaturalExpressions(
                generatedPrompt, generatedCode, updatedSourceFiles, diagnostics);
        return updateResourcesWithCodeSnippet(repairCode(client, payload), generatedCode, updatedSourceFiles);
    }

    private String repairCode(HttpClient client, JsonObject payload)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest codeReparationRequest = HttpRequest.newBuilder()
                .uri(new URI(copilotUrl + "/code/repair"))
                .header("Authorization", "Bearer " + copilotAccessToken)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        String body = client.send(codeReparationRequest, HttpResponse.BodyHandlers.ofString()).body();
        return JsonParser.parseString(body).getAsJsonObject()
                .getAsJsonPrimitive("repairResponse").getAsString();
    }

    private JsonObject constructCodeReparationPayload(String generatedPrompt, String generatedFuncName,
                                                             GeneratedCode generatedCode, JsonArray sourceFiles,
                                                             JsonArray diagnostics) {
        JsonObject payload = new JsonObject();

        payload.addProperty(
                "usecase", String.format("Fix issues in the generated '%s' function. " +
                        "Do not change anything other than the function body", generatedFuncName));
        return updateResourcePayload(payload, generatedPrompt, generatedCode, diagnostics, sourceFiles);
    }

    private JsonObject updateResourcePayload(JsonObject payload, String generatedPrompt,
                                            GeneratedCode generatedCode, JsonArray diagnostics, JsonArray sourceFiles) {
        payload.add("sourceFiles", sourceFiles);

        JsonObject chatHistoryMember = new JsonObject();
        chatHistoryMember.addProperty("actor", "user");
        chatHistoryMember.addProperty("message", generatedPrompt);
        JsonArray chatHistory = new JsonArray();
        chatHistory.add(chatHistoryMember);
        payload.add("chatHistory", chatHistory);
        payload.add("functions", generatedCode.functions());

        JsonObject diagnosticRequest = new JsonObject();
        diagnosticRequest.add("diagnostics", diagnostics);
        diagnosticRequest.addProperty("response", generatedCode.code());
        payload.add("diagnosticRequest", diagnosticRequest);

        return payload;
    }

    private JsonObject constructCodeGenerationPayload(String prompt, JsonArray sourceFiles) {
        JsonObject payload = new JsonObject();
        payload.addProperty("usecase", prompt);
        payload.add("sourceFiles", sourceFiles);
        return payload;
    }

    private JsonObject constructCodeReparationPayloadForConstNaturalExpressions(
            String generatedPrompt, GeneratedCode generatedCode, JsonArray sourceFiles,
            JsonArray diagnostics) {
        JsonObject payload = new JsonObject();
        payload.addProperty("usecase",
                "The generated expression results in the following errors. " +
                        "Fix the errors and return a new constant expression.");

        return updateResourcePayload(payload, generatedPrompt, generatedCode, diagnostics, sourceFiles);
    }

    private GeneratedCode extractGeneratedFunctionCode(Stream<String> lines) {
        String[] linesArr = lines.toArray(String[]::new);
        int length = linesArr.length;

        if (length == 1) {
            JsonObject jsonObject = JsonParser.parseString(linesArr[0]).getAsJsonObject();
            if (jsonObject.has("error_message")) {
                throw new RuntimeException(jsonObject.get("error_message").getAsString());
            }
        }

        StringBuilder responseBody = new StringBuilder();
        JsonArray functions = null;

        int index = 0;
        while (index < length) {
            String line = linesArr[index];

            if (line.isBlank()) {
                index++;
                continue;
            }

            if ("event: content_block_delta".equals(line)) {
                line = linesArr[++index].substring(6);
                responseBody.append(JsonParser.parseString(line).getAsJsonObject()
                        .getAsJsonPrimitive("text").getAsString());
                continue;
            }

            if ("event: functions".equals(line)) {
                line = linesArr[++index].substring(6);
                functions = JsonParser.parseString(line).getAsJsonArray();
                continue;
            }

            index++;
        }

        String responseBodyString = responseBody.toString();
        return new GeneratedCode(extractBallerinaCodeSnippet(responseBodyString), functions);
    }
}
