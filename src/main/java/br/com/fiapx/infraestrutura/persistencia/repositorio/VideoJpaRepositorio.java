package br.com.fiapx.infraestrutura.persistencia.repositorio;

import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.infraestrutura.persistencia.entidade.VideoEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoJpaRepositorio extends JpaRepository<VideoEntidade, Long> {

    List<VideoEntidade> findAllByOrderByCriadoEmDesc();

    List<VideoEntidade> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    List<VideoEntidade> findByStatusAndAtualizadoEmBefore(StatusVideo status, LocalDateTime limite);
}
