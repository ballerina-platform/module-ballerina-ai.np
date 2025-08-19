package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteFunction extends AbstractFunction {
    @JsonProperty("name")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
