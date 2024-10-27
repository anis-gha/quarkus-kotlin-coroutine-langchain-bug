package gg.anis.api

import io.quarkus.runtime.Shutdown
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

const val MAX_CONCURRENT_SCANS = 5

@ApplicationScoped
class ApiService(
    private val scanServices: Instance<ScanService>
) : CoroutineScope {

    private val logger = Logger.getLogger(this::class.java)
    private val job = SupervisorJob()
    private val dispatcher = Dispatchers.Default
    override val coroutineContext: CoroutineContext
        get() = dispatcher + job

    private val scanChannel = Channel<Scan>(Channel.UNLIMITED)
    private val activeScans = ConcurrentHashMap.newKeySet<UUID>()

    @Startup
    open fun startup() {
        launch {
            processQueue()
        }
    }

    @Shutdown
    open fun shutdown() {
        scanChannel.close()
        job.cancel()
    }

    fun queueScan(scan: Scan) {
        logger.info("Queuing scan ${scan.id}")

        launch {
            scanChannel.send(scan)
        }
    }

    open suspend fun processQueue() {
        for (queuedScan in scanChannel) {
            while (activeScans.size >= MAX_CONCURRENT_SCANS) {
                delay(100)
            }

            launch {
                try {
                    activeScans.add(queuedScan.id)
                    processScan(queuedScan)
                } catch (e: Exception) {
                    logger.error("Error while processing scan ${queuedScan.id}", e)
                } finally {
                    activeScans.remove(queuedScan.id)
                }
            }
        }
    }

    open suspend fun processScan(queuedScan: Scan) = coroutineScope {
        logger.info("Processing scan ${queuedScan.id}")

        try {
            val service = scanServices.firstOrNull { it.canHandle(queuedScan) }
                ?: throw Exception("No suitable service found for scan ${queuedScan.id}")

            val result = withTimeout(1.minutes.inWholeMilliseconds) {
                service.processScan(queuedScan)
            }

            logger.info("Scan ${queuedScan.id} processed, result: $result")
        } catch (_: TimeoutCancellationException) {
            logger.warn("Timeout while processing scan ${queuedScan.id}")
        } catch (e: Exception) {
            logger.error("Error while processing scan ${queuedScan.id}", e)
        }
    }
}

interface ScanService {
    suspend fun processScan(scan: Scan)
    fun canHandle(scan: Scan): Boolean
}

data class Scan(
    val id: UUID
)