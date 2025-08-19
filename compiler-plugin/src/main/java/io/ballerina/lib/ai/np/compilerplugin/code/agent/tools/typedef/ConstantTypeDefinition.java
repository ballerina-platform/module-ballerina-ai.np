package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConstantTypeDefinition extends TypeDefinition {
    @JsonProperty("value")
    private String value;

    @JsonProperty("varType")
    private Type varType;

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Type getVarType() { return varType; }
    public void setVarType(Type varType) { this.varType = varType; }
}
