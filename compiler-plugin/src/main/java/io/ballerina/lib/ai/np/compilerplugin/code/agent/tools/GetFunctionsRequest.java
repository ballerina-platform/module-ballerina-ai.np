package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.MinifiedRemoteFunction;

import java.util.List;

/**
 * Represents a request to retrieve functions, including the name,
 * description, associated clients, and functions.
 */
public class GetFunctionsRequest {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("clients")
    private List<MinifiedClient> clients;

    @JsonProperty("functions")
    private List<MinifiedRemoteFunction> functions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<MinifiedClient> getClients() { return clients; }
    public void setClients(List<MinifiedClient> clients) { this.clients = clients; }

    public List<MinifiedRemoteFunction> getFunctions() { return functions; }
    public void setFunctions(List<MinifiedRemoteFunction> functions) { this.functions = functions; }
}
