package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendadorReprocessamentoTest {

    @Mock
    private VideoRepositorio videoRepositorio;

    @Mock
    private FilaMensagemGateway filaMensagemGateway;

    private AgendadorReprocessamento agendador;

    @BeforeEach
    void setUp() {
        agendador = new AgendadorReprocessamento(videoRepositorio, filaMensagemGateway, 5);
    }

    @Test
    void deveResubmeterVideosTravadosEmProcessando() {
        Video videoTravado = criarVideo(1L, StatusVideo.PROCESSANDO, "/tmp/videos/video1.mp4");

        when(videoRepositorio.buscarPorStatusEAtualizadoAntesDe(eq(StatusVideo.PROCESSANDO), any(LocalDateTime.class)))
                .thenReturn(List.of(videoTravado));
        when(videoRepositorio.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        agendador.verificarVideosTravados();

        ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepositorio).salvar(videoCaptor.capture());
        assertEquals(StatusVideo.PENDENTE, videoCaptor.getValue().getStatus());

        verify(filaMensagemGateway).publicarParaProcessamento(1L, "/tmp/videos/video1.mp4");
    }

    @Test
    void deveResubmeterMultiplosVideosTravados() {
        Video video1 = criarVideo(1L, StatusVideo.PROCESSANDO, "/tmp/videos/video1.mp4");
        Video video2 = criarVideo(2L, StatusVideo.PROCESSANDO, "/tmp/videos/video2.mp4");

        when(videoRepositorio.buscarPorStatusEAtualizadoAntesDe(eq(StatusVideo.PROCESSANDO), any(LocalDateTime.class)))
                .thenReturn(List.of(video1, video2));
        when(videoRepositorio.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        agendador.verificarVideosTravados();

        verify(videoRepositorio, times(2)).salvar(any());
        verify(filaMensagemGateway).publicarParaProcessamento(1L, "/tmp/videos/video1.mp4");
        verify(filaMensagemGateway).publicarParaProcessamento(2L, "/tmp/videos/video2.mp4");
    }

    @Test
    void naoDeveResubmeterQuandoNaoHaVideosTravados() {
        when(videoRepositorio.buscarPorStatusEAtualizadoAntesDe(eq(StatusVideo.PROCESSANDO), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        agendador.verificarVideosTravados();

        verify(videoRepositorio, never()).salvar(any());
        verify(filaMensagemGateway, never()).publicarParaProcessamento(any(), any());
    }

    @Test
    void deveUsarTimeoutConfiguradoParaCalcularLimite() {
        when(videoRepositorio.buscarPorStatusEAtualizadoAntesDe(eq(StatusVideo.PROCESSANDO), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        LocalDateTime antes = LocalDateTime.now().minusMinutes(5);
        agendador.verificarVideosTravados();
        LocalDateTime depois = LocalDateTime.now().minusMinutes(5);

        ArgumentCaptor<LocalDateTime> limiteCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoRepositorio).buscarPorStatusEAtualizadoAntesDe(eq(StatusVideo.PROCESSANDO), limiteCaptor.capture());

        LocalDateTime limiteUsado = limiteCaptor.getValue();
        // O limite deve estar entre 'antes' e 'depois' (com tolerância de 1 segundo)
        assert !limiteUsado.isBefore(antes.minusSeconds(1));
        assert !limiteUsado.isAfter(depois.plusSeconds(1));
    }

    private Video criarVideo(Long id, StatusVideo status, String caminhoArquivo) {
        Video video = new Video(1L, "video.mp4", caminhoArquivo);
        video.setId(id);
        video.setStatus(status);
        video.setAtualizadoEm(LocalDateTime.now().minusMinutes(10));
        return video;
    }
}
