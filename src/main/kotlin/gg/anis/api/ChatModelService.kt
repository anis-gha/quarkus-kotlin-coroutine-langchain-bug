package gg.anis.api

import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@RegisterAiService(chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier::class)
interface ChatModelService {

    @UserMessage("Where is located Paris ?")
    fun request(message: String): String

}