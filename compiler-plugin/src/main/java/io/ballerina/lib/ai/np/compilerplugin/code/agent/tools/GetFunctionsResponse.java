package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a response containing libraries with their associated
 * functions and clients.
 */
public class GetFunctionsResponse {
    @JsonProperty("libraries")
    private List<GetFunctionResponse> libraries;

    public List<GetFunctionResponse> getLibraries() { return libraries; }
    public void setLibraries(List<GetFunctionResponse> libraries) { this.libraries = libraries; }
}
