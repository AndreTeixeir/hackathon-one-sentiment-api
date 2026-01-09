package br.com.hackathonone.sentiment_backend;

import br.com.hackathonone.sentiment_backend.config.DsProperties;
import br.com.hackathonone.sentiment_backend.domain.Cliente;
import br.com.hackathonone.sentiment_backend.domain.enums.TipoCliente;
import br.com.hackathonone.sentiment_backend.repository.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableConfigurationProperties(DsProperties.class)
@EnableFeignClients
public class SentimentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentimentBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner init(ClienteRepository clienteRepository) {
        return args -> {
            Cliente cliente = Cliente.builder()
                    .nome("Cliente de Teste")
                    .email("teste@exemplo.com")
                    .tipoCliente(TipoCliente.CLIENTE_COMPRADOR)
                    .criadoEm(LocalDateTime.now())
                    .build();

            clienteRepository.save(cliente);

            long total = clienteRepository.count();
            System.out.println("Total de clientes no banco: " + total);
        };
    }
}
