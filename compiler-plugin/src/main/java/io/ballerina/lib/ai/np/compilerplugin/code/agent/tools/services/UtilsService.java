package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services;

import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.GenerationType;

/**
 * Utility service for common operations, such as mapping generation modes.
 */
public class UtilsService {

    public String getGenerationMode(GenerationType generationType) {
        switch (generationType) {
            case CODE_GENERATION:
                return "CODE_GENERATION";
            case HEALTHCARE_GENERATION:
                return "HEALTHCARE_GENERATION";
            default:
                return "CODE_GENERATION";
        }
    }
}
