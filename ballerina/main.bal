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

const JSON_CONVERSION_ERROR = "FromJsonStringError";
const CONVERSION_ERROR = "ConversionError";
const ERROR_MESSAGE = "Error occurred while attempting to parse the response from the LLM as the expected type. Retrying and/or validating the prompt could fix the response.";

type DefaultModelConfig DefaultAzureOpenAIModelConfig|DefaultOpenAIModelConfig|DefaultBallerinaModelConfig;

type DefaultAzureOpenAIModelConfig record {|
    *AzureOpenAIModelConfig;
    string deploymentId;
    string apiVersion;
|};

type DefaultOpenAIModelConfig record {|
    *OpenAIModelConfig;
    string model;
|};

public annotation map<json> Schema on type;

final Model? defaultModel;

function init() returns error? {
    DefaultModelConfig? defaultModelConfigVar = defaultModelConfig;
    if defaultModelConfigVar is () {
        defaultModel = ();
        return;
    }

    if defaultModelConfigVar is DefaultAzureOpenAIModelConfig {
        defaultModel = check new AzureOpenAIModel({
            connectionConfig: defaultModelConfigVar.connectionConfig,
            serviceUrl: defaultModelConfigVar.serviceUrl
        }, defaultModelConfigVar.deploymentId, defaultModelConfigVar.apiVersion);
        return;
    }

    if defaultModelConfigVar is DefaultOpenAIModelConfig {
        string? serviceUrl = defaultModelConfigVar?.serviceUrl;
        defaultModel = serviceUrl is () ?
            check new OpenAIModel({
                connectionConfig: defaultModelConfigVar.connectionConfig
            }, defaultModelConfigVar.model) :
            check new OpenAIModel({
                connectionConfig: defaultModelConfigVar.connectionConfig,
                serviceUrl
            }, defaultModelConfigVar.model);
        return;
    }

    defaultModel = check new DefaultBallerinaModel(defaultModelConfigVar);
}

isolated function getDefaultModel() returns Model {
    final Model? defaultModelVar = defaultModel;
    if defaultModelVar is () {
        panic error("Default model is not initialized");
    }
    return defaultModelVar;
}

isolated function buildPromptString(Prompt prompt) returns string {
    string str = prompt.strings[0];
    anydata[] insertions = prompt.insertions;
    foreach int i in 0 ..< insertions.length() {
        str = str + insertions[i].toString() + prompt.strings[i + 1];
    }
    return str.trim();
}

isolated function getPromptWithExpectedResponseSchema(Prompt prompt, typedesc<anydata> expectedResponseTypedesc) returns string =>
    string `${buildPromptString(prompt)}
        ---

        The output should be a JSON value that satisfies the following JSON schema, 
        returned within a markdown snippet enclosed within ${"```json"} and ${"```"}
        
        Schema:
        ${getExpectedResponseSchema(expectedResponseTypedesc).toJsonString()}`;

isolated function callLlmGeneric(Prompt prompt, Context context, 
                                 typedesc<anydata> expectedResponseTypedesc) returns anydata|error {
    Model model = context.model;
    anydata response = check model->call(prompt, expectedResponseTypedesc);
    anydata|error result = response.ensureType(expectedResponseTypedesc);
    if result is error {
        return error(string `Invalid value returned from the LLM Client, expected: ${
            expectedResponseTypedesc.toBalString()}, found ${(typeof response).toBalString()}`);
    }
    return result;
}

// TODO: call from model clients
isolated function parseResponseAsJson(string resp) returns json|error {
    int startDelimLength = 7;
    int? startIndex = resp.indexOf("```json");
    if startIndex is () {
        startIndex = resp.indexOf("```");
        startDelimLength = 3;
    }
    int? endIndex = resp.lastIndexOf("```");

    string processedResponse = startIndex is () || endIndex is () ? 
        resp : 
        resp.substring(startIndex + startDelimLength, endIndex).trim();
    json|error result = trap processedResponse.fromJsonString();
    if result is error {
        return handleParseResponseError(result);
    }
    return result;
}

isolated function parseResponseAsType(string resp, typedesc<anydata> targetType) returns anydata|error {
    json respJson = check parseResponseAsJson(resp);
    anydata|error result = trap respJson.fromJsonWithType(targetType);
    if result is error {
        return handleParseResponseError(result);
    }
    return result;
}

isolated function handleParseResponseError(error chatResponseError) returns error {
    if chatResponseError.message().includes(JSON_CONVERSION_ERROR) 
            || chatResponseError.message().includes(CONVERSION_ERROR) {
        return error(string `${ERROR_MESSAGE}`, detail = chatResponseError);
    }
    return chatResponseError;
}

isolated function getExpectedResponseSchema(typedesc<anydata> expectedResponseTypedesc) returns map<json> {
    // Restricted at compile-time for now.
    typedesc<json> td = checkpanic expectedResponseTypedesc.ensureType();
    return generateJsonSchemaForTypedescAsJson(td);
}
