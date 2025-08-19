package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import dev.langchain4j.service.UserMessage;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.LibraryListResponse;

/**
 * AI service interface for library selection
 */
public interface LibrarySelectionService {

    LibraryListResponse selectLibraries(String systemPrompt, @UserMessage String userPrompt);
}
