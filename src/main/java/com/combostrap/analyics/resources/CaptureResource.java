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
@Tag(name = "Capture", description = "API capture endpoint")
public class CaptureResource {


    @GET
    @Path("capture")
    @Operation(
            summary = "Capture",
            description = "Returns nothing"
    )
    @ApiResponse(
            responseCode = "200",
            description = "In any case"
    )
    public Response capture() {

        return Response.ok("TODO").build();

    }

}
