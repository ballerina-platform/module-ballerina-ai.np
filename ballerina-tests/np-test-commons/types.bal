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

type OpenAIChatCompletionRequestUserMessage record {
    string content;
    "user" role;
    string name?;
};

type OpenAICreateChatCompletionRequest record {
    OpenAIChatCompletionRequestUserMessage[1] messages;
    string model;
    ChatCompletionTool[] tools?;
};

type FunctionType "function";

type ChatCompletionTool record {
    FunctionType 'type;
    FunctionObject 'function;
};

type FunctionObject record {
    string description?;
    string name;
    FunctionParameters parameters?;
};

type FunctionParameters record {

};

type DefaultChatCompletionRequest record {|
    string prompt;
    map<json> outputSchema;
|};
