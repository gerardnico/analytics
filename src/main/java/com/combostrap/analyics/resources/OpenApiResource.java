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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Path("/")
@Tag(name = "API Documentation", description = "OpenAPI specification endpoint")
public class OpenApiResource {

    @GET
    @Path("openapi")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get OpenAPI specification",
            description = "Returns the OpenAPI 3.0 specification for this API in JSON format"
    )
    @ApiResponse(
            responseCode = "200",
            description = "OpenAPI specification",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "object")
            )
    )
    public Response getOpenApiJson() {
        String openApiJson = loadOpenApiJson();
        return Response.ok(openApiJson).build();
    }

    @GET
    @Path("openapi.yaml")
    @Produces("application/yaml")
    @Operation(
            summary = "Get OpenAPI specification in YAML",
            description = "Returns the OpenAPI 3.0 specification for this API in YAML format"
    )
    @ApiResponse(
            responseCode = "200",
            description = "OpenAPI specification in YAML",
            content = @Content(
                    mediaType = "application/yaml"
            )
    )
    public Response getOpenApiYaml() {
        String openApiYaml = loadOpenApiYaml();
        return Response.ok(openApiYaml).build();
    }

    private static volatile String cachedYaml;
    private static volatile String cachedJson;

    public static String loadOpenApiYaml() {
        if (cachedYaml == null) {
            synchronized (OpenApiResource.class) {
                if (cachedYaml == null) {
                    cachedYaml = loadResource("/openapi/openapi.yaml");
                }
            }
        }
        return cachedYaml;
    }

    public static String loadOpenApiJson() {
        if (cachedJson == null) {
            synchronized (OpenApiResource.class) {
                if (cachedJson == null) {
                    cachedJson = loadResource("/openapi/openapi.json");
                }
            }
        }
        return cachedJson;
    }

    private static String loadResource(String resourcePath) {
        try (InputStream inputStream = OpenApiResource.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
