package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.excecao.AcessoNegadoException;
import br.com.fiapx.dominio.excecao.VideoNaoEncontradoException;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaixarVideoTest {

    @Mock
    private ArmazenamentoArquivoGateway armazenamentoGateway;

    @Mock
    private VideoRepositorio videoRepositorio;

    private BaixarVideo baixarVideo;

    @BeforeEach
    void setUp() {
        baixarVideo = new BaixarVideo(armazenamentoGateway, videoRepositorio);
    }

    @Test
    void deveBaixarZipQuandoArquivoExiste() {
        Long videoId = 1L;
        Long usuarioId = 10L;
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");
        byte[] conteudoZip = "conteudo do zip".getBytes();

        Video video = new Video(usuarioId, "video.mp4", "/tmp/videos/video.mp4");
        video.setId(videoId);

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip)).thenReturn(conteudoZip);

        byte[] resultado = baixarVideo.executar(videoId, usuarioId);

        assertNotNull(resultado);
        assertArrayEquals(conteudoZip, resultado);
        verify(videoRepositorio).buscarPorId(videoId);
        verify(armazenamentoGateway).obterCaminhoZip(videoId);
        verify(armazenamentoGateway).lerArquivo(caminhoZip);
    }

    @Test
    void deveLancarExcecaoQuandoVideoNaoExiste() {
        Long videoId = 1L;
        Long usuarioId = 10L;

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.empty());

        assertThrows(VideoNaoEncontradoException.class, () -> {
            baixarVideo.executar(videoId, usuarioId);
        });
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEDono() {
        Long videoId = 1L;
        Long donoId = 10L;
        Long outroUsuarioId = 20L;

        Video video = new Video(donoId, "video.mp4", "/tmp/videos/video.mp4");
        video.setId(videoId);

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));

        AcessoNegadoException exception = assertThrows(AcessoNegadoException.class, () -> {
            baixarVideo.executar(videoId, outroUsuarioId);
        });

        assertTrue(exception.getMessage().contains("permissao"));
        verify(armazenamentoGateway, never()).obterCaminhoZip(any());
        verify(armazenamentoGateway, never()).lerArquivo(any());
    }

    @Test
    void deveLancarExcecaoQuandoZipNaoExiste() {
        Long videoId = 1L;
        Long usuarioId = 10L;
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");

        Video video = new Video(usuarioId, "video.mp4", "/tmp/videos/video.mp4");
        video.setId(videoId);

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip))
                .thenThrow(new RuntimeException("Arquivo nao encontrado: " + caminhoZip));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            baixarVideo.executar(videoId, usuarioId);
        });

        assertTrue(exception.getMessage().contains("Arquivo nao encontrado"));
    }

    @Test
    void deveObterCaminhoZipCorretoParaVideoId() {
        Long videoId = 42L;
        Long usuarioId = 10L;
        Path caminhoZip = Paths.get("/tmp/zips/42.zip");
        byte[] conteudoZip = "zip content".getBytes();

        Video video = new Video(usuarioId, "video.mp4", "/tmp/videos/video.mp4");
        video.setId(videoId);

        when(videoRepositorio.buscarPorId(videoId)).thenReturn(Optional.of(video));
        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip)).thenReturn(conteudoZip);

        baixarVideo.executar(videoId, usuarioId);

        verify(armazenamentoGateway).obterCaminhoZip(42L);
    }
}
