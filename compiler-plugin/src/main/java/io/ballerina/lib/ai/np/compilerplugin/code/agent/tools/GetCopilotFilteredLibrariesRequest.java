package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a request to retrieve filtered libraries, including
 * library names and mode.
 */
public class GetCopilotFilteredLibrariesRequest {
    @JsonProperty("libNames")
    private List<String> libNames;

    @JsonProperty("mode")
    private String mode;

    public List<String> getLibNames() { return libNames; }
    public void setLibNames(List<String> libNames) { this.libNames = libNames; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
