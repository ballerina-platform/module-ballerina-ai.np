package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.JsonSchemaProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services.LibraryService;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.GenerationType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Library Provider Tool for fetching detailed information about Ballerina libraries
 */
public class LibraryProviderTool {

    private final String libraryDescriptions;
    private final GenerationType generationType;
    private final LibraryService libraryService;
    private final ObjectMapper objectMapper;

    public LibraryProviderTool(String libraryDescriptions, GenerationType generationType,
                               LibraryService libraryService) {
        this.libraryDescriptions = libraryDescriptions;
        this.generationType = generationType;
        this.libraryService = libraryService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Tool method that will be exposed to the agent
     */
    @Tool("Fetches detailed information about Ballerina libraries, including clients, functions, and types. " +
            "This tool analyzes a user query and returns **only the relevant** clients, functions, and types " +
            "from the selected Ballerina libraries based on the provided user prompt.\n\n" +
            "Before calling this tool:\n" +
            "- **Review all library descriptions** below.\n" +
            "- Select only the libraries that might be needed to fulfill the user query.\n\n" +
            "Available libraries:\n%s")
    public List<Library> getLibraryProvider(
            @JsonProperty("libraryNames") List<String> libraryNames,
            @JsonProperty("userPrompt") String userPrompt) {

        try {
            long startTime = System.currentTimeMillis();

            System.out.printf("[LibraryProviderTool] Called with %d libraries: %s and prompt: %s%n",
                    libraryNames.size(), String.join(", ", libraryNames), userPrompt);

            List<Library> libraries = libraryProviderTool(libraryNames, userPrompt, generationType);

            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("[LibraryProviderTool] Fetched %d libraries: %s, took %.3fs%n",
                    libraries.size(),
                    libraries.stream().map(Library::getName).collect(Collectors.joining(", ")),
                    duration / 1000.0);

            return libraries;

        } catch (Exception error) {
            System.err.printf("[LibraryProviderTool] Error fetching libraries: %s%n", error.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Core implementation of the library provider tool
     */
    private List<Library> libraryProviderTool(List<String> libraryNames, String userPrompt,
                                              GenerationType generationType) throws Exception {
        return libraryService.selectRequiredFunctions(userPrompt, libraryNames, generationType);
    }

    /**
     * Create tool specification for langchain4j
     */
    public static ToolSpecification createToolSpecification(String libraryDescriptions) {
        return ToolSpecification.builder()
                .name("getLibraryProviderTool")
                .description(String.format(
                        "Fetches detailed information about Ballerina libraries, including clients, functions, and types. " +
                                "This tool analyzes a user query and returns **only the relevant** clients, functions, and types " +
                                "from the selected Ballerina libraries based on the provided user prompt.\n\n" +
                                "Before calling this tool:\n" +
                                "- **Review all library descriptions** below.\n" +
                                "- Select only the libraries that might be needed to fulfill the user query.\n\n" +
                                "Available libraries:\n%s", libraryDescriptions))
                .addParameter("libraryNames", JsonSchemaProperty.STRING, JsonSchemaProperty.description(
                        "List of Ballerina library names to fetch details for"))
                .addParameter("userPrompt", JsonSchemaProperty.STRING, JsonSchemaProperty.description(
                        "User query to determine relevant functions and types"))
                .build();
    }
}
