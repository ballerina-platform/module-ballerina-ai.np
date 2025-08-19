package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Listener;

/**
 * Abstract base class for services, representing common properties
 * such as listener and type. Supports polymorphic behavior for
 * different service types.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GenericService.class, name = "generic"),
        @JsonSubTypes.Type(value = FixedService.class, name = "fixed")
})
public abstract class Service {
    @JsonProperty("listener")
    private Listener listener;

    @JsonProperty("type")
    private String type;

    public Listener getListener() { return listener; }
    public void setListener(Listener listener) { this.listener = listener; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
