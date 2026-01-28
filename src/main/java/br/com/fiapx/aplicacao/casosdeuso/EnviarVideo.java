package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public class EnviarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;
    private final ProcessadorVideoGateway processadorGateway;
    private final AtomicLong contadorId = new AtomicLong(0);

    public EnviarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                       ProcessadorVideoGateway processadorGateway) {
        this.armazenamentoGateway = armazenamentoGateway;
        this.processadorGateway = processadorGateway;
    }

    public Video executar(String nomeArquivo, InputStream conteudo) {
        Path caminhoVideo = armazenamentoGateway.salvarVideo(nomeArquivo, conteudo);

        Video video = new Video(nomeArquivo, caminhoVideo.toString());
        video.setId(contadorId.incrementAndGet());

        try {
            video.marcarComoProcessando();
            Path caminhoZip = processadorGateway.processarVideo(caminhoVideo, video.getId());
            video.marcarComoConcluido(caminhoZip.toString());
            armazenamentoGateway.deletarArquivo(caminhoVideo);
        } catch (Exception e) {
            video.marcarComoFalha(e.getMessage());
        }

        return video;
    }
}
