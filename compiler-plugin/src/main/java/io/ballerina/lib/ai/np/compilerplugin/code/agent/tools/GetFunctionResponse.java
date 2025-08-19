package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.MinifiedRemoteFunction;

import java.util.List;

/**
 * Represents a response for a single library, including its name,
 * associated clients, and functions.
 */
public class GetFunctionResponse {
    @JsonProperty("name")
    private String name;

    @JsonProperty("clients")
    private List<MinifiedClient> clients;

    @JsonProperty("functions")
    private List<MinifiedRemoteFunction> functions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<MinifiedClient> getClients() { return clients; }
    public void setClients(List<MinifiedClient> clients) { this.clients = clients; }

    public List<MinifiedRemoteFunction> getFunctions() { return functions; }
    public void setFunctions(List<MinifiedRemoteFunction> functions) { this.functions = functions; }
}
