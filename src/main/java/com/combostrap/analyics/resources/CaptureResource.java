package com.combostrap.analyics.resources;

import com.combostrap.analyics.capture.CaptureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/")
@Tag(name = "Capture", description = "API capture endpoint")
public class CaptureResource {


    @POST
    @Path("capture")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Capture",
            description = "Ingests a PostHog-compatible event. The event is enqueued and a 200 "
                    + "is returned immediately (fire-and-forget). The request is rate limited."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Event accepted for ingestion",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string", example = "{\"status\":1}"))),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Response capture(String body, @Context HttpHeaders headers) {

        CaptureService.Result result = CaptureService.instance().handle(body, clientKey(headers));
        return Response.status(result.status()).entity(result.body()).build();

    }

    /**
     * Best-effort caller identity for rate limiting: prefer the proxy-forwarded
     * client IP, falling back to a constant so a missing header still limits.
     */
    private static String clientKey(HttpHeaders headers) {
        String forwarded = headers.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; the first is the client.
            return forwarded.split(",")[0].trim();
        }
        String realIp = headers.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return "unknown";
    }

}
