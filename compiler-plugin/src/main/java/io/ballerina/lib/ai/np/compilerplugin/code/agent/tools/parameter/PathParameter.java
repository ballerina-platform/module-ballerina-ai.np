package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.parameter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a path parameter, including its name and type.
 */
public class PathParameter {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
