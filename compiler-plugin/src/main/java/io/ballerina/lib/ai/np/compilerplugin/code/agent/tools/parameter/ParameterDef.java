package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.parameter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.Type;

/**
 * Represents a parameter definition, including its description,
 * type, default value, and optional flag.
 */
public class ParameterDef {
    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("default")
    private String defaultValue;

    @JsonProperty("optional")
    private boolean optional;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }
}
