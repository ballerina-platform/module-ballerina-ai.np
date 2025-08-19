package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Abstract base class for simplified functions, including their
 * parameters and return type.
 */
public abstract class MiniFunction {
    @JsonProperty("parameters")
    private List<String> parameters;

    @JsonProperty("returnType")
    private String returnType;

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
}
