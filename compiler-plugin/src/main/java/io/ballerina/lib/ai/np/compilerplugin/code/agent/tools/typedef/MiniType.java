package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a simplified type, including its name and description.
 */
public class MiniType {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
