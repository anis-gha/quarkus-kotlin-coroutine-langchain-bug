package gg.anis.api

import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class ScanTextService(
    private val chatModelService: ChatModelService
) : ScanService {

    private val logger = Logger.getLogger(this::class.java)

    override suspend fun processScan(scan: Scan) {
        val result = chatModelService.request("Hello")

        logger.info("Scan ${scan.id} processed, result: $result")
    }

    override fun canHandle(scan: Scan) = true
}