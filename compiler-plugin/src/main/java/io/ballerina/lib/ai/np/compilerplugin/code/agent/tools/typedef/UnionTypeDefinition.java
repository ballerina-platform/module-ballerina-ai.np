package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UnionTypeDefinition extends TypeDefinition {
    @JsonProperty("members")
    private List<UnionValue> members;

    public List<UnionValue> getMembers() { return members; }
    public void setMembers(List<UnionValue> members) { this.members = members; }
}
