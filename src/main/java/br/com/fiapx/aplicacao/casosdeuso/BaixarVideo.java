package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.excecao.AcessoNegadoException;
import br.com.fiapx.dominio.excecao.VideoNaoEncontradoException;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;

import java.nio.file.Path;

public class BaixarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;
    private final VideoRepositorio videoRepositorio;

    public BaixarVideo(ArmazenamentoArquivoGateway armazenamentoGateway, VideoRepositorio videoRepositorio) {
        this.armazenamentoGateway = armazenamentoGateway;
        this.videoRepositorio = videoRepositorio;
    }

    public byte[] executar(Long videoId, Long usuarioId) {
        Video video = videoRepositorio.buscarPorId(videoId)
                .orElseThrow(() -> new VideoNaoEncontradoException(videoId));

        if (!video.getUsuarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("Usuario nao tem permissao para acessar este video");
        }

        Path caminhoZip = armazenamentoGateway.obterCaminhoZip(videoId);
        return armazenamentoGateway.lerArquivo(caminhoZip);
    }
}
