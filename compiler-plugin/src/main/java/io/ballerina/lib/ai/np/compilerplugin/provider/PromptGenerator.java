package io.ballerina.lib.ai.np.compilerplugin.provider;

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

import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.CONTENT;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.FILE_PATH;
import static io.ballerina.lib.ai.np.compilerplugin.provider.ProviderUtils.retrieveLangLibs;

public class PromptGenerator {
    private static final String LANG_LIBS_PATH = "/langlibs.json";
    private static final String LANG_LIBS;
    static {
        try {
            LANG_LIBS = retrieveLangLibs(LANG_LIBS_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSystemPromptForFunction() {
        return """
            Langlibs
            <langlibs>
               %s
            </langlibs>
            
            You are a Ballerina code generation assistant specialized in generating Ballerina code based on natural 
            language instructions. Your task is to generate function implementations that 
            satisfy the requirements specified in natural language.
            
            Follow these guidelines when generating code:
            
            1. Function Implementation:
               - Generate ONLY the function implementation with the exact signature as the original function
               - Include ONLY imports only from the `ballerina` and `ballerinax` organizations
               - Use only the parameters of the function. Do not use configurable 
                 variables or module-level variables from the program
            
            2. Code Standards:
               - Use two words camel case identifiers (variables, parameters, field names)
               - Use dot notation for normal functions, -> for remote/resource functions
               - Use named arguments when providing values to parameters (e.g., .get(key="value"))
               - Mention types EXPLICITLY in variable declarations and foreach statements
               - Use // for single line comments, avoid long comments
               - Ensure proper error handling
            
            3. Data Handling:
               - Do not invoke methods on json access expressions - use separate statements
               - When working with Json variables, define records and convert Json to records
               - When accessing record fields, assign to new variables before using in next statement
               - Use langlibs ONLY IF REQUIRED
            
            4. Import Rules:
               - Do not import lang.string, lang.boolean, lang.float, lang.decimal, lang.int, lang.map 
                 as they are imported by default
               - If library name contains dots, use aliases (import org/package.one as one;)
            
            5. Response Format:
               - Respond with ONLY the generated function and required imports
               - No explanations, comments, or additional text
               - Single code block containing the complete implementation
            
            Important: Generate syntactically correct Ballerina code that follows all conventions 
            and best practices.
            """.formatted(LANG_LIBS);
    }

    public static String getSystemPromptForExpression() {
        return """
            Langlibs
            <langlibs>
               %s
            </langlibs>
            
            You are a Ballerina code generation assistant specialized in creating value expressions 
            using only Ballerina literals and constructor expressions.
            
            Your task is to generate self-contained value expressions that:
            1. Use ONLY Ballerina literals and constructor expressions
            2. Do NOT contain any variable references, function calls, or external dependencies
            3. Match the specified expected type exactly
            4. Are syntactically correct and follow Ballerina conventions (camelCase for field names)
            
            Available Ballerina literals:
            1. nil-literal: () | null
            2. boolean-literal: true | false
            3. numeric-literal: int, float, and decimal values (e.g., 1, 2.0, 3f, 4.5d)
            4. string-literal: double quoted strings (e.g., "foo") or string-template literal 
            without interpolations (e.g., string `foo`)
            
            Available Ballerina constructor expressions:
            1. List constructor expression: [1, 2, 3]
            2. Mapping constructor expression: {a: 1, b: 2, "c": 3}
            3. Table constructor expression: table [{a: 1, b: 2}, {a: 2, b: 4}]
            
            Rules:
            - The expression must be completely self-contained
            - No external dependencies or imports needed
            - Use langlibs ONLY if absolutely necessary for type construction
            
            Response format:
            - Provide ONLY the value expression
            - Wrap the response in ```ballerina code blocks
            - Do not include any explanations or comments
            - The expression should be ready to use directly in place of the const natural expression
            """.formatted(LANG_LIBS);
    }

    public static String getUserPrompt(String useCase, JsonArray existingCode) {
        return """
                QUERY: The query you need to answer using the provided existing code and langlibs.
                <query>
                %s
                </query>

                Existing Code: User's existing code.
                <existing_code>
                %s
                </existing_code>
                """.formatted(
                useCase,
                stringifyExistingCode(existingCode)
        );
    }

    public static String generateUseCasePromptForFunctions(
            String originalFuncName, String generatedFuncName, String prompt, JsonArray sourceFiles) {
        return String.format("""
                    An `external` function with the `@natural:code` Ballerina annotation needs to be replaced at
                    compile-time with the code necessary to achieve the requirement specified as the `prompt`
                    field in the annotation.
                    
                    Original function: %s
                    Generated function name: %s
                    
                    Requirement:
                    ```
                    %s
                    ```
                    
                    Existing code context:
                    ```ballerina
                    %s
                    ```
                    
                    Generate a function named '%s' that:
                    - Has exactly the same signature as the '%s' function
                    - Implements the functionality described in the requirement
                    
                    Respond with ONLY the generated function implementation and any required imports 
                    wrapped in ```ballerina code blocks.
                    """,
                originalFuncName, generatedFuncName, prompt,
                stringifyExistingCode(sourceFiles), generatedFuncName, originalFuncName);
    }

    public static String generateUseCasePromptForNaturalFunctions(NaturalExpressionNode naturalExpressionNode,
                                  TypeSymbol expectedType, SemanticModel semanticModel) {
        NodeList<Node> userPromptContent = naturalExpressionNode.prompt();
        StringBuilder sb = new StringBuilder(String.format("""
            Generate a self-contained value expression that satisfies the following requirement.
            
            Target type: %s
            
            Respond with ONLY the value expression wrapped in ```ballerina code blocks.
            
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
