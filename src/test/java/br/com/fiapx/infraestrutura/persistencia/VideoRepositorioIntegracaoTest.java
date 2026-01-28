package br.com.fiapx.infraestrutura.persistencia;

import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VideoRepositorioIntegracaoTest {

    @Autowired
    private VideoRepositorio videoRepositorio;

    @Test
    void deveSalvarVideo() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");

        Video salvo = videoRepositorio.salvar(video);

        assertNotNull(salvo.getId());
        assertEquals("video.mp4", salvo.getNomeOriginal());
        assertEquals(StatusVideo.PENDENTE, salvo.getStatus());
    }

    @Test
    void deveBuscarVideoPorId() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");
        Video salvo = videoRepositorio.salvar(video);

        Optional<Video> encontrado = videoRepositorio.buscarPorId(salvo.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(salvo.getId(), encontrado.get().getId());
    }

    @Test
    void deveListarTodosVideos() {
        videoRepositorio.salvar(new Video("video1.mp4", "/tmp/video1.mp4"));
        videoRepositorio.salvar(new Video("video2.mp4", "/tmp/video2.mp4"));

        List<Video> videos = videoRepositorio.listarTodos();

        assertEquals(2, videos.size());
    }

    @Test
    void deveAtualizarStatusDoVideo() {
        Video video = new Video("video.mp4", "/tmp/video.mp4");
        Video salvo = videoRepositorio.salvar(video);

        salvo.marcarComoConcluido("/tmp/1.zip");
        Video atualizado = videoRepositorio.salvar(salvo);

        assertEquals(StatusVideo.CONCLUIDO, atualizado.getStatus());
        assertEquals("/tmp/1.zip", atualizado.getCaminhoZip());
    }
}
