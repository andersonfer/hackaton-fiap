package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;

import java.io.InputStream;
import java.nio.file.Path;

public class EnviarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;
    private final FilaMensagemGateway filaMensagemGateway;
    private final VideoRepositorio videoRepositorio;

    public EnviarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                       FilaMensagemGateway filaMensagemGateway,
                       VideoRepositorio videoRepositorio) {
        this.armazenamentoGateway = armazenamentoGateway;
        this.filaMensagemGateway = filaMensagemGateway;
        this.videoRepositorio = videoRepositorio;
    }

    public Video executar(Long usuarioId, String nomeArquivo, InputStream conteudo) {
        Path caminhoVideo = armazenamentoGateway.salvarVideo(nomeArquivo, conteudo);

        Video video = new Video(usuarioId, nomeArquivo, caminhoVideo.toString());
        video = videoRepositorio.salvar(video);

        filaMensagemGateway.publicarParaProcessamento(video.getId(), caminhoVideo.toString());

        return video;
    }
}
