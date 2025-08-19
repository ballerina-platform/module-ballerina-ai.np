package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.EnumValue;

import java.util.List;

public class EnumTypeDefinition extends TypeDefinition {
    @JsonProperty("members")
    private List<EnumValue> members;

    public List<EnumValue> getMembers() { return members; }
    public void setMembers(List<EnumValue> members) { this.members = members; }
}
