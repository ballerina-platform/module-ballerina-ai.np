package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a link to a type, including its category, record name,
 * and library name.
 */
public class Link {
    @JsonProperty("category")
    private Category category;

    @JsonProperty("recordName")
    private String recordName;

    @JsonProperty("libraryName")
    private String libraryName;

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getRecordName() { return recordName; }
    public void setRecordName(String recordName) { this.recordName = recordName; }

    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
}
