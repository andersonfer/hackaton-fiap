package br.com.fiapx.infraestrutura.persistencia.repositorio;

import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import br.com.fiapx.infraestrutura.persistencia.entidade.VideoEntidade;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class VideoRepositorioImpl implements VideoRepositorio {

    private final VideoJpaRepositorio jpaRepositorio;

    public VideoRepositorioImpl(VideoJpaRepositorio jpaRepositorio) {
        this.jpaRepositorio = jpaRepositorio;
    }

    @Override
    public Video salvar(Video video) {
        VideoEntidade entidade = toEntidade(video);
        VideoEntidade salva = jpaRepositorio.save(entidade);
        return toDominio(salva);
    }

    @Override
    public Optional<Video> buscarPorId(Long id) {
        return jpaRepositorio.findById(id).map(this::toDominio);
    }

    @Override
    public List<Video> listarTodos() {
        return jpaRepositorio.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    public List<Video> listarPorUsuarioId(Long usuarioId) {
        return jpaRepositorio.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    public List<Video> buscarPorStatusEAtualizadoAntesDe(StatusVideo status, LocalDateTime limite) {
        return jpaRepositorio.findByStatusAndAtualizadoEmBefore(status, limite)
                .stream()
                .map(this::toDominio)
                .toList();
    }

    private VideoEntidade toEntidade(Video video) {
        VideoEntidade entidade = new VideoEntidade();
        entidade.setId(video.getId());
        entidade.setUsuarioId(video.getUsuarioId());
        entidade.setNomeOriginal(video.getNomeOriginal());
        entidade.setStatus(video.getStatus());
        entidade.setCaminhoArquivo(video.getCaminhoArquivo());
        entidade.setCaminhoZip(video.getCaminhoZip());
        entidade.setMensagemErro(video.getMensagemErro());
        if (video.getCriadoEm() != null) {
            entidade.setCriadoEm(video.getCriadoEm());
        }
        return entidade;
    }

    private Video toDominio(VideoEntidade entidade) {
        Video video = new Video();
        video.setId(entidade.getId());
        video.setUsuarioId(entidade.getUsuarioId());
        video.setNomeOriginal(entidade.getNomeOriginal());
        video.setStatus(entidade.getStatus());
        video.setCaminhoArquivo(entidade.getCaminhoArquivo());
        video.setCaminhoZip(entidade.getCaminhoZip());
        video.setMensagemErro(entidade.getMensagemErro());
        video.setCriadoEm(entidade.getCriadoEm());
        video.setAtualizadoEm(entidade.getAtualizadoEm());
        return video;
    }
}
