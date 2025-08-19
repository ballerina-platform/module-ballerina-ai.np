package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a response containing a list of compact libraries.
 */
public class GetCopilotCompactLibrariesResponse {
    @JsonProperty("libraries")
    private List<MinifiedLibrary> libraries;

    public List<MinifiedLibrary> getLibraries() { return libraries; }
    public void setLibraries(List<MinifiedLibrary> libraries) { this.libraries = libraries; }
}
