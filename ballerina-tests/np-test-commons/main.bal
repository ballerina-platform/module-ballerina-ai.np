// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/test;

service /llm on new http:Listener(8080) {
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

        test:assertEquals(payload.model, "gpt-4o-mini");
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
                                arguments: getMockLLMResponse(contentStr)
                            }
                        }]
                    }
                }
            ]
        };
    }

    // resource function post 'default/chat/complete(@http:Payload string contentStr)
    //         returns json|error {
    //     test:assertEquals(contentStr, getExpectedPrompt(contentStr.toString()));
    //     return {
    //         content: [getMockLLMResponse(contentStr)]
    //     };
    // }
}

isolated function getExpectedParameterSchema(string prompt) returns map<json> {
    string trimmedPrompt = prompt.trim();

    if trimmedPrompt.startsWith("Which country") {
        return  {"type": "object","properties":{"result":{"type":"string"}}};
    }

    if trimmedPrompt.startsWith("For each string value ") {
        return {"type": "object", "properties":{"result":{"type":"array", "items":{"type":"object", "anyOf":[{"type":"string"}, {"type":"integer"}]}}}};
    }

    if trimmedPrompt.startsWith("Who is a popular sportsperson") {
        return {"type":"object", "anyOf":[{"required":["firstName", "lastName", "sport", "yearOfBirth"], "type":"object", "properties":{"firstName":{"type":"string", "description":"First name of the person"}, "lastName":{"type":"string", "description":"Last name of the person"}, "yearOfBirth":{"type":"integer", "description":"Year the person was born", "format":"int64"}, "sport":{"type":"string", "description":"Sport that the person plays"}}}, {"type":null}]};
    }

    if trimmedPrompt.includes("Tell me about places in the specified country") && trimmedPrompt.includes("Sri Lanka") {
        return {"type": "object", "properties":{"result":{"type":"array", "items":{"required":["city", "country", "description", "name"], "type":"object", "properties":{"name":{"type":"string", "description":"Name of the place."}, "city":{"type":"string", "description":"City in which the place is located."}, "country":{"type":"string", "description":"Country in which the place is located."}, "description":{"type":"string", "description":"One-liner description of the place."}}}}}};
    }

    if trimmedPrompt.includes("Tell me about places in the specified country") && trimmedPrompt.includes("UAE") {
        return {"type": "object", "properties":{"result":{"type":"array", "items":{"required":["city", "country", "description", "name"], "type":"object", "properties":{"name":{"type":"string", "description":"Name of the place."}, "city":{"type":"string", "description":"City in which the place is located."}, "country":{"type":"string", "description":"Country in which the place is located."}, "description":{"type":"string", "description":"One-liner description of the place."}}}}}};
    }

    if trimmedPrompt.startsWith("What's the output of the Ballerina code below") {
        return {"type": "object", "properties":{"result":{"type":"integer"}}};
    }

    if trimmedPrompt.includes("What's the sum of these") {
        if trimmedPrompt.includes("[]") {
            return {"type": "object", "properties":{"result":{"type":"integer"}}};
        }

        if trimmedPrompt.includes("[40,50]") {
            return {"type": "object", "properties":{"result":{"type":"integer"}}};
        }
    }

    if trimmedPrompt.includes("Give me the sum of these values") {
        if trimmedPrompt.includes("[]") {
            return {"type": "object", "properties":{"result":{"type":"integer"}}};
        }

        if trimmedPrompt.includes("[500]") {
            return {"type": "object", "properties":{"result":{"type":"integer"}}};
        }
    }

    test:assertFail("Unexpected prompt: " + trimmedPrompt);
}

isolated function getMockLLMResponse(string message) returns string? {
    if message.startsWith("Which country") {
        return {result: "Sri Lanka"}.toJsonString();
    }

    if message.startsWith("For each string value ") {
        return {result: ["foo", 1, "bar", "2.3", 4]}.toJsonString();
    }

    if message.startsWith("Who is a popular sportsperson") {
        return {result: {"firstName":"Simone","lastName":"Biles","yearOfBirth":1997,"sport":"Gymnastics"}}.toJsonString();
    }

    if message.includes("Tell me about places in the specified country") && message.includes("Sri Lanka") {
        return {result: [{"name":"Unawatuna Beach","city":"Galle","country":"Sri Lanka","description":"A popular beach known for its golden sands and vibrant nightlife."},{"name":"Mirissa Beach","city":"Mirissa","country":"Sri Lanka","description":"Famous for its stunning sunsets and opportunities for whale watching."},{"name":"Hikkaduwa Beach","city":"Hikkaduwa","country":"Sri Lanka","description":"A great destination for snorkeling and surfing, lined with lively restaurants."}]}.toJsonString();
    }

    if message.includes("Tell me about places in the specified country") && message.includes("UAE") {
        return {result: [{"name":"Burj Khalifa","city":"Dubai","country":"UAE","description":"The tallest building in the world, offering panoramic views of the city."},{"name":"Ain Dubai","city":"Dubai","country":"UAE","description":"The world's tallest observation wheel, providing breathtaking views of the Dubai skyline."}]}.toJsonString();
    }

    if message.startsWith("What's the output of the Ballerina code below?") {
        return {result: 30}.toJsonString();
    }

    if message.includes("What's the sum of these") {
        if message.includes("[]") {
            return {result: 3}.toJsonString();
        }

        if message.includes("[40,50]") {
            return {result: 140}.toJsonString();
        }
    }

    if message.includes("Give me the sum of these values") {
        if message.includes("[]") {
            return {result: 300}.toJsonString();
        }

        if message.includes("[500]") {
            return {result: 1200}.toJsonString();
        }
    }

    test:assertFail("Unexpected prompt");
}
