package com.combostrap.analyics.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/")
@Tag(name = "Health Check", description = "API health check endpoint")
public class PingResource {


    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Health check",
            description = "Returns pong to verify the API is running"
    )
    @ApiResponse(
            responseCode = "200",
            description = "API is healthy",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(type = "string", example = "pong")
            )
    )
    public Response ping() {

        return Response.ok("OK").build();

    }

}
