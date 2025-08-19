package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a union value, including its name and type.
 */
public class UnionValue {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private Type type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
