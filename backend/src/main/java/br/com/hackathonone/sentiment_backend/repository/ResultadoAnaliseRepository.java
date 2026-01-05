package br.com.hackathonone.sentiment_backend.repository;

import br.com.hackathonone.sentiment_backend.domain.ResultadoAnalise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoAnaliseRepository extends JpaRepository<ResultadoAnalise, Long> {
}
