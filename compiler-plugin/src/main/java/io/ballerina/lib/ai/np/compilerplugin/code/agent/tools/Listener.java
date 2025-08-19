package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.parameter.Parameter;

import java.util.List;

/**
 * Represents a listener with a name and a list of parameters.
 */
public class Listener {
    @JsonProperty("name")
    private String name;

    @JsonProperty("parameters")
    private List<Parameter> parameters;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }
}
