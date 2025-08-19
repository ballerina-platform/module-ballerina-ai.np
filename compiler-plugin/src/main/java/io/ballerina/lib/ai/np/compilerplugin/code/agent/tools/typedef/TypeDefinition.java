package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RecordTypeDefinition.class, name = "record"),
        @JsonSubTypes.Type(value = EnumTypeDefinition.class, name = "enum"),
        @JsonSubTypes.Type(value = UnionTypeDefinition.class, name = "union"),
        @JsonSubTypes.Type(value = ClassTypeDefinition.class, name = "class"),
        @JsonSubTypes.Type(value = ConstantTypeDefinition.class, name = "constant")
})
public abstract class TypeDefinition {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
