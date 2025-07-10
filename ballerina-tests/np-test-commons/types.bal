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

import ballerina/data.jsondata;

public type ChatCompletionFunctionParameters record {
};

public type ChatCompletionRequestMessage record {
    ChatCompletionRequestMessageRole role;
};

public type ChatCompletionRequestMessageRole "system"|"user"|"assistant"|"tool"|"function";

public type ChatCompletionToolChoiceOption "none"|"auto"|ChatCompletionNamedToolChoice;

public type ChatCompletionNamedToolChoice record {
    ChatCompletionNamedToolChoice_function 'function?;
    "function" 'type?;
};

public type ChatCompletionNamedToolChoice_function record {
    string name;
};

public type ChatCompletionTool_function record {
    string description?;
    string name;
    ChatCompletionFunctionParameters parameters;
};

public type ChatCompletionTool record {
    ChatCompletionTool_function 'function;
    ChatCompletionToolType 'type;
};

public type ChatCompletionToolType "function";

public type CreateChatCompletionRequest record {
    ChatCompletionRequestMessage[] messages;
    ChatCompletionTool[] tools?;
};

public type ChatCompletionResponseObject "chat.completion";

public type ChatCompletionsResponseCommon record {
    string id;
    ChatCompletionResponseObject 'object;
    int created;
    string model;
};

public type ToolCallType "function";

public type ChatCompletionMessageToolCall record {
    string id;
    ToolCallType 'type;
    ChatCompletionMessageToolCall_function 'function;
};

public type ChatCompletionMessageToolCall_function record {
    string name;
    string arguments;
};

public type ChatCompletionResponseMessage record {
    @jsondata:Name {value: "tool_calls"}
    ChatCompletionMessageToolCall[] toolCalls?;
};

public type CreateChatCompletionResponse record {
    *ChatCompletionsResponseCommon;
    record {
        ChatCompletionResponseMessage message?;
    }[] choices;
};
