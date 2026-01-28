package br.com.fiapx.infraestrutura.persistencia.repositorio;

import br.com.fiapx.infraestrutura.persistencia.entidade.UsuarioEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepositorioJpa extends JpaRepository<UsuarioEntidade, Long> {

    Optional<UsuarioEntidade> findByEmail(String email);
}
