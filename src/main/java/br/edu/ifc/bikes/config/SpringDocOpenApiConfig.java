package br.edu.ifc.bikes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
public class SpringDocOpenApiConfig {

    public static final String SECURITY_SCHEME = "security";

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, securityScheme()))
                .info(new Info()
                        .title("Bikes API")
                        .description("API para gestão de locadora de bikes.")
                        .version("V1")
                        .contact(new Contact()
                                .name("Fábio Pinheiro")
                                .email("fabio.pinheiro@ifc.edu.br")
                        )
                );
    }

    //habilita o botão "Authorize" do Swagger UI para informar o JWT devolvido por POST /api/v1/auth
    private SecurityScheme securityScheme(){
        return new SecurityScheme()
                .description("Insira o token JWT obtido em POST /api/v1/auth (sem o prefixo Bearer)")
                .type(SecurityScheme.Type.HTTP)
                .in(SecurityScheme.In.HEADER)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(SECURITY_SCHEME);
    }

    //só as operações protegidas por @PreAuthorize exibem o cadeado; as públicas (criar usuário, autenticar) ficam livres
    @Bean
    public OperationCustomizer securityRequirementCustomizer(){
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(PreAuthorize.class)) {
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
            }
            return operation;
        };
    }
}
