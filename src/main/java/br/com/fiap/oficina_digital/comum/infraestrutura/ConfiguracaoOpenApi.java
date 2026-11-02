package br.com.fiap.oficina_digital.comum.infraestrutura;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApi {
        @Bean
        OpenAPI oficinaDigitalOpenApi() {
                return new OpenAPI()
                                .info(new Info().title("Oficina Digital API")
                                                .description("API para atendimento de clientes, gestão de veículos, ordens de serviço, catálogo, estoque e indicadores. Todas as rotas, exceto login e acompanhamento por código, exigem um JWT Bearer.")
                                                .version("v1"))
                                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                                .components(new Components().addSecuritySchemes("bearer-jwt",
                                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                                                .bearerFormat("JWT")));
        }
}
