package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

/**
 * Interface for language client operations, providing methods to
 * retrieve compact and filtered libraries.
 */
public interface LangClient {
    GetCopilotCompactLibrariesResponse getCopilotCompactLibraries(GetCopilotCompactLibrariesRequest request) throws Exception;
    GetCopilotFilteredLibrariesResponse getCopilotFilteredLibraries(GetCopilotFilteredLibrariesRequest request) throws Exception;
}
