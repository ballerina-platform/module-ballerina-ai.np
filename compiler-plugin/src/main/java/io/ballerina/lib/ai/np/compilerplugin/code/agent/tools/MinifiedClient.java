package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a simplified client, including its name, description,
 * and associated functions.
 */
public class MinifiedClient {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("functions")
    private List<Object> functions; // Can be MinifiedRemoteFunction or MinifiedResourceFunction

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Object> getFunctions() { return functions; }
    public void setFunctions(List<Object> functions) { this.functions = functions; }
}
