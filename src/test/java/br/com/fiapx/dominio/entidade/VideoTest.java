package br.com.fiapx.dominio.entidade;

import br.com.fiapx.dominio.enums.StatusVideo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoTest {

    @Test
    void deveCriarVideoComStatusPendente() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");

        assertEquals("video.mp4", video.getNomeOriginal());
        assertEquals("/tmp/video.mp4", video.getCaminhoArquivo());
        assertEquals(StatusVideo.PENDENTE, video.getStatus());
    }

    @Test
    void deveMarcarComoProcessando() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");

        video.marcarComoProcessando();

        assertEquals(StatusVideo.PROCESSANDO, video.getStatus());
    }

    @Test
    void deveMarcarComoConcluido() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");
        String caminhoZip = "/tmp/1.zip";

        video.marcarComoConcluido(caminhoZip);

        assertEquals(StatusVideo.CONCLUIDO, video.getStatus());
        assertEquals(caminhoZip, video.getCaminhoZip());
    }

    @Test
    void deveMarcarComoFalha() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");
        String mensagemErro = "Erro no processamento";

        video.marcarComoFalha(mensagemErro);

        assertEquals(StatusVideo.FALHA, video.getStatus());
        assertEquals(mensagemErro, video.getMensagemErro());
    }
}
