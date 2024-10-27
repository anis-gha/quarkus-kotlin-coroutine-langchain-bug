package gg.anis.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import java.util.UUID

@Path("/api")
class ApiResource(
    private val apiService: ApiService
) {

    @Path("/")
    @GET
    fun queue(): Response {
        apiService.queueScan(Scan(UUID.randomUUID()))
        return Response.ok("Scan queued").build()
    }
}