package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.ServiceRemoteFunction;

import java.util.List;

/**
 * Represents a fixed service with a list of remote functions (methods).
 */
public class FixedService extends Service {
    @JsonProperty("methods")
    private List<ServiceRemoteFunction> methods;

    public List<ServiceRemoteFunction> getMethods() { return methods; }
    public void setMethods(List<ServiceRemoteFunction> methods) { this.methods = methods; }
}
