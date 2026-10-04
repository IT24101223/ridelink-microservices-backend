package com.ridelink.ride.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures springdoc-openapi to add a global bearer-auth security scheme,
 * so Swagger UI shows an "Authorize" button for JWT tokens.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideLinkOpenApi() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink – Ride Management Service")
                        .version("1.0.0")
                        .description("""
                                Service #3 of 4 in the RideLink microservices platform.
                                Manages the full ride lifecycle: create → assign → accept
                                → start → complete | cancel.
                                """)
                        .contact(new Contact()
                                .name("RideLink Team")
                                .email("team@ridelink.example.com")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
