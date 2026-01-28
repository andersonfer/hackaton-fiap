package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnviarVideoTest {

    @Mock
    private ArmazenamentoArquivoGateway armazenamentoGateway;

    @Mock
    private ProcessadorVideoGateway processadorGateway;

    private EnviarVideo enviarVideo;

    @BeforeEach
    void setUp() {
        enviarVideo = new EnviarVideo(armazenamentoGateway, processadorGateway);
    }

    @Test
    void deveProcessarVideoComSucesso() {
        String nomeArquivo = "video.mp4";
        InputStream conteudo = new ByteArrayInputStream("conteudo".getBytes());
        Path caminhoVideo = Paths.get("/tmp/video.mp4");
        Path caminhoZip = Paths.get("/tmp/1.zip");

        when(armazenamentoGateway.salvarVideo(eq(nomeArquivo), any())).thenReturn(caminhoVideo);
        when(processadorGateway.processarVideo(eq(caminhoVideo), any())).thenReturn(caminhoZip);

        Video resultado = enviarVideo.executar(nomeArquivo, conteudo);

        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals(nomeArquivo, resultado.getNomeOriginal());
        assertEquals(StatusVideo.CONCLUIDO, resultado.getStatus());
        assertEquals(caminhoZip.toString(), resultado.getCaminhoZip());
        assertNull(resultado.getMensagemErro());
    }

    @Test
    void deveMarcarComoFalhaQuandoOcorrerErro() {
        String nomeArquivo = "video.mp4";
        InputStream conteudo = new ByteArrayInputStream("conteudo".getBytes());
        Path caminhoVideo = Paths.get("/tmp/video.mp4");

        when(armazenamentoGateway.salvarVideo(eq(nomeArquivo), any())).thenReturn(caminhoVideo);
        when(processadorGateway.processarVideo(eq(caminhoVideo), any()))
                .thenThrow(new RuntimeException("Erro no FFmpeg"));

        Video resultado = enviarVideo.executar(nomeArquivo, conteudo);

        assertNotNull(resultado);
        assertEquals(StatusVideo.FALHA, resultado.getStatus());
        assertEquals("Erro no FFmpeg", resultado.getMensagemErro());
    }
}
