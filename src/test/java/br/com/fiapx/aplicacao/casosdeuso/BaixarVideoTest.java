package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaixarVideoTest {

    @Mock
    private ArmazenamentoArquivoGateway armazenamentoGateway;

    private BaixarVideo baixarVideo;

    @BeforeEach
    void setUp() {
        baixarVideo = new BaixarVideo(armazenamentoGateway);
    }

    @Test
    void deveBaixarZipQuandoArquivoExiste() {
        Long videoId = 1L;
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");
        byte[] conteudoZip = "conteudo do zip".getBytes();

        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip)).thenReturn(conteudoZip);

        byte[] resultado = baixarVideo.executar(videoId);

        assertNotNull(resultado);
        assertArrayEquals(conteudoZip, resultado);
        verify(armazenamentoGateway).obterCaminhoZip(videoId);
        verify(armazenamentoGateway).lerArquivo(caminhoZip);
    }

    @Test
    void deveLancarExcecaoQuandoZipNaoExiste() {
        Long videoId = 1L;
        Path caminhoZip = Paths.get("/tmp/zips/1.zip");

        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip))
                .thenThrow(new RuntimeException("Arquivo nao encontrado: " + caminhoZip));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            baixarVideo.executar(videoId);
        });

        assertTrue(exception.getMessage().contains("Arquivo nao encontrado"));
    }

    @Test
    void deveObterCaminhoZipCorretoParaVideoId() {
        Long videoId = 42L;
        Path caminhoZip = Paths.get("/tmp/zips/42.zip");
        byte[] conteudoZip = "zip content".getBytes();

        when(armazenamentoGateway.obterCaminhoZip(videoId)).thenReturn(caminhoZip);
        when(armazenamentoGateway.lerArquivo(caminhoZip)).thenReturn(conteudoZip);

        baixarVideo.executar(videoId);

        verify(armazenamentoGateway).obterCaminhoZip(42L);
    }
}
