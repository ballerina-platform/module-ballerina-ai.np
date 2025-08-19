package io.ballerina.lib.ai.np.compilerplugin.code.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.Library;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.GenerationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for generating and repairing Ballerina code using an AI model.
 * This class is a Java translation of the provided JavaScript code.
 */
public class CodeGeneratorService {

    private static final int MAX_REPAIR_ATTEMPTS = 3;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mock dependencies
    private final RpcManager rpcManager = new RpcManager(); // Mock
    private final LibraryManager libraryManager = new LibraryManager(); // Mock
    private final ChatLanguageModel anthropicClient; // Mock Client

    public CodeGeneratorService(ChatLanguageModel anthropicClient) {
        this.anthropicClient = anthropicClient;
    }

    /**
     * Main public method that uses the default event handler.
     */
    public void generateCode(GenerateCodeRequest params) {
        CopilotEventHandler eventHandler = EventManager.createWebviewEventHandler(Command.CODE);
        try {
            generateCodeCore(params, eventHandler);
        } catch (Exception error) {
            System.err.println("Error during code generation: " + error.getMessage());
            eventHandler.handle(new AIEvent.Error(getErrorMessage(error)));
        }
    }

    /**
     * Core code generation function that emits events.
     * Note: The streaming logic is simplified as the original JS `streamText` behavior is not fully detailed.
     */
    public void generateCodeCore(GenerateCodeRequest params, CopilotEventHandler eventHandler) throws Exception {
        eventHandler.handle(new AIEvent.Start());

        // 1. Prepare inputs for the AI model
        ProjectSource project = rpcManager.getProjectSource(params.operationType());
        String packageName = project.projectName();
        List<SourceFiles> sourceFiles = transformProjectSource(project);
        String prompt = getRewrittenPrompt(params, sourceFiles);

        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.add(SystemMessage.from(getSystemPromptPrefix(sourceFiles, params.operationType(), GenerationType.CODE_GENERATION)));
        allMessages.add(SystemMessage.from(getSystemPromptSuffix(libraryManager.getLangLibs())));
        allMessages.addAll(populateHistory(params.chatHistory()));
        allMessages.add(UserMessage.from(getUserPrompt(prompt, sourceFiles, params.fileAttachmentContents(), packageName, params.operationType())));

        // 2. Simulate streaming call and handle responses
        // This part simulates the `for await...of` loop over the AI stream.
        try {
            // In a real scenario, a streaming client would be used with a handler.
            // For this conversion, we simulate a single response and post-processing.
            AiMessage response = anthropicClient.generate(allMessages).content();
            String assistantResponse = response.text();
            eventHandler.handle(new AIEvent.ContentBlock(assistantResponse));

            // 3. Post-process and repair loop
            PostProcessResponse postProcessedResp = rpcManager.postProcess(new PostProcessRequest(assistantResponse));
            String diagnosticFixResp = postProcessedResp.assistant_response();
            List<DiagnosticEntry> diagnostics = postProcessedResp.diagnostics().diagnostics();

            int repairAttempt = 0;
            while (hasCodeBlocks(diagnosticFixResp) && !diagnostics.isEmpty() && repairAttempt < MAX_REPAIR_ATTEMPTS) {
                System.out.printf("Repair iteration: %d%n", repairAttempt);
                System.out.println("Diagnostics to fix: " + diagnostics);

                RepairResponse repairedResponse = repairCode(
                        new RepairParams(allMessages, diagnosticFixResp, diagnostics),
                        new ArrayList<>() // Assuming libraryDetails would be extracted from the tool call
                );
                diagnosticFixResp = repairedResponse.repairResponse();
                diagnostics = repairedResponse.diagnostics();
                repairAttempt++;
            }

            System.out.println("Final Diagnostics: " + diagnostics);
            eventHandler.handle(new AIEvent.ContentReplace(diagnosticFixResp));
            eventHandler.handle(new AIEvent.Diagnostics(diagnostics));
            eventHandler.handle(new AIEvent.Messages(allMessages));
            eventHandler.handle(new AIEvent.Stop(Command.CODE));

        } catch (CancellationException e) {
            System.out.println("Finish reason: abort");
        } catch (Exception error) {
            System.err.println("Error during Code generation: " + error.getMessage());
            eventHandler.handle(new AIEvent.Error(getErrorMessage(error)));
        }
    }

