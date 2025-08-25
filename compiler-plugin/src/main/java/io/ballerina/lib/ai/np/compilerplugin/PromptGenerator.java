package io.ballerina.lib.ai.np.compilerplugin;

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.InterpolationNode;
import io.ballerina.compiler.syntax.tree.LiteralValueToken;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;

import java.io.IOException;

import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.CONTENT;
import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.FILE_PATH;
import static io.ballerina.lib.ai.np.compilerplugin.CommonUtils.retrieveLangLibs;

public class PromptGenerator {
    public static final String LANG_LIBS_PATH = "/langlibs.json";
    private static final String LANG_LIBS;
    static {
        try {
            LANG_LIBS = retrieveLangLibs(LANG_LIBS_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSystemPromptPrefix() {
        return """
                You are an expert assistant who specializes in writing Ballerina code.
                Your goal is to ONLY answer Ballerina related queries. You should always answer with
                    accurate and functional Ballerina code that addresses the specified query while adhering
                    to the constraints.
                You will be provided with following inputs:
           """;
    }

    public static String getSystemPromptSuffix() {
        return """
                Langlibs
                <langlibs>
                   %s
                </langlibs>
                                
                If the query requires code, Follow these steps to generate the Ballerina code:
                                
                1. Carefully analyze the provided sources:
                                
                2. Thoroughly read and understand the given query:
                   - Identify the main requirements and objectives of the integration.
                   - Determine which libraries, functions and their relavant records and types from the API
                   documentation which are needed to achieve the query and forget about unused API docs.
                   - Note the libraries needed to achieve the query and plan the control flow of the applicaiton based
                   input and output parameters of each function of the connector according to the API documentation.
                                
                3. Plan your code structure:
                   - Decide which libraries need to be imported (Avoid importing lang.string, lang.boolean,
                   lang.float, lang.decimal, lang.int, lang.map langlibs as they are already imported by default).
                   - Determine the necessary client initialization.
                   - Define Types needed for the query in the types.bal file.
                   - Outline the service OR main function for the query.
                   - Outline the required function usages as noted in Step 2.
                   - Based on the types of identified functions, plan the data flow. Transform data as necessary.
                                
                4. Generate the Ballerina code:
                   - Start with the required import statements.
                   - Define required configurables for the query. Use only string, int, boolean types in
                   configurable variables.
                   - Initialize any necessary clients with the correct configuration at the module level
                   (before any function or service declarations).
                   - Implement the main function OR service to address the query requirements.
                   - Use defined connectors based on the query by following the API documentation.
                   - Use only the functions, types, and clients specified in the API documentation.
                   - Use dot donation to access a normal function. Use -> to access a remote function
                   or resource function.
                   - Ensure proper error handling and type checking.
                   - Do not invoke methods on json access expressions. Always Use seperate statements.
                   - Use langlibs ONLY IF REQUIRED.
                                
                5. Review and refine your code:
                   - Check that all query requirements are met.
                   - Verify that you're only using elements from the provided API documentation.
                   - Ensure the code follows Ballerina best practices and conventions.
                                
                Provide a brief explanation of how your code addresses the query and then output your
                generated ballerina code.
                                
                Important reminders:
                - Ensure your code is syntactically correct and follows Ballerina conventions.
                - Do not use dynamic listener registrations.
                - Do not write code in a way that requires updating/assigning values of function parameters.
                - ALWAYS Use two words camel case identifiers (variable, function parameter, resource
                function parameter and field names).
                - If the library name contains a . Always use an alias in the import statement.
                (import org/package.one as one;)
                - Treat generated connectors/clients inside the generated folder as submodules.
                - A submodule MUST BE imported before being used.  The import statement should only contain the
                package name and submodule name.  For package my_pkg, folder strucutre generated/fooApi the import
                should be \\`import my_pkg.fooApi;\\`
                - If the return parameter typedesc default value is marked as <> in the given API docs, define a
                custom record in the code that represents the data structure based on the use case and assign to it.
                - Whenever you have a Json variable, NEVER access or manipulate Json variables. ALWAYS define a
                record and convert the Json to that record and use it.
                - When invoking resource function from a client, use the correct paths with accessor and paramters.
                (eg: exampleClient->/path1/["param"]/path2.get(key="value"))
                - When you are accessing a field of a record, always assign it into new variable and use that
                variable in the next statement.
                - Avoid long comments in the code. Use // for single line comments.
                - Always use named arguments when providing values to any parameter. (eg: .get(key="value"))
                - Mention types EXPLICITLY in variable declarations and foreach statements.
                - Do not modify the README.md file unless asked to be modified explicitly in the query.
                - Do not add/modify toml files(Config.toml/Ballerina.toml) unless asked.
                - In the library API documentation if the service type is specified as generic, adhere to the
                instructions specified there on writing the service.
                - For GraphQL service related queries, If the user haven't specified their own GraphQL Scehma,
                Write the proposed GraphQL schema for the user query right after explanation before generating the
                ballerina code. Use same names as the GraphQL Schema when defining record types.
                                
                Begin your response with the explanation, once the entire explanation is finished only, include
                codeblock segments(if any) in the end of the response.
                The explanation should explain the control flow decided in step 2, along with the selected
                libraries and their functions.
                                
                Each file which needs modifications, should have a codeblock segment and it MUST have complete
                file content with the proposed change.
                The codeblock segments should only have .bal contents and it should not generate or modify any
                other file types. Politely decline if the query requests for such cases.
                                
                Example Codeblock segment:
                <code filename="main.bal">
                \\`\\`\\`ballerina
                //code goes here
                \\`\\`\\`
                </code>
                """.formatted(LANG_LIBS);
    }

    public static String getUserPrompt(
            String useCase,
            JsonArray existingCode
    ) {
        return """
                QUERY: The query you need to answer using the provided api documentation.
                <query>
                %s
                </query>

                Existing Code: Users existing code.
                <existing_code>
                %s
                </existing_code>
                """.formatted(
                useCase,
                stringifyExistingCode(existingCode)
        );
    }

    public static String generateUseCasePromptForFunctions(
            String originalFuncName, String generatedFuncName, String prompt) {
        return String.format("""
                        An `external` function with the `@natural:code` Ballerina annotation needs to be replaced at
                        compile-time with the code necessary to achieve the requirement specified as the `prompt`
                        field in the annotation.
                        
                        As a skilled Ballerina programmer, you have to generate the code to do this for the %s function.
                        The following prompt defines the requirement:
                        
                        ```
                        %s
                        ```
                        
                        Your task is to generate a function named '%s' with the code that is needed to satisfy this user
                        prompt.
                        
                        The '%s' function should have exactly the same signature as the '%s' function.
                        Use only the parameters passed to the function and module-level clients that are clients \
                        from the ballerina and ballerinax module in the generated code.
                        Do not use any configurable variables or module-level variables defined in the program.
                        
                        Respond with ONLY THE GENERATED FUNCTION AND ANY IMPORTS REQUIRED BY THE GENERATED FUNCTION.
                        """,
                originalFuncName, prompt, generatedFuncName, generatedFuncName, originalFuncName);
    }

    public static String generateUseCasePromptForNaturalFunctions(NaturalExpressionNode naturalExpressionNode,
                                                              TypeSymbol expectedType, SemanticModel semanticModel) {
        NodeList<Node> userPromptContent = naturalExpressionNode.prompt();
        StringBuilder sb = new StringBuilder(String.format("""
                Generate a value expression to satisfy the following requirement using only Ballerina literals and
                constructor expressions. The expression should be self-contained and should not have references.
                
                Ballerina literals:
                1. nil-literal :=  () | null
                2. boolean-literal := true | false
                3. numeric-literal - int, float, and decimal values (e.g., 1, 2.0, 3f, 4.5d)
                4. string-literal - double quoted strings (e.g., "foo") or
                    string-template literal without interpolations (e.g., string `foo`)
                
                Ballerina constructor expressions:
                1. List constructor expression - e.g., [1, 2]
                2. Mapping constructor expression - e.g., {a: 1, b: 2, "c": 3}
                3. Table constructor expression - e.g., table [{a: 1, b: 2}, {a: 2, b: 4}]
                
                The value should belong to the type '%s'. This value will be used in the code in place of the
                `const natural {...}` expression with the requirement.

                Respond with ONLY THE VALUE EXPRESSION within ```ballerina and ```.
                
                Requirement:
                """, expectedType.signature()));

        for (int i = 0; i < userPromptContent.size(); i++) {
            Node node = userPromptContent.get(i);
            if (node instanceof LiteralValueToken literalValueToken) {
                sb.append(literalValueToken.text());
                continue;
            }

            Symbol symbol = semanticModel.symbol(((InterpolationNode) node).expression()).get();
            if (symbol instanceof ConstantSymbol constantSymbol) {
                sb.append(constantSymbol.resolvedValue().get());
            }
        }
        return sb.toString();
    }

    private static String stringifyExistingCode(JsonArray sourceFiles) {
        StringBuilder sb = new StringBuilder();
        sourceFiles.forEach(
                file -> {
                    if (file.isJsonObject()) {
                        sb.append("**").append(file.getAsJsonObject().get(FILE_PATH).getAsString()).append("**\n");
                        sb.append(file.getAsJsonObject().get(CONTENT).getAsString()).append("\n");
                    }
                }
        );
        return sb.toString();
    }

    private static String constructDiagnosticMessages(JsonArray diagnostics) {
        StringBuilder sb = new StringBuilder();
        diagnostics.forEach(
                diagnostic -> {
                    if (diagnostic.isJsonObject()) {
                        String message = diagnostic.getAsJsonObject().get("message").getAsString();
                        sb.append(message).append("\n");
                    }
                }
        );
        return sb.toString();
    }

    public static String getRepairPromptForFunctions(String generatedFuncName, JsonArray diagnostics) {
        return """
                Fix following issues in the generated '%s' function.
                Do not change anything other than the function body.
                Errors: %s
                """.formatted(generatedFuncName, constructDiagnosticMessages(diagnostics));
    }

    public static String getRepairPromptForNaturalExpressions(JsonArray diagnostics) {
        return """
                "The generated expression results in the following errors. " +
                "Fix the errors and return a new constant expression.
                Errors: %s"
                """.formatted(constructDiagnosticMessages(diagnostics));
    }
}
