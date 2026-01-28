package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.casosdeuso.BaixarVideo;
import br.com.fiapx.aplicacao.casosdeuso.EnviarVideo;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.interfaces.dto.resposta.VideoResposta;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/videos")
public class VideoControlador {

    private final EnviarVideo enviarVideo;
    private final BaixarVideo baixarVideo;

    public VideoControlador(EnviarVideo enviarVideo, BaixarVideo baixarVideo) {
        this.enviarVideo = enviarVideo;
        this.baixarVideo = baixarVideo;
    }

    @PostMapping("/enviar")
    public ResponseEntity<VideoResposta> enviar(@RequestParam("video") MultipartFile arquivo) throws IOException {
        Video video = enviarVideo.executar(
                arquivo.getOriginalFilename(),
                arquivo.getInputStream()
        );

        return ResponseEntity.ok(VideoResposta.fromVideo(video));
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
