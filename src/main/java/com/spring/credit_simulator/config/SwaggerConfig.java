package com.spring.credit_simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do Swagger/OpenAPI para documentação da API.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers());
    }

    private Info getApiInfo() {
        return new Info()
                .title("API de Simulação de Crédito")
                .description(
                        "API RESTful para simulação de empréstimos bancários. " +
                                "Esta API calcula parcelas, juros e valor total de empréstimos " +
                                "baseando-se na idade do cliente e oferece diferentes taxas de juros " +
                                "por faixa etária. " +
                                "\n\n" +
                                "**Funcionalidades principais:**\n" +
                                "- Simulação individual de crédito com cálculo instantâneo\n" +
                                "- Processamento em lote para até 10.000 simulações\n" +
                                "- Cálculo automático de taxa de juros por idade\n" +
                                "- Suporte para processamento assíncrono de grandes volumes\n" +
                                "\n\n" +
                                "**Regras de negócio:**\n" +
                                "- Idade mínima: 18 anos\n" +
                                "- Valor do empréstimo: R$ 1.000 a R$ 1.000.000\n" +
                                "- Prazo: 6 a 360 meses\n" +
                                "\n\n" +
                                "**Taxas de juros por faixa etária:**\n" +
                                "- Até 25 anos: 5% ao ano\n" +
                                "- 26 a 40 anos: 3% ao ano\n" +
                                "- 41 a 60 anos: 2% ao ano\n" +
                                "- Acima de 60 anos: 4% ao ano"
                )
                .version("1.0.0")
                .contact(getContact());
    }

    private Contact getContact() {
        return new Contact()
                .name("João Silveira")
                .email("joaoadsistemas@gmail.com");
    }

    /**
     * Configura servidores para diferentes ambientes (local e Docker).
     */
    private List<Server> getServers() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Servidor de desenvolvimento local");

        Server dockerServer = new Server()
                .url("http://localhost:8080")
                .description("Servidor Docker local");

        return List.of(localServer, dockerServer);
    }
}