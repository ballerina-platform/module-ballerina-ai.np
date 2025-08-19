package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

/**
 * Enum representing the category of a link, either internal or external.
 */
public enum Category {
    INTERNAL("internal"),
    EXTERNAL("external");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
