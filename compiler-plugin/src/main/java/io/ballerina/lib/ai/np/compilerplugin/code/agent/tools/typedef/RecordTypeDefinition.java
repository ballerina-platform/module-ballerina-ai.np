package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Field;

import java.util.List;

public class RecordTypeDefinition extends TypeDefinition {
    @JsonProperty("fields")
    private List<Field> fields;

    public List<Field> getFields() { return fields; }
    public void setFields(List<Field> fields) { this.fields = fields; }
}
