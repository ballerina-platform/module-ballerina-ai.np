package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.Type;

/**
 * Represents the return value of a function, including its
 * description and type.
 */
public class Return {
    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private Type type;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
