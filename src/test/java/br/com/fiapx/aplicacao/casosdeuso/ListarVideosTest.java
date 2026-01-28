package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListarVideosTest {

    @Mock
    private VideoRepositorio videoRepositorio;

    private ListarVideos listarVideos;

    @BeforeEach
    void setUp() {
        listarVideos = new ListarVideos(videoRepositorio);
    }

    @Test
    void deveListarVideosDoUsuario() {
        Long usuarioId = 1L;
        Video video1 = criarVideo(1L, usuarioId, "video1.mp4", StatusVideo.CONCLUIDO);
        Video video2 = criarVideo(2L, usuarioId, "video2.mp4", StatusVideo.PROCESSANDO);
        List<Video> videosEsperados = Arrays.asList(video1, video2);

        when(videoRepositorio.listarPorUsuarioId(usuarioId)).thenReturn(videosEsperados);

        List<Video> resultado = listarVideos.executar(usuarioId);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("video1.mp4", resultado.get(0).getNomeOriginal());
        assertEquals("video2.mp4", resultado.get(1).getNomeOriginal());
        verify(videoRepositorio).listarPorUsuarioId(usuarioId);
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioSemVideos() {
        Long usuarioId = 1L;

        when(videoRepositorio.listarPorUsuarioId(usuarioId)).thenReturn(Collections.emptyList());

        List<Video> resultado = listarVideos.executar(usuarioId);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(videoRepositorio).listarPorUsuarioId(usuarioId);
    }

    @Test
    void deveListarApenasVideosDoUsuarioEspecificado() {
        Long usuarioId = 1L;
        Video video = criarVideo(1L, usuarioId, "video.mp4", StatusVideo.PENDENTE);

        when(videoRepositorio.listarPorUsuarioId(usuarioId)).thenReturn(List.of(video));

        List<Video> resultado = listarVideos.executar(usuarioId);

        assertEquals(1, resultado.size());
        assertEquals(usuarioId, resultado.get(0).getUsuarioId());
    }

    @Test
    void deveListarVideosComDiferentesStatus() {
        Long usuarioId = 1L;
        Video videoPendente = criarVideo(1L, usuarioId, "pendente.mp4", StatusVideo.PENDENTE);
        Video videoProcessando = criarVideo(2L, usuarioId, "processando.mp4", StatusVideo.PROCESSANDO);
        Video videoConcluido = criarVideo(3L, usuarioId, "concluido.mp4", StatusVideo.CONCLUIDO);
        Video videoFalha = criarVideo(4L, usuarioId, "falha.mp4", StatusVideo.FALHA);

        List<Video> todosVideos = Arrays.asList(videoPendente, videoProcessando, videoConcluido, videoFalha);
        when(videoRepositorio.listarPorUsuarioId(usuarioId)).thenReturn(todosVideos);

        List<Video> resultado = listarVideos.executar(usuarioId);

        assertEquals(4, resultado.size());
        assertEquals(StatusVideo.PENDENTE, resultado.get(0).getStatus());
        assertEquals(StatusVideo.PROCESSANDO, resultado.get(1).getStatus());
        assertEquals(StatusVideo.CONCLUIDO, resultado.get(2).getStatus());
        assertEquals(StatusVideo.FALHA, resultado.get(3).getStatus());
    }

    private Video criarVideo(Long id, Long usuarioId, String nome, StatusVideo status) {
        Video video = new Video(usuarioId, nome, "/tmp/" + nome);
        video.setId(id);
        video.setStatus(status);
        return video;
    }
}
