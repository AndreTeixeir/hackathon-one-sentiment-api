package br.com.hackathonone.sentiment_backend.repository;

import br.com.hackathonone.sentiment_backend.domain.Cliente;
import br.com.hackathonone.sentiment_backend.domain.enums.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmail(String email);

    List<Cliente> findByTipoCliente(TipoCliente tipoCliente);
}
