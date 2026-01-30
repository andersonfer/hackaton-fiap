package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.casosdeuso.BaixarVideo;
import br.com.fiapx.aplicacao.casosdeuso.EnviarVideo;
import br.com.fiapx.aplicacao.casosdeuso.ListarVideos;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.infraestrutura.seguranca.UsuarioAutenticado;
import br.com.fiapx.interfaces.dto.resposta.VideoResposta;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<VideoResposta> enviar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam("video") MultipartFile arquivo) throws IOException {
        Video video = enviarVideo.executar(
                usuario.id(),
                arquivo.getOriginalFilename(),
                arquivo.getInputStream()
        );

        return ResponseEntity.ok(VideoResposta.fromVideo(video));
    }

    @PostMapping("/enviar-lote")
    public ResponseEntity<List<VideoResposta>> enviarLote(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam("videos") List<MultipartFile> arquivos) throws IOException {
        if (arquivos.size() > 20) {
            return ResponseEntity.badRequest().build();
        }

        List<VideoResposta> respostas = new java.util.ArrayList<>();
        for (MultipartFile arquivo : arquivos) {
            Video video = enviarVideo.executar(
                    usuario.id(),
                    arquivo.getOriginalFilename(),
                    arquivo.getInputStream()
            );
            respostas.add(VideoResposta.fromVideo(video));
        }

        return ResponseEntity.ok(respostas);
    }

    @GetMapping
    public ResponseEntity<List<VideoResposta>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        List<VideoResposta> videos = listarVideos.executar(usuario.id())
                .stream()
                .map(VideoResposta::fromVideo)
                .toList();

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}/baixar")
    public ResponseEntity<byte[]> baixar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        byte[] conteudo = baixarVideo.executar(id, usuario.id());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", id + "_frames.zip");

        return ResponseEntity.ok()
                .headers(headers)
                .body(conteudo);
    }
}
