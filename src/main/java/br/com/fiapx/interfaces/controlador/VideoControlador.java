package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.casosdeuso.BaixarVideo;
import br.com.fiapx.aplicacao.casosdeuso.EnviarVideo;
import br.com.fiapx.aplicacao.casosdeuso.ListarVideos;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.interfaces.dto.resposta.VideoResposta;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoControlador {

    private final EnviarVideo enviarVideo;
    private final BaixarVideo baixarVideo;
    private final ListarVideos listarVideos;

    public VideoControlador(EnviarVideo enviarVideo, BaixarVideo baixarVideo, ListarVideos listarVideos) {
        this.enviarVideo = enviarVideo;
        this.baixarVideo = baixarVideo;
        this.listarVideos = listarVideos;
    }

    @PostMapping("/enviar")
    public ResponseEntity<VideoResposta> enviar(@RequestParam("video") MultipartFile arquivo) throws IOException {
        Video video = enviarVideo.executar(
                arquivo.getOriginalFilename(),
                arquivo.getInputStream()
        );

        return ResponseEntity.ok(VideoResposta.fromVideo(video));
    }

    @GetMapping
    public ResponseEntity<List<VideoResposta>> listar() {
        List<VideoResposta> videos = listarVideos.executar()
                .stream()
                .map(VideoResposta::fromVideo)
                .toList();

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}/baixar")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id) {
        byte[] conteudo = baixarVideo.executar(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", id + "_frames.zip");

        return ResponseEntity.ok()
                .headers(headers)
                .body(conteudo);
    }
}
