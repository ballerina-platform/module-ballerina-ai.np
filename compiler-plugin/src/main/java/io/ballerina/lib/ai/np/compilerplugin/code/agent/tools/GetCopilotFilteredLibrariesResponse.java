package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a response containing a list of filtered libraries.
 */
public class GetCopilotFilteredLibrariesResponse {
    @JsonProperty("libraries")
    private List<Library> libraries;

    public List<Library> getLibraries() { return libraries; }
    public void setLibraries(List<Library> libraries) { this.libraries = libraries; }
}
