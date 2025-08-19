package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import dev.langchain4j.service.UserMessage;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetFunctionsResponse;

/**
 * AI service interface for function selection
 */
public interface FunctionSelectionService {

    GetFunctionsResponse selectFunctions(String systemPrompt, @UserMessage String userPrompt);
}