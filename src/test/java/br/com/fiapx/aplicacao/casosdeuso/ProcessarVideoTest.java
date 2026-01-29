package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessarVideoTest {

    @Mock
    private ArmazenamentoArquivoGateway armazenamentoGateway;

    @Mock
    private ProcessadorVideoGateway processadorGateway;

    @Mock
    private VideoRepositorio videoRepositorio;

    private ProcessarVideo processarVideo;

    @BeforeEach
    void setUp() {
        processarVideo = new ProcessarVideo(armazenamentoGateway, processadorGateway, videoRepositorio);
    }

    @Test
    void deveMarcarComoProcessandoAoIniciar() {
        Long videoId = 1L;
        String caminhoArquivo = "/tmp/videos/video.mp4";
        Video video = criarVideoComId(videoId);
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");
        List<StatusVideo> statusCapturados = new ArrayList<>();

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(processadorGateway.processarVideo(any(), eq(videoId))).thenReturn(caminhoZip);
        when(videoRepositorio.salvar(any())).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            statusCapturados.add(v.getStatus());
            return v;
        });

        processarVideo.executar(videoId, caminhoArquivo);

        assertEquals(2, statusCapturados.size());
        assertEquals(StatusVideo.PROCESSANDO, statusCapturados.get(0));
        assertEquals(StatusVideo.CONCLUIDO, statusCapturados.get(1));
    }

    @Test
    void deveMarcarComoConcluidoAposSucesso() {
        Long videoId = 1L;
        String caminhoArquivo = "/tmp/videos/video.mp4";
        Video video = criarVideoComId(videoId);
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");
        List<StatusVideo> statusCapturados = new ArrayList<>();
        List<String> zipCapturados = new ArrayList<>();

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(processadorGateway.processarVideo(any(), eq(videoId))).thenReturn(caminhoZip);
        when(videoRepositorio.salvar(any())).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            statusCapturados.add(v.getStatus());
            zipCapturados.add(v.getCaminhoZip());
            return v;
        });

        processarVideo.executar(videoId, caminhoArquivo);

        assertEquals(StatusVideo.CONCLUIDO, statusCapturados.get(1));
        assertEquals(caminhoZip.toString(), zipCapturados.get(1));
    }

    @Test
    void deveMarcarComoFalhaQuandoFFmpegFalha() {
        Long videoId = 1L;
        String caminhoArquivo = "/tmp/videos/video.mp4";
        Video video = criarVideoComId(videoId);
        String mensagemErro = "FFmpeg falhou ao processar o video";
        List<StatusVideo> statusCapturados = new ArrayList<>();
        List<String> errosCapturados = new ArrayList<>();

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(processadorGateway.processarVideo(any(), eq(videoId)))
                .thenThrow(new RuntimeException(mensagemErro));
        when(videoRepositorio.salvar(any())).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            statusCapturados.add(v.getStatus());
            errosCapturados.add(v.getMensagemErro());
            return v;
        });

        assertThrows(RuntimeException.class, () ->
                processarVideo.executar(videoId, caminhoArquivo));

        assertEquals(2, statusCapturados.size());
        assertEquals(StatusVideo.FALHA, statusCapturados.get(1));
        assertEquals(mensagemErro, errosCapturados.get(1));
    }

    @Test
    void deveDeletarArquivoOriginalAposSucesso() {
        Long videoId = 1L;
        String caminhoArquivo = "/tmp/videos/video.mp4";
        Video video = criarVideoComId(videoId);
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(processadorGateway.processarVideo(any(), eq(videoId))).thenReturn(caminhoZip);
        when(videoRepositorio.salvar(any())).thenReturn(video);

        processarVideo.executar(videoId, caminhoArquivo);

        verify(armazenamentoGateway).deletarArquivo(Paths.get(caminhoArquivo));
    }

    @Test
    void deveLancarExcecaoQuandoVideoNaoExiste() {
        Long videoId = 999L;
        String caminhoArquivo = "/tmp/videos/video.mp4";

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processarVideo.executar(videoId, caminhoArquivo);
        });

        assertEquals("Video nao encontrado: 999", exception.getMessage());
        verify(processadorGateway, never()).processarVideo(any(), any());
    }

    @Test
    void naoDeveDeletarArquivoOriginalQuandoFalha() {
        Long videoId = 1L;
        String caminhoArquivo = "/tmp/videos/video.mp4";
        Video video = criarVideoComId(videoId);

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(processadorGateway.processarVideo(any(), eq(videoId)))
                .thenThrow(new RuntimeException("Erro no processamento"));
        when(videoRepositorio.salvar(any())).thenReturn(video);

        assertThrows(RuntimeException.class, () ->
                processarVideo.executar(videoId, caminhoArquivo));

        verify(armazenamentoGateway, never()).deletarArquivo(any());
    }

    private Video criarVideoComId(Long id) {
        Video video = new Video(1L, "video.mp4", "/tmp/videos/video.mp4");
        video.setId(id);
        return video;
    }
}
