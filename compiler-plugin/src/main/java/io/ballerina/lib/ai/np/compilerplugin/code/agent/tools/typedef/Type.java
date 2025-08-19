package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a type definition, including its name and associated links.
 */
public class Type {
    @JsonProperty("name")
    private String name;

    @JsonProperty("links")
    private List<Link> links;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Link> getLinks() { return links; }
    public void setLinks(List<Link> links) { this.links = links; }
}