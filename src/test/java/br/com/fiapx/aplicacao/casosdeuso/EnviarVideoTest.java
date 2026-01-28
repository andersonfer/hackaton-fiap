package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnviarVideoTest {

    @Mock
    private ArmazenamentoArquivoGateway armazenamentoGateway;

    @Mock
    private FilaMensagemGateway filaMensagemGateway;

    @Mock
    private VideoRepositorio videoRepositorio;

    private EnviarVideo enviarVideo;

    @BeforeEach
    void setUp() {
        enviarVideo = new EnviarVideo(armazenamentoGateway, filaMensagemGateway, videoRepositorio);

        when(videoRepositorio.salvar(any(Video.class))).thenAnswer(invocation -> {
            Video video = invocation.getArgument(0);
            if (video.getId() == null) {
                video.setId(1L);
            }
            return video;
        });
    }

    @Test
    void deveEnviarVideoParaFilaComStatusPendente() {
        String nomeArquivo = "video.mp4";
        InputStream conteudo = new ByteArrayInputStream("conteudo".getBytes());
        Path caminhoVideo = Paths.get("/tmp/video.mp4");

        when(armazenamentoGateway.salvarVideo(eq(nomeArquivo), any())).thenReturn(caminhoVideo);

        Video resultado = enviarVideo.executar(nomeArquivo, conteudo);

        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals(nomeArquivo, resultado.getNomeOriginal());
        assertEquals(StatusVideo.PENDENTE, resultado.getStatus());

        verify(filaMensagemGateway).publicarParaProcessamento(resultado.getId(), caminhoVideo.toString());
    }

    @Test
    void deveSalvarVideoAntesDePublicarNaFila() {
        String nomeArquivo = "video.mp4";
        InputStream conteudo = new ByteArrayInputStream("conteudo".getBytes());
        Path caminhoVideo = Paths.get("/tmp/video.mp4");

        when(armazenamentoGateway.salvarVideo(eq(nomeArquivo), any())).thenReturn(caminhoVideo);

        Video resultado = enviarVideo.executar(nomeArquivo, conteudo);

        verify(videoRepositorio).salvar(any(Video.class));
        verify(filaMensagemGateway).publicarParaProcessamento(resultado.getId(), caminhoVideo.toString());
    }
}
