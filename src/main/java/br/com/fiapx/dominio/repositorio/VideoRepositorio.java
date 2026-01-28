package br.com.fiapx.dominio.repositorio;

import br.com.fiapx.dominio.entidade.Video;

import java.util.List;
import java.util.Optional;

public interface VideoRepositorio {

    Video salvar(Video video);

    Optional<Video> buscarPorId(Long id);

    List<Video> listarTodos();

    List<Video> listarPorUsuarioId(Long usuarioId);
}
