package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResourceFunction extends AbstractFunction {
    @JsonProperty("accessor")
    private String accessor;

    @JsonProperty("paths")
    private List<Object> paths; // Can be PathParameter or String

    public String getAccessor() { return accessor; }
    public void setAccessor(String accessor) { this.accessor = accessor; }

    public List<Object> getPaths() { return paths; }
    public void setPaths(List<Object> paths) { this.paths = paths; }
}
