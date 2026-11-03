package com.jaf.application.config.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "JAF Application API",
                description = "API para gerenciamento de funcionários, obras, gastos e relatórios",
                contact = @Contact(
                        name = "Grupo 6",
                        url = "https://github.com/grupo6/jaf-application",
                        email = "contato@jaf.com"
                ),
                license = @License(name = "MIT License"),
                version = "1.0.0"
        ),
        security = @SecurityRequirement(name = "cookieAuth")
)
@SecurityScheme(
        name = "cookieAuth",
        type = SecuritySchemeType.APIKEY,
        paramName = "jwt",
        in = SecuritySchemeIn.COOKIE
)
public class OpenApiConfig {

}