    /**
     * Core repair function.
     */
    public RepairResponse repairCode(RepairParams params, List<Library> libraryDetails) throws Exception {
        List<ChatMessage> allMessages = new ArrayList<>();
        try {
            String libraryDetailsJson = objectMapper.writeValueAsString(libraryDetails);
            allMessages.add(SystemMessage.from(String.format("Library details used in the original code generation:\n<library_details>\n%s\n</library_details>", libraryDetailsJson)));
        } catch (JsonProcessingException e) {
            // Handle error or ignore
        }

        allMessages.addAll(params.previousMessages());
        allMessages.add(AiMessage.from(params.assistantResponse()));
        String errorMessages = params.diagnostics().stream().map(DiagnosticEntry::message).collect(Collectors.joining("\n"));
        allMessages.add(UserMessage.from("Generated code returns following errors. Double-check all functions, types, record field access against the provided library details. Fix the compiler errors and return the new response. \n Errors: \n " + errorMessages));

        AiMessage response = anthropicClient.generate(allMessages).content();
        String repairedText = response.text();

        String diagnosticFixResp = replaceCodeBlocks(params.assistantResponse(), repairedText);
        PostProcessResponse postProcessResp = rpcManager.postProcess(new PostProcessRequest(diagnosticFixResp));
        diagnosticFixResp = postProcessResp.assistant_response();

        System.out.println("After auto repair, Diagnostics: " + postProcessResp.diagnostics().diagnostics());
        return new RepairResponse(diagnosticFixResp, postProcessResp.diagnostics().diagnostics());
    }

    // --- Helper Methods (Translated from JS) ---

    private String getSystemPromptPrefix(List<SourceFiles> sourceFiles, OperationType op, GenerationType generationType) {
        String basePrompt = """
            # QUESTION
            Analyze the user query provided in the user message to identify the relevant Ballerina libraries needed to fulfill the query. Use the LibraryProviderTool to fetch details (name, description, clients, functions, types) for only the selected libraries. The tool description contains all available libraries and their descriptions. Do not assume library contents unless provided by the tool.

            # Example
            **Context**:
            [
              {
                "name": "ballerinax/azure.openai.chat",
                "description": "Provides a Ballerina client for the Azure OpenAI Chat API."
              },
              {
                "name": "ballerinax/github",
                "description": "Provides a Ballerina client for the GitHub API."
              },
              {
                "name": "ballerinax/slack",
                "description": "Provides a Ballerina client for the Slack API."
              },
              {
                "name": "ballerinax/http",
                "description": "Allows to interact with HTTP services."
              }
            ]
            **Query**: Write an application to read GitHub issues, summarize them, and post the summary to a Slack channel.
            **LibraryProviderTool Call**: Call LibraryProviderTool with libraryNames: ["ballerinax/github", "ballerinax/slack", "ballerinax/azure.openai.chat"]

            # Instructions
            1. Analyze the user query to determine the required functionality.
            2. Select the minimal set of libraries that can fulfill the query based on their descriptions.
            3. Call the LibraryProviderTool with the selected libraryNames and the user query to fetch detailed information (clients, functions, types).
            4. Use the tool's output to generate accurate Ballerina code.
            5. Do not include libraries unless they are explicitly needed for the query.
            %s
            You are an expert assistant specializing in Ballerina code generation.""".formatted(
                generationType == GenerationType.HEALTHCARE_GENERATION
                        ? "6. For healthcare-related queries, ALWAYS include the following libraries in the LibraryProviderTool call in addition to those selected based on the query: ballerinax/health.base, ballerinax/health.fhir.r4, ballerinax/health.fhir.r4.parser, ballerinax/health.fhir.r4utils, ballerinax/health.fhir.r4.international401, ballerinax/health.hl7v2commons, ballerinax/health.hl7v2."
                        : ""
        );

        if (op == OperationType.CODE_FOR_USER_REQUIREMENT) {
            return getRequirementAnalysisCodeGenPrefix(List.of(), extractResourceDocumentContent(sourceFiles)) + "\n" + basePrompt;
        } else if (op == OperationType.TESTS_FOR_USER_REQUIREMENT) {
            return getRequirementAnalysisTestGenPrefix(List.of(), extractResourceDocumentContent(sourceFiles)) + "\n" + basePrompt;
        }
        return basePrompt;
    }

