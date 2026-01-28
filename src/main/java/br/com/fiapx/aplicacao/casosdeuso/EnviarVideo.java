package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;

import java.io.InputStream;
import java.nio.file.Path;

public class EnviarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;
    private final ProcessadorVideoGateway processadorGateway;
    private final VideoRepositorio videoRepositorio;

    public EnviarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                       ProcessadorVideoGateway processadorGateway,
                       VideoRepositorio videoRepositorio) {
        this.armazenamentoGateway = armazenamentoGateway;
        this.processadorGateway = processadorGateway;
        this.videoRepositorio = videoRepositorio;
    }

    public Video executar(String nomeArquivo, InputStream conteudo) {
        Path caminhoVideo = armazenamentoGateway.salvarVideo(nomeArquivo, conteudo);

        Video video = new Video(nomeArquivo, caminhoVideo.toString());
        video = videoRepositorio.salvar(video);

        try {
            video.marcarComoProcessando();
            video = videoRepositorio.salvar(video);

            Path caminhoZip = processadorGateway.processarVideo(caminhoVideo, video.getId());
            video.marcarComoConcluido(caminhoZip.toString());
            armazenamentoGateway.deletarArquivo(caminhoVideo);
        } catch (Exception e) {
            video.marcarComoFalha(e.getMessage());
        }

        return videoRepositorio.salvar(video);
    }
}
