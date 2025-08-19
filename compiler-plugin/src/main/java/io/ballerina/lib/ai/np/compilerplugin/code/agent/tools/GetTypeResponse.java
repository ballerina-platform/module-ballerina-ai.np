package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.MiniType;

import java.util.List;

/**
 * Represents a response containing types for a specific library.
 */
public class GetTypeResponse {
    @JsonProperty("libName")
    private String libName;

    @JsonProperty("types")
    private List<MiniType> types;

    public String getLibName() { return libName; }
    public void setLibName(String libName) { this.libName = libName; }

    public List<MiniType> getTypes() { return types; }
    public void setTypes(List<MiniType> types) { this.types = types; }
}
