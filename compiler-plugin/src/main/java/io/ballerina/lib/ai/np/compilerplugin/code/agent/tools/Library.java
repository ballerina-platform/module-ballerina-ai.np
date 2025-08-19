package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.RemoteFunction;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services.Service;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.TypeDefinition;

import java.util.List;

public class Library {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("typeDefs")
    private List<TypeDefinition> typeDefs;

    @JsonProperty("clients")
    private List<Client> clients;

    @JsonProperty("functions")
    private List<RemoteFunction> functions;

    @JsonProperty("services")
    private List<Service> services;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<TypeDefinition> getTypeDefs() { return typeDefs; }
    public void setTypeDefs(List<TypeDefinition> typeDefs) { this.typeDefs = typeDefs; }

    public List<Client> getClients() { return clients; }
    public void setClients(List<Client> clients) { this.clients = clients; }

    public List<RemoteFunction> getFunctions() { return functions; }
    public void setFunctions(List<RemoteFunction> functions) { this.functions = functions; }

    public List<Service> getServices() { return services; }
    public void setServices(List<Service> services) { this.services = services; }
}