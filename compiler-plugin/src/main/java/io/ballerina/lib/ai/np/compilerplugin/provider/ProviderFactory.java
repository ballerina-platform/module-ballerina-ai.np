package io.ballerina.lib.ai.np.compilerplugin.provider;

import io.ballerina.projects.ProjectException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProviderFactory {

    private static final String AZURE_TOKEN_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_TOKEN";
    private static final String AZURE_DEPLOYMENT_ID_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_DEPLOYEMENT_ID";
    private static final String AZURE_API_VERSION_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_API_VERSION";
    private static final String AZURE_SERVICE_URL_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_SERVICE_URL";
    private static final String ANTHROPIC_TOKEN_ENV_VAR = "BAL_CODEGEN_ANTHROPIC_TOKEN";
    private static final String ANTHROPIC_SERVICE_URL_ENV_VAR = "BAL_CODEGEN_ANTHROPIC_SERVICE_URL";
    private static final String OPENAI_TOKEN_ENV_VAR = "BAL_CODEGEN_OPENAI_TOKEN";
    private static final String OPENAI_SERVICE_URL_ENV_VAR = "BAL_CODEGEN_OPENAI_SERVICE_URL";
    private static final String BAL_CODEGEN_URL = "BAL_CODEGEN_URL";
    private static final String BAL_CODEGEN_TOKEN = "BAL_CODEGEN_TOKEN";

    public static CodeGenerator getProviderInstance() {
        Optional<CodeGenerator> providerOpt = ProviderFactory.createModelFromEnvironment();
        if (providerOpt.isEmpty()) {
            throw new RuntimeException("Failed to create a provider for code generation. " +
                    "Ensure that environment variables for exactly one provider are set.");
        }
        return providerOpt.get();
    }

    private static Optional<CodeGenerator> createModelFromEnvironment() {
        List<CodeGenerator> availableProviders = Stream.of(
                        createAnthropicProvider(),
                        createAzureOpenAiProvider(),
                        createOpenAiProvider(),
                        createBallerinaCopilotProvider()
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (availableProviders.isEmpty()) {
            return Optional.empty();
        }

        if (availableProviders.size() > 1) {
            String configuredProviderNames = availableProviders.stream()
                    .map(CodeGenerator::getName)
                    .collect(Collectors.joining(", "));
            throw new ProjectException("Multiple AI model providers are configured. " +
                    "Please set environment variables for only one provider. Found configurations for: "
                    + configuredProviderNames);
        }

        return Optional.of(availableProviders.get(0));
    }

    private static Optional<CodeGenerator> createAnthropicProvider() {
        String anthropicToken = System.getenv(ANTHROPIC_TOKEN_ENV_VAR);
        if (isNotNullOrEmpty(anthropicToken)) {
            String anthropicServiceUrl = System.getenv(ANTHROPIC_SERVICE_URL_ENV_VAR);
            if (!isNotNullOrEmpty(anthropicServiceUrl)) {
                return Optional.of(new AnthropicModelProvider(anthropicToken));
            }
            return Optional.of(new AnthropicModelProvider(anthropicToken, anthropicServiceUrl));
        }
        return Optional.empty();
    }

    private static Optional<CodeGenerator> createAzureOpenAiProvider() {
        String token = System.getenv(AZURE_TOKEN_ENV_VAR);
        String deploymentId = System.getenv(AZURE_DEPLOYMENT_ID_ENV_VAR);
        String apiVersion = System.getenv(AZURE_API_VERSION_ENV_VAR);
        String serviceUrl = System.getenv(AZURE_SERVICE_URL_ENV_VAR);

        if (!isNotNullOrEmpty(token) && !isNotNullOrEmpty(deploymentId) &&
                !isNotNullOrEmpty(apiVersion) && !isNotNullOrEmpty(serviceUrl)) {
            return Optional.empty();
        }

        List<String> missingVars = new ArrayList<>();
        if (!isNotNullOrEmpty(token)) {
            missingVars.add(AZURE_TOKEN_ENV_VAR);
        }
        if (!isNotNullOrEmpty(deploymentId)) {
            missingVars.add(AZURE_DEPLOYMENT_ID_ENV_VAR);
        }
        if (!isNotNullOrEmpty(apiVersion)) {
            missingVars.add(AZURE_API_VERSION_ENV_VAR);
        }
        if (!isNotNullOrEmpty(serviceUrl)) {
            missingVars.add(AZURE_SERVICE_URL_ENV_VAR);
        }

        if (!missingVars.isEmpty()) {
            throw new IllegalStateException("Azure OpenAI configuration is incomplete. " +
                    "The following required environment variables are missing: "
                    + String.join(", ", missingVars));
        }
        return Optional.of(new AzureOpenAIModelProvider(token, deploymentId, serviceUrl, apiVersion));
    }

    private static Optional<CodeGenerator> createOpenAiProvider() {
        String openAiToken = System.getenv(OPENAI_TOKEN_ENV_VAR);
        if (isNotNullOrEmpty(openAiToken)) {
            String openAiServiceUrl = System.getenv(OPENAI_SERVICE_URL_ENV_VAR);
            if (!isNotNullOrEmpty(openAiServiceUrl)) {
                return Optional.of(new OpenAIModelProvider(openAiToken));
            }
            return Optional.of(new OpenAIModelProvider(openAiToken, openAiServiceUrl));
        }
        return Optional.empty();
    }

    private static Optional<CodeGenerator> createBallerinaCopilotProvider() {
        String url = System.getenv(BAL_CODEGEN_URL);
        String token = System.getenv(BAL_CODEGEN_TOKEN);

        if (!isNotNullOrEmpty(url) && !isNotNullOrEmpty(token)) {
            return Optional.empty();
        }

        List<String> missingVars = new ArrayList<>();
        if (!isNotNullOrEmpty(url)) {
            missingVars.add(BAL_CODEGEN_URL);
        }
        if (!isNotNullOrEmpty(token)) {
            missingVars.add(BAL_CODEGEN_TOKEN);
        }

        if (!missingVars.isEmpty()) {
            throw new IllegalStateException("Ballerina Copilot configuration is incomplete. " +
                    "The following required environment variables are missing: "
                    + String.join(", ", missingVars));
        }
        return Optional.of(new BallerinaCopilotServiceProvider(url, token));
    }

    private static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
