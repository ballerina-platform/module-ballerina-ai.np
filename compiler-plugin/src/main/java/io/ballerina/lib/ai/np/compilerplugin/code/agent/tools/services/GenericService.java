package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a generic service with additional instructions.
 */
public class GenericService extends Service {
    @JsonProperty("instructions")
    private String instructions;

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
