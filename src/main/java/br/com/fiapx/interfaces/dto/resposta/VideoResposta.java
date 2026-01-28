package br.com.fiapx.interfaces.dto.resposta;

import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;

public record VideoResposta(
        Long id,
        String nomeOriginal,
        StatusVideo status,
        String urlDownload,
        String mensagemErro
) {
    public static VideoResposta fromVideo(Video video) {
        String urlDownload = null;
        if (video.getStatus() == StatusVideo.CONCLUIDO) {
            urlDownload = "/api/videos/" + video.getId() + "/baixar";
        }

        return new VideoResposta(
                video.getId(),
                video.getNomeOriginal(),
                video.getStatus(),
                urlDownload,
                video.getMensagemErro()
        );
    }
}
