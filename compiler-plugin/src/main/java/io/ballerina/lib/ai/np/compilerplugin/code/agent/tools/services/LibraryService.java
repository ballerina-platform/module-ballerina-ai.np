package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Client;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetCopilotCompactLibrariesRequest;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetCopilotCompactLibrariesResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetCopilotFilteredLibrariesRequest;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetCopilotFilteredLibrariesResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetFunctionResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetFunctionsRequest;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetFunctionsResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.GetTypeResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.LangClient;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Library;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.LibraryListResponse;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.MinifiedClient;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.MinifiedLibrary;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.MinifiedRemoteFunction;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.function.RemoteFunction;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.GenerationType;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.TypeDefinition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for handling library operations and function selection
 */
public class LibraryService {

    private final ChatLanguageModel chatModel;
    private final LangClient langClient;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;

    public LibraryService(ChatLanguageModel chatModel, LangClient langClient, UtilsService utilsService) {
        this.chatModel = chatModel;
        this.langClient = langClient;
        this.utilsService = utilsService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Select required functions based on prompt and library names
     */
    public List<Library> selectRequiredFunctions(String prompt, List<String> selectedLibNames,
                                                 GenerationType generationType) throws Exception {

        List<Library> selectedLibs = getMaximizedSelectedLibs(selectedLibNames, generationType);
        List<GetFunctionResponse> functionsResponse = getRequiredFunctions(selectedLibNames, prompt, selectedLibs);

        List<Library> typeLibraries = new ArrayList<>();
        if (generationType == GenerationType.HEALTHCARE_GENERATION) {
            // TODO: Implement getRequiredTypesFromLibJson equivalent
            List<GetTypeResponse> resp = getRequiredTypesFromLibJson(selectedLibNames, prompt, selectedLibs);
            typeLibraries = toTypesToLibraries(resp, selectedLibs);
        }

        List<Library> maximizedLibraries = toMaximizedLibrariesFromLibJson(functionsResponse, selectedLibs);

        // Merge typeLibraries and maximizedLibraries without duplicates
        return mergeLibrariesWithoutDuplicates(maximizedLibraries, typeLibraries);
    }

    /**
     * Get selected libraries based on prompt
     */
    public List<String> getSelectedLibraries(String prompt, GenerationType generationType) throws Exception {
        List<MinifiedLibrary> allLibraries = getAllLibraries(generationType);
        if (allLibraries.isEmpty()) {
            return new ArrayList<>();
        }

        String systemPrompt = getSystemPrompt(allLibraries);
        String userPrompt = getUserPrompt(prompt, generationType);

        long startTime = System.currentTimeMillis();

        // TODO: Implement AI service call to select libraries
        LibrarySelectionService service = AiServices.create(LibrarySelectionService.class, chatModel);
        LibraryListResponse response = service.selectLibraries(systemPrompt, userPrompt);

        long endTime = System.currentTimeMillis();
        System.out.printf("Library selection took %dms%n", endTime - startTime);

        System.out.println("Selected libraries: " + response.getLibraries());
        return response.getLibraries();
    }

    /**
     * Get all available libraries
     */
    public List<MinifiedLibrary> getAllLibraries(GenerationType generationType) throws Exception {
        GetCopilotCompactLibrariesRequest request = new GetCopilotCompactLibrariesRequest();
        request.setMode(utilsService.getGenerationMode(generationType));

        GetCopilotCompactLibrariesResponse result = langClient.getCopilotCompactLibraries(request);
        return result.getLibraries();
    }

    /**
     * Get maximized selected libraries
     */
    public List<Library> getMaximizedSelectedLibs(List<String> libNames, GenerationType generationType) throws Exception {
        GetCopilotFilteredLibrariesRequest request = new GetCopilotFilteredLibrariesRequest();
        request.setLibNames(libNames);
        request.setMode(utilsService.getGenerationMode(generationType));

        GetCopilotFilteredLibrariesResponse result = langClient.getCopilotFilteredLibraries(request);
        return result.getLibraries();
    }

    // Private helper methods

    private String getSystemPrompt(List<MinifiedLibrary> libraryList) {
        try {
            String libraryJson = objectMapper.writeValueAsString(libraryList);
            return String.format(
                    "You are an assistant tasked with selecting all the Ballerina libraries needed to answer a specific question " +
                            "from a given set of libraries provided in the context as a JSON. RESPOND ONLY WITH A JSON.\n\n" +
                            "# Library Context JSON\n\n%s", libraryJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize library list", e);
        }
    }

    private String getUserPrompt(String prompt, GenerationType generationType) {
        String basePrompt = String.format(
                "\n# QUESTION\n\n%s\n\n" +
                        "# Example\n\n" +
                        "Context:\n" +
                        "[\n" +
                        "    {\n" +
                        "        \"name\": \"ballerinax/azure.openai.chat\",\n" +
                        "        \"description\": \"Provides a Ballerina client for the Azure OpenAI Chat API.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"name\": \"ballerinax/github\",\n" +
                        "        \"description\": \"Provides a Ballerina client for the GitHub API.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"name\": \"ballerinax/slack\",\n" +
                        "        \"description\": \"Provides a Ballerina client for the Slack API.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"name\": \"ballerinax/http\",\n" +
                        "        \"description\": \"Allows to intract with HTTP services.\"\n" +
                        "    }\n" +
                        "]\n" +
                        "Question: \n" +
                        "Write an application to read github issue, summarize them and post the summary to a slack channel.\n\n" +
                        "Response: \n" +
                        "{\n" +
                        "    \"libraries\": [\"ballerinax/github\", \"ballerinax/slack\", \"ballerinax/azure.openai.chat\"]\n" +
                        "}", prompt);

        if (generationType == GenerationType.HEALTHCARE_GENERATION) {
            basePrompt += " ALWAYS include `ballerinax/health.base`, `ballerinax/health.fhir.r4`, " +
                    "`ballerinax/health.fhir.r4.parser`, `ballerinax/health.fhir.r4utils`, " +
                    "`ballerinax/health.fhir.r4.international401`, `ballerinax/health.hl7v2commons` " +
                    "and `ballerinax/health.hl7v2` libraries in the selection in addition to what you selected.";
        }

        return basePrompt;
    }

    private List<GetFunctionResponse> getRequiredFunctions(List<String> libraries, String prompt,
                                                           List<Library> librariesJson) throws Exception {
        if (librariesJson.isEmpty()) {
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();

        List<GetFunctionsRequest> libraryList = librariesJson.stream()
                .filter(lib -> libraryContains(lib.getName(), libraries))
                .map(this::convertToGetFunctionsRequest)
                .collect(Collectors.toList());

        List<GetFunctionsRequest> largeLibs = libraryList.stream()
                .filter(lib -> getClientFunctionCount(lib.getClients()) >= 100)
                .collect(Collectors.toList());

        List<GetFunctionsRequest> smallLibs = libraryList.stream()
                .filter(lib -> !largeLibs.contains(lib))
                .collect(Collectors.toList());

        System.out.printf("[Parallel Execution Plan] Large libraries: %d (%s), Small libraries: %d (%s)%n",
                largeLibs.size(), largeLibs.stream().map(GetFunctionsRequest::getName).collect(Collectors.joining(", ")),
                smallLibs.size(), smallLibs.stream().map(GetFunctionsRequest::getName).collect(Collectors.joining(", ")));

        // Create futures for large libraries (each processed individually)
        List<CompletableFuture<List<GetFunctionResponse>>> largeLiberiesPromises = largeLibs.stream()
                .map(funcItem -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return getSuggestedFunctions(prompt, Arrays.asList(funcItem));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .collect(Collectors.toList());

        // Create future for small libraries (processed in bulk)
        CompletableFuture<List<GetFunctionResponse>> smallLibrariesPromise =
                !smallLibs.isEmpty()
                        ? CompletableFuture.supplyAsync(() -> {
                    try {
                        return getSuggestedFunctions(prompt, smallLibs);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                        : CompletableFuture.completedFuture(new ArrayList<>());

        System.out.printf("[Parallel Execution Start] Starting %d large library requests + 1 small libraries bulk request%n",
                largeLiberiesPromises.size());
        long parallelStartTime = System.currentTimeMillis();

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                Stream.concat(
                        Stream.of(smallLibrariesPromise),
                        largeLiberiesPromises.stream()
                ).toArray(CompletableFuture[]::new)
        );

        allFutures.join();

        long parallelEndTime = System.currentTimeMillis();
        double parallelDuration = (parallelEndTime - parallelStartTime) / 1000.0;

        System.out.printf("[Parallel Execution Complete] Total parallel execution time: %.3fs%n", parallelDuration);

        // Collect results
        List<GetFunctionResponse> collectiveResp = new ArrayList<>();
        collectiveResp.addAll(smallLibrariesPromise.get());

        for (CompletableFuture<List<GetFunctionResponse>> future : largeLiberiesPromises) {
            collectiveResp.addAll(future.get());
        }

        long endTime = System.currentTimeMillis();
        double totalDuration = (endTime - startTime) / 1000.0;

        int totalFunctionCount = collectiveResp.stream()
                .mapToInt(lib ->
                        (lib.getClients() != null ? lib.getClients().stream().mapToInt(client -> client.getFunctions().size()).sum() : 0) +
                                (lib.getFunctions() != null ? lib.getFunctions().size() : 0))
                .sum();

        System.out.printf("[getRequiredFunctions Complete] Total function count: %d, Total duration: %.3fs, " +
                        "Preparation time: %.3fs, Parallel time: %.3fs%n",
                totalFunctionCount, totalDuration, (parallelStartTime - startTime) / 1000.0, parallelDuration);

        return collectiveResp;
    }

    private List<GetFunctionResponse> getSuggestedFunctions(String prompt, List<GetFunctionsRequest> libraryList) throws Exception {
        long startTime = System.currentTimeMillis();
        String libraryNames = libraryList.stream().map(GetFunctionsRequest::getName).collect(Collectors.joining(", "));
        int functionCount = libraryList.stream()
                .mapToInt(lib -> getClientFunctionCount(lib.getClients()) +
                        (lib.getFunctions() != null ? lib.getFunctions().size() : 0))
                .sum();

        System.out.printf("[AI Request Start] Libraries: [%s], Function Count: %d%n", libraryNames, functionCount);

        String getLibSystemPrompt = String.format(
                "You are an AI assistant tasked with filtering and removing unwanted functions and clients from a provided " +
                        "set of libraries and clients based on a user query. Your goal is to return ONLY the relevant libraries, " +
                        "clients, and functions from the provided context that match the user's needs. Do NOT include any libraries " +
                        "or functions not explicitly listed in the provided Library_Context_JSON.\n\n" +
                        "Rules:\n" +
                        "1. Use ONLY the libraries listed in Library_Context_JSON (e.g., %s).\n" +
                        "2. Do NOT create or infer new libraries or functions.", libraryNames);

        String getLibUserPrompt;
        try {
            getLibUserPrompt = String.format(
                    "You will be provided with a list of libraries, clients, and their functions, and a user query.\n\n" +
                            "<QUERY>\n%s\n</QUERY>\n\n" +
                            "<Library_Context_JSON>\n%s\n</Library_Context_JSON>\n\n" +
                            "To process the user query and filter the libraries, clients, and functions, follow these steps:\n\n" +
                            "1. Analyze the user query to understand the specific requirements or needs\n" +
                            "2. Review the provided libraries, clients, and functions in Library_Context_JSON.\n" +
                            "3. Select only the libraries, clients, and functions that directly match the query's needs.\n" +
                            "4. Exclude any irrelevant libraries, clients, or functions.\n" +
                            "5. If no relevant functions are found, return an empty array for the library's functions or clients.\n" +
                            "6. Organize the remaining relevant information.\n\n" +
                            "Now, based on the provided libraries, clients, and functions, and the user query, please filter and return the relevant information.",
                    prompt, objectMapper.writeValueAsString(libraryList));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize library list", e);
        }

        try {
            // TODO: Implement AI service call to get suggested functions
            FunctionSelectionService service = AiServices.create(FunctionSelectionService.class, chatModel);
            GetFunctionsResponse response = service.selectFunctions(getLibSystemPrompt, getLibUserPrompt);

            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            // Filter to remove hallucinated libraries
            List<GetFunctionResponse> filteredLibList = response.getLibraries().stream()
                    .filter(lib -> libraryList.stream().anyMatch(inputLib -> inputLib.getName().equals(lib.getName())))
                    .collect(Collectors.toList());

            System.out.printf("[AI Request Complete] Libraries: [%s], Duration: %.3fs, Selected Functions: %d%n",
                    libraryNames, duration,
                    filteredLibList.stream().mapToInt(lib ->
                            (lib.getClients() != null ? lib.getClients().stream().mapToInt(client -> client.getFunctions().size()).sum() : 0) +
                                    (lib.getFunctions() != null ? lib.getFunctions().size() : 0)).sum());

            printSelectedFunctions(filteredLibList);
            return filteredLibList;

        } catch (Exception error) {
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            System.err.printf("[AI Request Failed] Libraries: [%s], Duration: %.3fs, Error: %s%n",
                    libraryNames, duration, error.getMessage());
            throw new Exception("Failed to parse bulk functions response: " + error.getMessage(), error);
        }
    }

    // Helper methods

    private int getClientFunctionCount(List<MinifiedClient> clients) {
        return clients.stream().mapToInt(client -> client.getFunctions().size()).sum();
    }

    private boolean libraryContains(String library, List<String> libraries) {
        return libraries.contains(library);
    }

    private GetFunctionsRequest convertToGetFunctionsRequest(Library lib) {
        GetFunctionsRequest request = new GetFunctionsRequest();
        request.setName(lib.getName());
        request.setDescription(lib.getDescription());
        request.setClients(filteredClients(lib.getClients()));
        request.setFunctions(filteredNormalFunctions(lib.getFunctions()));
        return request;
    }

    private List<MinifiedClient> filteredClients(List<Client> clients) {
        return clients.stream()
                .map(cli -> {
                    MinifiedClient minClient = new MinifiedClient();
                    minClient.setName(cli.getName());
                    minClient.setDescription(cli.getDescription());
                    minClient.setFunctions(filteredFunctions(cli.getFunctions()));
                    return minClient;
                })
                .collect(Collectors.toList());
    }

    private List<Object> filteredFunctions(List<Object> functions) {
        // TODO: Implement function filtering logic based on RemoteFunction/ResourceFunction types
        List<Object> output = new ArrayList<>();

        for (Object item : functions) {
            // This needs to be implemented based on the actual function types
            // For now, adding as placeholder
            output.add(item);
        }

        return output;
    }

    private List<MinifiedRemoteFunction> filteredNormalFunctions(List<RemoteFunction> functions) {
        if (functions == null) {
            return null;
        }

        return functions.stream()
                .map(item -> {
                    MinifiedRemoteFunction minFunc = new MinifiedRemoteFunction();
                    minFunc.setName(item.getName());
                    minFunc.setParameters(item.getParameters().stream()
                            .map(param -> param.getName())
                            .collect(Collectors.toList()));
                    minFunc.setReturnType(item.getReturn().getType().getName());
                    return minFunc;
                })
                .collect(Collectors.toList());
    }

    private void printSelectedFunctions(List<GetFunctionResponse> libraries) {
        try {
            System.out.println("Selected functions: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(libraries));
        } catch (Exception e) {
            System.err.println("Failed to print selected functions: " + e.getMessage());
        }
    }

    private List<Library> toMaximizedLibrariesFromLibJson(List<GetFunctionResponse> functionResponses,
                                                          List<Library> originalLibraries) throws Exception {
        // TODO: Implement complete logic for maximizing libraries from function responses
        List<Library> minifiedLibrariesWithoutRecords = new ArrayList<>();

        for (GetFunctionResponse funcResponse : functionResponses) {
            Library originalLib = originalLibraries.stream()
                    .filter(lib -> lib.getName().equals(funcResponse.getName()))
                    .findFirst()
                    .orElse(null);

            if (originalLib == null) {
                continue;
            }

            // TODO: Implement selectClients and selectFunctions methods
            List<Client> filteredClients = selectClients(originalLib.getClients(), funcResponse);
            List<RemoteFunction> filteredFunctions = selectFunctions(originalLib.getFunctions(), funcResponse);

            Library maximizedLib = new Library();
            maximizedLib.setName(funcResponse.getName());
            maximizedLib.setDescription(originalLib.getDescription());
            maximizedLib.setClients(filteredClients);
            maximizedLib.setFunctions(filteredFunctions);
            // TODO: Implement getOwnTypeDefsForLib
            maximizedLib.setTypeDefs(getOwnTypeDefsForLib(filteredClients, filteredFunctions, originalLib.getTypeDefs()));
            maximizedLib.setServices(originalLib.getServices());

            minifiedLibrariesWithoutRecords.add(maximizedLib);
        }

        // TODO: Implement external type references handling
        Map<String, List<String>> externalRecordsRefs = getExternalTypeDefsRefs(minifiedLibrariesWithoutRecords);
        getExternalRecords(minifiedLibrariesWithoutRecords, externalRecordsRefs, originalLibraries);

        return minifiedLibrariesWithoutRecords;
    }

    private List<Library> toTypesToLibraries(List<GetTypeResponse> types, List<Library> fullLibs) {
        // TODO: Implement types to libraries conversion
        List<Library> librariesWithTypes = new ArrayList<>();

        for (GetTypeResponse minifiedSelectedLib : types) {
            try {
                Library fullDefOfSelectedLib = getLibraryByNameFromLibJson(minifiedSelectedLib.getLibName(), fullLibs);
                if (fullDefOfSelectedLib == null) {
                    continue;
                }

                // TODO: Implement selectTypes method
                List<TypeDefinition> filteredTypes = selectTypes(fullDefOfSelectedLib.getTypeDefs(), minifiedSelectedLib);

                Library typeLib = new Library();
                typeLib.setName(fullDefOfSelectedLib.getName());
                typeLib.setDescription(fullDefOfSelectedLib.getDescription());
                typeLib.setTypeDefs(filteredTypes);
                typeLib.setServices(fullDefOfSelectedLib.getServices());
                typeLib.setClients(new ArrayList<>());

                librariesWithTypes.add(typeLib);
            } catch (Exception error) {
                System.err.printf("Error processing library %s: %s%n", minifiedSelectedLib.getLibName(), error.getMessage());
            }
        }

        return librariesWithTypes;
    }

    private Library getLibraryByNameFromLibJson(String libName, List<Library> librariesJson) {
        return librariesJson.stream()
                .filter(lib -> lib.getName().equals(libName))
                .findFirst()
                .orElse(null);
    }

    private List<Library> mergeLibrariesWithoutDuplicates(List<Library> maximizedLibraries, List<Library> typeLibraries) {
        List<Library> finalLibraries = new ArrayList<>(maximizedLibraries);

        for (Library typeLib : typeLibraries) {
            Library finalLib = findLibraryByName(typeLib.getName(), finalLibraries);
            if (finalLib != null) {
                finalLib.getTypeDefs().addAll(typeLib.getTypeDefs());
            } else {
                finalLibraries.add(typeLib);
            }
        }

        return finalLibraries;
    }

    private Library findLibraryByName(String name, List<Library> libraries) {
        return libraries.stream()
                .filter(lib -> lib.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    // TODO: Implement remaining helper methods
    private List<GetTypeResponse> getRequiredTypesFromLibJson(List<String> selectedLibNames, String prompt, List<Library> selectedLibs) {
        // TODO: Implement healthcare type selection logic
        return new ArrayList<>();
    }

    private List<TypeDefinition> selectTypes(List<TypeDefinition> fullDefOfSelectedLib, GetTypeResponse minifiedSelectedLib) {
        // TODO: Implement type selection logic
        return new ArrayList<>();
    }

    private List<Client> selectClients(List<Client> originalClients, GetFunctionResponse funcResponse) {
        // TODO: Implement client selection logic
        return new ArrayList<>();
    }

    private List<RemoteFunction> selectFunctions(List<RemoteFunction> originalFunctions, GetFunctionResponse funcResponse) {
        // TODO: Implement function selection logic
        return new ArrayList<>();
    }

    private List<TypeDefinition> getOwnTypeDefsForLib(List<Client> filteredClients, List<RemoteFunction> filteredFunctions,
                                                      List<TypeDefinition> allTypeDefs) {
        // TODO: Implement type definition collection logic
        return new ArrayList<>();
    }

    private Map<String, List<String>> getExternalTypeDefsRefs(List<Library> libraries) {
        // TODO: Implement external type references collection
        return new HashMap<>();
    }

    private void getExternalRecords(List<Library> newLibraries, Map<String, List<String>> libRefs, List<Library> originalLibraries) {
        // TODO: Implement external records fetching logic
    }
}
