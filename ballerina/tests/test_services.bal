import ballerina/http;
import ballerina/test;

service /llm on new http:Listener(8080) {
    resource function post azureopenai/deployments/gpt4onew/chat/completions(
            string api\-version, AzureOpenAICreateChatCompletionRequest payload)
                returns json|error {
        test:assertEquals(api\-version, "2023-08-01-preview");
        string content = payload.messages[0].content;
        AzureOpenAIChatCompletionRequestMessage[]? messages = payload.messages;
        if messages is () {
            test:assertFail("Expected messages in the payload");
        }
        AzureOpenAIChatCompletionRequestMessage message = messages[0];
        test:assertEquals(message.role, "user");
        ChatCompletionTool[]? tools = payload?.tools;
        if tools is () {
            test:assertFail("No tools in the payload");
        }

        FunctionParameters? parameters = tools[0].'function.parameters;
        if parameters is () {
            test:assertFail("No tools in the payload");
        }
        test:assertEquals(parameters, getExpectedParameterSchema(content));

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
                        tool_calls: [{
                            id: "get_results",
                            'type: "function",
                            'function: {
                                name: "get_results",
                                arguments: getTheMockLLMResult(content)
                            }
                        }]
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
        ChatCompletionTool[]? tools = payload?.tools;
        if tools is () {
            test:assertFail("No tools in the payload");
        }

        FunctionParameters? parameters = tools[0].'function.parameters;
        if parameters is () {
            test:assertFail("No tools in the payload");
        }
        test:assertEquals(parameters, getExpectedParameterSchema(contentStr));

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
                        tool_calls: [{
                            id: "get_results",
                            'type: "function",
                            'function: {
                                name: "get_results",
                                arguments: getTheMockLLMResult(contentStr)
                            }
                        }]
                    }
                }
            ]
        };
    }
}
