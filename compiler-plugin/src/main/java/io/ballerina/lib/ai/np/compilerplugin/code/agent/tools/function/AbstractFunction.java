package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Return;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.parameter.Parameter;

import java.util.List;

/**
 * Abstract base class for functions, representing common properties
 * such as type, description, parameters, and return value.
 */
public abstract class AbstractFunction {
    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private List<Parameter> parameters;

    @JsonProperty("return")
    private Return returnValue;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }

    public Return getReturn() { return returnValue; }
    public void setReturn(Return returnValue) { this.returnValue = returnValue; }
}
