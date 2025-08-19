package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ClassTypeDefinition extends TypeDefinition {
    @JsonProperty("functions")
    private List<Object> functions;

    public List<Object> getFunctions() { return functions; }
    public void setFunctions(List<Object> functions) { this.functions = functions; }
}
