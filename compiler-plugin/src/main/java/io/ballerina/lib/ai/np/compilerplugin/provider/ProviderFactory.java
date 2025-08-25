package io.ballerina.lib.ai.np.compilerplugin.provider;

import io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider.AnthropicModelProvider;
import io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider.AzureOpenAIModelProvider;
import io.ballerina.lib.ai.np.compilerplugin.provider.modelprovider.OpenAIModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProviderFactory {

    private static final String AZURE_TOKEN_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_TOKEN";
    private static final String AZURE_DEPLOYMENT_ID_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_DEPLOYEMENT_ID";
    private static final String AZURE_API_VERSION_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_API_VERSION";
    private static final String AZURE_SERVICE_URL_ENV_VAR = "BAL_CODEGEN_AZURE_OPENAI_SERVICE_URL";
    private static final String ANTHROPIC_TOKEN_ENV_VAR = "BAL_CODEGEN_ANTHROPIC_TOKEN";
    private static final String OPENAI_TOKEN_ENV_VAR = "BAL_CODEGEN_OPENAI_TOKEN";
    private static final String BAL_CODEGEN_URL = "BAL_CODEGEN_URL";
    private static final String BAL_CODEGEN_TOKEN = "BAL_CODEGEN_TOKEN";

    private static Provider provider = null;

    public static Provider getProviderInstance() {
        if (provider != null) {
            return provider;
        }

        Optional<Provider> providerOpt = ProviderFactory.createModelFromEnvironment();
        if (providerOpt.isEmpty()) {
            throw new RuntimeException("Failed to create a provider for code generation. " +
                    "Ensure that the environment are set properly.");
        }

        return providerOpt.get();
    }

    private static Optional<Provider> createModelFromEnvironment() {
        String anthropicToken = System.getenv(ANTHROPIC_TOKEN_ENV_VAR);
        if (isNotNullOrEmpty(anthropicToken)) {
            return Optional.of(new AnthropicModelProvider(anthropicToken));
        }

        String azureToken = System.getenv(AZURE_TOKEN_ENV_VAR);
        String azureDeploymentId = System.getenv(AZURE_DEPLOYMENT_ID_ENV_VAR);
        String azureApiVersion = System.getenv(AZURE_API_VERSION_ENV_VAR);
        String azureServiceUrl = System.getenv(AZURE_SERVICE_URL_ENV_VAR);

        if (isNotNullOrEmpty(azureToken) || isNotNullOrEmpty(azureDeploymentId) ||
                isNotNullOrEmpty(azureApiVersion) || isNotNullOrEmpty(azureServiceUrl)) {
            List<String> missingVars = new ArrayList<>();
            if (!isNotNullOrEmpty(azureToken)) {
                missingVars.add(AZURE_TOKEN_ENV_VAR);
            }
            if (!isNotNullOrEmpty(azureDeploymentId)) {
                missingVars.add(AZURE_DEPLOYMENT_ID_ENV_VAR);
            }
            if (!isNotNullOrEmpty(azureApiVersion)) {
                missingVars.add(AZURE_API_VERSION_ENV_VAR);
            }
            if (!isNotNullOrEmpty(azureServiceUrl)) {
                missingVars.add(AZURE_SERVICE_URL_ENV_VAR);
            }
            if (missingVars.isEmpty()) {
                return Optional.of(new AzureOpenAIModelProvider(
                        azureToken, azureDeploymentId, azureServiceUrl, azureApiVersion));
            }

            throw new IllegalStateException("Azure OpenAI configuration is incomplete. " +
                    "The following required environment variables are missing: "
                    + String.join(", ", missingVars));
        }

        String openAiToken = System.getenv(OPENAI_TOKEN_ENV_VAR);
        if (isNotNullOrEmpty(openAiToken)) {
            return Optional.of(new OpenAIModelProvider(openAiToken));
        }

        String balCodegenUrl = System.getenv(BAL_CODEGEN_URL);
        String balCodegenToken = System.getenv(BAL_CODEGEN_TOKEN);
        if (isNotNullOrEmpty(balCodegenUrl) || isNotNullOrEmpty(balCodegenToken)) {
            List<String> missingVars = new ArrayList<>();
            if (!isNotNullOrEmpty(balCodegenUrl)) {
                missingVars.add(BAL_CODEGEN_URL);
            }
            if (!isNotNullOrEmpty(balCodegenToken)) {
                missingVars.add(BAL_CODEGEN_TOKEN);
            }
            if (!missingVars.isEmpty()) {
                throw new IllegalStateException("Ballerina Copilot configuration is incomplete. " +
                        "The following required environment variables are missing: "
                        + String.join(", ", missingVars));
            }

            return Optional.of(new BallerinaCopilotServiceProvider(balCodegenUrl, balCodegenToken));
        }

        return Optional.empty();
    }

    private static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
