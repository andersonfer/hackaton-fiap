package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;
    private final ProcessadorVideoGateway processadorGateway;
    private final VideoRepositorio videoRepositorio;

    public ProcessarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                          ProcessadorVideoGateway processadorGateway,
                          VideoRepositorio videoRepositorio) {
        this.armazenamentoGateway = armazenamentoGateway;
        this.processadorGateway = processadorGateway;
        this.videoRepositorio = videoRepositorio;
    }

    public void executar(Long videoId, String caminhoArquivo) {
        Video video = videoRepositorio.buscarPorId(videoId)
                .orElseThrow(() -> new RuntimeException("Video nao encontrado: " + videoId));

        Path caminhoVideo = Paths.get(caminhoArquivo);

        try {
            video.marcarComoProcessando();
            videoRepositorio.salvar(video);

            Path caminhoZip = processadorGateway.processarVideo(caminhoVideo, videoId);
            video.marcarComoConcluido(caminhoZip.toString());
            armazenamentoGateway.deletarArquivo(caminhoVideo);
        } catch (Exception e) {
            video.marcarComoFalha(e.getMessage());
        }

        videoRepositorio.salvar(video);
    }
}