    private String getSystemPromptSuffix(List<Library> langlibs) {
        try {
            return """
                You will be provided with default langlibs which are already imported in the Ballerina code.
                Langlibs
                <langlibs>
                %s
                </langlibs>

                If the query doesn't require code examples, answer the code by utilizing the api documentation.
                If the query requires code, Follow these steps to generate the Ballerina code:

                1. Carefully analyze the provided API documentation:
                   - Identify the available libraries, clients, their functions and their relevant types.

                2. Thoroughly read and understand the given query:
                   - Identify the main requirements and objectives of the integration.
                   - Determine which libraries, functions and their relevant records and types from the API documentation which are needed to achieve the query and forget about unused API docs.
                   - Note the libraries needed to achieve the query and plan the control flow of the application based input and output parameters of each function of the connector according to the API documentation.

                3. Plan your code structure:
                   - Decide which libraries need to be imported (Avoid importing lang.string, lang.boolean, lang.float, lang.decimal, lang.int, lang.map langlibs as they are already imported by default).
                   - Determine the necessary client initialization.
                   - Define Types needed for the query in the types.bal file.
                   - Outline the service OR main function for the query.
                   - Outline the required function usages as noted in Step 2.
                   - Based on the types of identified functions, plan the data flow. Transform data as necessary.

                4. Generate the Ballerina code:
                   - Start with the required import statements.
                   - Define required configurables for the query. Use only string, int, boolean types in configurable variables.
                   - Initialize any necessary clients with the correct configuration at the module level(before any function or service declarations).
                   - Implement the main function OR service to address the query requirements.
                   - Use defined connectors based on the query by following the API documentation.
                   - Use only the functions, types, and clients specified in the API documentation.
                   - Use dot notation to access a normal function. Use -> to access a remote function or resource function.
                   - Ensure proper error handling and type checking.
                   - Do not invoke methods on json access expressions. Always use separate statements.
                   - Use langlibs ONLY IF REQUIRED.

                5. Review and refine your code:
                   - Check that all query requirements are met.
                   - Verify that you're only using elements from the provided API documentation.
                   - Ensure the code follows Ballerina best practices and conventions.

                Provide a brief explanation of how your code addresses the query and then output your generated Ballerina code.

                Important reminders:
                - Only use the libraries, functions, types, services and clients specified in the provided API documentation.
                - Always strictly respect the types given in the API Docs.
                - Do not introduce any additional libraries or functions not mentioned in the API docs.
                - Only use specified fields in records according to the api docs. this applies to array types of that record as well.
                - Ensure your code is syntactically correct and follows Ballerina conventions.
                - Do not use dynamic listener registrations.
                - Do not write code in a way that requires updating/assigning values of function parameters.
                - ALWAYS Use two words camel case identifiers (variable, function parameter, resource function parameter and field names).
                - If the library name contains a . Always use an alias in the import statement. (import org/package.one as one;)
                - Treat generated connectors/clients inside the generated folder as submodules.
                - A submodule MUST BE imported before being used.  The import statement should only contain the package name and submodule name.  For package my_pkg, folder structure generated/fooApi the import should be `import my_pkg.fooApi;`
                - If the return parameter typedesc default value is marked as <> in the given API docs, define a custom record in the code that represents the data structure based on the use case and assign to it.
                - Whenever you have a Json variable, NEVER access or manipulate Json variables. ALWAYS define a record and convert the Json to that record and use it.
                - When invoking resource function from a client, use the correct paths with accessor and parameters. (eg: exampleClient->/path1/["param"]/path2.get(key="value"))
                - When you are accessing a field of a record, always assign it into new variable and use that variable in the next statement.
                - Avoid long comments in the code. Use // for single line comments.
                - Always use named arguments when providing values to any parameter. (eg: .get(key="value"))
                - Mention types EXPLICITLY in variable declarations and foreach statements.
                - Do not modify the README.md file unless asked to be modified explicitly in the query.
                - Do not add/modify toml files(Config.toml/Ballerina.toml) unless asked.
                - In the library API documentation if the service type is specified as generic, adhere to the instructions specified there on writing the service.
                - For GraphQL service related queries, If the user haven't specified their own GraphQL Schema, Write the proposed GraphQL schema for the user query right after explanation before generating the Ballerina code. Use same names as the GraphQL Schema when defining record types.

                Begin your response with the explanation, once the entire explanation is finished only, include codeblock segments(if any) in the end of the response.
                The explanation should explain the control flow decided in step 2, along with the selected libraries and their functions.

                Each file which needs modifications, should have a codeblock segment and it MUST have complete file content with the proposed change.
                The codeblock segments should only have .bal contents and it should not generate or modify any other file types. Politely decline if the query requests for such cases.

                Example Codeblock segment:
                <code filename="main.bal">
                ```ballerina
                //code goes here
                ```
                </code>
                """.formatted(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(langlibs));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUserPrompt(String usecase, List<SourceFiles> existingCode, List<FileAttatchment> fileUploadContents, String packageName, OperationType op) {
        String fileInstructions = "";
        if (fileUploadContents != null && !fileUploadContents.isEmpty()) {
            fileInstructions = fileUploadContents.stream()
                    .map(file -> String.format("File Name: %s\nContent: %s", file.fileName(), file.content()))
                    .collect(Collectors.joining("\n\n", "4. File Upload Contents. : Contents of the file which the user uploaded as additional information for the query.\n\n", ""));
        }

        return String.format("""
            QUERY: The query you need to answer using the provided api documentation.
            <query>
            %s
            </query>

            Existing Code: Users existing code.
            <existing_code>
            %s
            </existing_code>

            Current Package name: %s

            %s
            """, usecase, stringifyExistingCode(existingCode, op), packageName, fileInstructions);
    }

    public String stringifyExistingCode(@NotNull List<SourceFiles> existingCode, OperationType op) {
        StringBuilder sb = new StringBuilder();
        for (SourceFiles file : existingCode) {
            if (op != OperationType.CODE_GENERATION && !file.filePath().endsWith(".bal")) {
                continue;
            }
            sb.append("filepath : ").append(file.filePath()).append("\n");
            sb.append(file.content()).append("\n");
        }
        return sb.toString();
    }

    public boolean hasCodeBlocks(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        Pattern codeBlockRegex = Pattern.compile("<code[^>]*>[\\s\\S]*?<\\/code>", Pattern.CASE_INSENSITIVE);
        return codeBlockRegex.matcher(text).find();
    }

    public String replaceCodeBlocks(String originalResp, String newResp) {
        Pattern newCodeRegex = Pattern.compile("<code filename=\"(.+?)\">\\s*```ballerina\\s*([\\s\\S]*?)```\\s*<\\/code>", Pattern.DOTALL);
        Matcher newMatcher = newCodeRegex.matcher(newResp);
        Map<String, String> newCodeBlocks = new java.util.HashMap<>();
        while (newMatcher.find()) {
            newCodeBlocks.put(newMatcher.group(1), newMatcher.group(2).trim());
        }

        StringBuffer updatedResp = new StringBuffer();
        Pattern originalCodeRegex = Pattern.compile("<code filename=\"(.+?)\">\\s*```ballerina\\s*([\\s\\S]*?)```\\s*<\\/code>", Pattern.DOTALL);
        Matcher originalMatcher = originalCodeRegex.matcher(originalResp);

        while (originalMatcher.find()) {
            String filename = originalMatcher.group(1);
            String newContent = newCodeBlocks.get(filename);
            String replacement;
            if (newContent != null) {
                replacement = String.format("<code filename=\"%s\">\n```ballerina\n%s\n```\n</code>", filename, newContent);
                newCodeBlocks.remove(filename); // Remove so it's not appended later
            } else {
                replacement = originalMatcher.group(0); // Keep original
            }
            originalMatcher.appendReplacement(updatedResp, Matcher.quoteReplacement(replacement));
        }
        originalMatcher.appendTail(updatedResp);

        // Append any new code blocks that weren't replacements
        newCodeBlocks.forEach((filename, content) -> {
            updatedResp.append(String.format("\n\n<code filename=\"%s\">\n```ballerina\n%s\n```\n</code>", filename, content));
        });

        return updatedResp.toString();
    }

    // --- Mocks for external dependencies ---
    private static class RpcManager {
        public ProjectSource getProjectSource(OperationType op) { /* Mock implementation */ return new ProjectSource("my_project", List.of(new SourceFiles("main.bal", "public function main() {}"))); }
        public PostProcessResponse postProcess(PostProcessRequest req) { /* Mock implementation */ return new PostProcessResponse(req.assistant_response, new Diagnostics(new ArrayList<>())); }
    }

    private static class LibraryManager {
        public List<Library> getAllLibraries(GenerationType type) { /* Mock implementation */ return List.of(new Library("ballerinax/http", "HTTP client library.")); }
        public List<Library> getLangLibs() { /* Mock implementation */ return new ArrayList<>(); }
    }

    // Mocks for other utility functions
    private String getRewrittenPrompt(GenerateCodeRequest params, List<SourceFiles> files) { return params.usecase(); }
    private List<ChatMessage> populateHistory(List<ChatMessage> history) { return history != null ? history : new ArrayList<>(); }
    private List<SourceFiles> transformProjectSource(ProjectSource project) { return project.sourceFiles(); }
    private String getErrorMessage(Throwable error) { return error.getMessage(); }
    private String extractResourceDocumentContent(List<SourceFiles> files) { return ""; }
    private String getRequirementAnalysisCodeGenPrefix(List<Object> p1, String p2) { return ""; }
    private String getRequirementAnalysisTestGenPrefix(List<Object> p1, String p2) { return ""; }
    record PostProcessRequest(String assistant_response) {}
}


/**
 * Represents a single file in a project.
 * Corresponds to TypeScript type: `SourceFile`.
 */
record SourceFiles(String filePath, String content) {}

/**
 * Represents a file attached by the user.
 * Corresponds to TypeScript type: `FileAttatchment`.
 */
record FileAttatchment(String fileName, String content) {}

/**
 * Represents a single entry in the chat history.
 * Corresponds to TypeScript type: `ChatEntry`.
 */
record ChatEntry(String actor, String message, Boolean isCodeGeneration) {}

/**
 * Represents a compiler diagnostic or an error message.
 * Corresponds to TypeScript type: `DiagnosticEntry`.
 */
record DiagnosticEntry(Integer line, String message, String code) {}

/**
 * A container for a list of diagnostics.
 * Corresponds to TypeScript type: `ProjectDiagnostics`.
 */
record ProjectDiagnostics(List<DiagnosticEntry> diagnostics) {}

/**
 * Represents a simplified library definition.
 * Corresponds to TypeScript type: `MinifiedLibrary`.
 */
record MinifiedLibrary(String name, String description) {}


// --- Request & Response Payloads ---

/**
 * Request payload for the main code generation feature.
 * Corresponds to TypeScript type: `GenerateCodeRequest`.
 */
record GenerateCodeRequest(
        String usecase,
        List<ChatEntry> chatHistory,
        OperationType operationType,
        List<FileAttatchment> fileAttachmentContents
) {}

/**
 * Request payload for the code repair feature.
 * Note: `previousMessages` uses LangChain4j's `ChatMessage` for type safety.
 * Corresponds to TypeScript type: `RepairParams`.
 */
record RepairParams(
        List<ChatMessage> previousMessages,
        String assistantResponse,
        List<DiagnosticEntry> diagnostics
) {}

/**
 * Response from the code repair feature.
 * Corresponds to TypeScript type: `RepairResponse`.
 */
record RepairResponse(String repairResponse, List<DiagnosticEntry> diagnostics) {}

/**
 * Response from the post-processing step after AI generation.
 * Corresponds to TypeScript type: `PostProcessResponse`.
 */
record PostProcessResponse(String assistant_response, ProjectDiagnostics diagnostics) {}


// --- Enums ---

/**
 * Defines the type of code generation operation.
 * Corresponds to TypeScript type: `OperationType`.
 */
enum OperationType {
    CODE_GENERATION,
    CODE_FOR_USER_REQUIREMENT,
    TESTS_FOR_USER_REQUIREMENT
}

/**
 * Defines the command context for the AI panel.
 * Corresponds to TypeScript type: `Command`.
 */
enum Command {
    CODE
    // Other commands can be added here
}

/**
 * Represents a module within a Ballerina project.
 * Corresponds to TypeScript type: `ProjectModule`.
 */
record ProjectModule(
        String moduleName,
        List<SourceFiles> sourceFiles,
        boolean isGenerated
) {}

/**
 * Represents the entire source code and structure of a Ballerina project.
 * Corresponds to TypeScript type: `ProjectSource`.
 * Note: Optional fields in TypeScript are represented here as fields that can be null.
 */
record ProjectSource(
        List<ProjectModule> projectModules,
        List<SourceFiles> projectTests,
        List<SourceFiles> sourceFiles,
        String projectName
) {}

