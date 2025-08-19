package io.ballerina.lib.ai.np.compilerplugin.code.agent.tools;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services.LibraryService;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.services.UtilsService;
import io.ballerina.lib.ai.np.compilerplugin.code.agent.tools.typedef.GenerationType;

/**
 * Factory class for creating instances of LibraryProviderTool.
 */
public class LibraryProviderToolFactory {

    public static LibraryProviderTool create(String libraryDescriptions,
                                             GenerationType generationType,
                                             ChatLanguageModel chatModel,
                                             LangClient langClient) {
        UtilsService utilsService = new UtilsService();
        LibraryService libraryService = new LibraryService(chatModel, langClient, utilsService);

        return new LibraryProviderTool(libraryDescriptions, generationType, libraryService);
    }
}
