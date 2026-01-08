package br.com.hackathonone.sentiment_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import br.com.hackathonone.sentiment_backend.config.DsProperties;

@SpringBootApplication
@EnableConfigurationProperties(DsProperties.class)
@EnableFeignClients // Essa é a linha nova que habilita o Feign
public class SentimentBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentimentBackendApplication.class, args);
	}
}