package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a request to retrieve compact libraries, including the mode.
 */
public class GetCopilotCompactLibrariesRequest {
    @JsonProperty("mode")
    private String mode;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}