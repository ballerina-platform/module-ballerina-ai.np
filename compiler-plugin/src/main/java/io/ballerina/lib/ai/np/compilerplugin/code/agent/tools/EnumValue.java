package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a value in an enumeration, including its name and description.
 */
public class EnumValue {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
