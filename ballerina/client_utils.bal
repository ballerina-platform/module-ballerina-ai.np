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
import ballerina/url;
type ClientHttp1Settings record {|
    http:KeepAlive keepAlive = http:KEEPALIVE_AUTO;
    http:Chunking chunking = http:CHUNKING_AUTO;
    ProxyConfig proxy?;
|};
type ProxyConfig record {|
    string host = "";
    int port = 0;
    string userName = "";
    @display {label: "", kind: "password"}
    string password = "";
|};
type OpenAIConnectionConfig record {|
    http:BearerTokenConfig auth;
    http:HttpVersion httpVersion = http:HTTP_2_0;
    ClientHttp1Settings http1Settings?;
    http:ClientHttp2Settings http2Settings?;
    decimal timeout = 60;
    string forwarded = "disable";
    http:PoolConfiguration poolConfig?;
    http:CacheConfig cache?;
    http:Compression compression = http:COMPRESSION_AUTO;
    http:CircuitBreakerConfig circuitBreaker?;
    http:RetryConfig retryConfig?;
    http:ResponseLimitConfigs responseLimits?;
    http:ClientSecureSocket secureSocket?;
    http:ProxyConfig proxy?;
    boolean validation = true;
|};

type ApiKeysConfig record {|
    @display {label: "", kind: "password"}
    string apiKey;
|};

type OpenAIChatCompletionRequestUserMessage record {
    string content;
    "user" role;
    string name?;
};

type OpenAICreateChatCompletionRequest record {
    OpenAIChatCompletionRequestUserMessage[1] messages;
    string model;
    boolean? store = false;
    decimal? frequency_penalty = 0;
    boolean? logprobs = false;
    int? n = 1;
    decimal? presence_penalty = 0;
    "auto"|"default"? service_tier = "auto";
    boolean? 'stream = false;
    decimal? temperature = 1;
    decimal? top_p = 1;
    ChatCompletionTool[] tools?;
    ChatCompletionToolChoiceOption tool_choice?;
};

type ChatCompletionToolChoiceOption ChatCompletionNamedToolChoice;

type ChatCompletionTool record {
    "function" 'type;
    FunctionObject 'function;
};

type FunctionObject record {
    string description?;
    string name;
    FunctionParameters parameters?;
    boolean? strict = false;
};

type FunctionParameters record {
};

type ChatCompletionNamedToolChoice record {
    "function" 'type;
    AssistantsNamedToolChoice_function 'function;
};

type AssistantsNamedToolChoice_function record {
    string name;
};

type OpenAIChatCompletionResponseMessage record {
    string? content;
    ChatCompletionMessageToolCalls tool_calls?;
};

type ChatCompletionMessageToolCalls ChatCompletionMessageToolCall[];

type ChatCompletionMessageToolCall record {
    string id;
    "function" 'type;
    ChatCompletionMessageToolCall_function 'function;
};
type ChatCompletionMessageToolCall_function record {
    string name;
    string arguments;
};

type OpenAICreateChatCompletionResponse_choices record {
    OpenAIChatCompletionResponseMessage message;
};

type OpenAICreateChatCompletionResponse record {
    OpenAICreateChatCompletionResponse_choices[] choices;
};

type AzureOpenAIConnectionConfig record {|
    http:BearerTokenConfig|ApiKeysConfig auth;
    http:HttpVersion httpVersion = http:HTTP_2_0;
    ClientHttp1Settings http1Settings?;
    http:ClientHttp2Settings http2Settings?;
    decimal timeout = 60;
    string forwarded = "disable";
    http:PoolConfiguration poolConfig?;
    http:CacheConfig cache?;
    http:Compression compression = http:COMPRESSION_AUTO;
    http:CircuitBreakerConfig circuitBreaker?;
    http:RetryConfig retryConfig?;
    http:ResponseLimitConfigs responseLimits?;
    http:ClientSecureSocket secureSocket?;
    http:ProxyConfig proxy?;
    boolean validation = true;
|};

type AzureOpenAIChatCompletionRequestMessageRole "system"|"user"|"assistant"|"tool"|"function";

type AzureOpenAIChatCompletionRequestMessage record {|
    AzureOpenAIChatCompletionRequestMessageRole role;
    string content;
|};

type AzureOpenAICreateChatCompletionRequest record {|
    AzureOpenAIChatCompletionRequestMessage[1] messages;
    ChatCompletionTool[] tools?;
    ChatCompletionToolChoiceOption tool_choice?;
|};

type AzureOpenAIChatCompletionResponseMessage record {
    string? content?;
    ChatCompletionMessageToolCalls tool_calls?;
};

type AzureOpenAICreateChatCompletionResponse record {
    record {
        AzureOpenAIChatCompletionResponseMessage message?;
    }[] choices?;
};

isolated function buildHttpClientConfig(AzureOpenAIConnectionConfig config) returns http:ClientConfiguration {
    http:ClientConfiguration httpClientConfig = {
        httpVersion: config.httpVersion,
        timeout: config.timeout,
        forwarded: config.forwarded,
        poolConfig: config.poolConfig,
        compression: config.compression,
        circuitBreaker: config.circuitBreaker,
        retryConfig: config.retryConfig,
        validation: config.validation
    };

    ClientHttp1Settings? http1Settings = config.http1Settings;
    if http1Settings is ClientHttp1Settings {
        httpClientConfig.http1Settings = {...http1Settings};
    }
    http:ClientHttp2Settings? http2Settings = config.http2Settings;
    if http2Settings is http:ClientHttp2Settings {
        httpClientConfig.http2Settings = {...http2Settings};
    }
    http:CacheConfig? cache = config.cache;
    if cache is http:CacheConfig {
        httpClientConfig.cache = cache;
    }
    http:ResponseLimitConfigs? responseLimits = config.responseLimits;
    if responseLimits is http:ResponseLimitConfigs {
        httpClientConfig.responseLimits = responseLimits;
    }
    http:ClientSecureSocket? secureSocket = config.secureSocket;
    if secureSocket is http:ClientSecureSocket {
        httpClientConfig.secureSocket = secureSocket;
    }
    http:ProxyConfig? proxy = config.proxy;
    if proxy is http:ProxyConfig {
        httpClientConfig.proxy = proxy;
    }
    return httpClientConfig;
}

isolated function getEncodedUri(anydata value) returns string|error => url:encode(value.toString(), "UTF8");
