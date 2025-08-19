package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.parameter.ParameterDef;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Return;

import java.util.List;

/**
 * Represents a remote function in a service, including its type,
 * description, parameters, return value, and optional flag.
 */
public class ServiceRemoteFunction {
    @JsonProperty("type")
    private String type; // "remote" or "resource"

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private List<ParameterDef> parameters;

    @JsonProperty("return")
    private Return returnValue;

    @JsonProperty("optional")
    private boolean optional;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ParameterDef> getParameters() { return parameters; }
    public void setParameters(List<ParameterDef> parameters) { this.parameters = parameters; }

    public Return getReturn() { return returnValue; }
    public void setReturn(Return returnValue) { this.returnValue = returnValue; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }
}
