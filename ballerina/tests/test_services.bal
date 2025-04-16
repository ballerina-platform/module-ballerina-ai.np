import ballerina/http;
import ballerina/test;

service /llm on new http:Listener(8080) {
    resource function post azureopenai/deployments/gpt4onew/chat/completions(
            string api\-version, AzureOpenAICreateChatCompletionRequest payload)
                returns json|error {
        test:assertEquals(api\-version, "2023-08-01-preview");
        AzureOpenAIChatCompletionRequestMessage[]? messages = payload.messages;
        if messages is () {
            test:assertFail("Expected messages in the payload");
        }
        AzureOpenAIChatCompletionRequestMessage message = messages[0];
        anydata content = message["content"];
        string contentStr = content.toString();
        test:assertEquals(message.role, "user");
        test:assertEquals(content, getExpectedPrompt(contentStr));
        return {
            'object: "chat.completion",
            created: 0,
            model: "",
            id: "",
            choices: [
                {
                    message: {
                        content: getTheMockLLMResult(contentStr)
                    }
                }
            ]
        };
    }

    resource function post openai/chat/completions(OpenAICreateChatCompletionRequest payload)
            returns json|error {

        OpenAIChatCompletionRequestUserMessage message = payload.messages[0];
        anydata content = message["content"];
        string contentStr = content.toString();
        test:assertEquals(message.role, "user");
        test:assertEquals(content, getExpectedPrompt(content.toString()));

        test:assertEquals(payload.model, "gpt4o");
        return {
            'object: "chat.completion",
            created: 0,
            model: "",
            id: "",
            choices: [
                {
                    finish_reason: "stop",
                    index: 0,
                    logprobs: (),
                    message: {
                        role: "assistant",
                        content: getTheMockLLMResult(contentStr),
                        refusal: ()
                    }
                }
            ]
        };
    }
}
