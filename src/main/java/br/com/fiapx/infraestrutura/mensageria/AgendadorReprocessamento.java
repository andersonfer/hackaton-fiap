package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class AgendadorReprocessamento {

    private static final Logger log = LoggerFactory.getLogger(AgendadorReprocessamento.class);

    private final VideoRepositorio videoRepositorio;
    private final FilaMensagemGateway filaMensagemGateway;
    private final int timeoutMinutos;

    public AgendadorReprocessamento(VideoRepositorio videoRepositorio,
                                    FilaMensagemGateway filaMensagemGateway,
                                    @Value("${app.reprocessamento.timeout-minutos:5}") int timeoutMinutos) {
        this.videoRepositorio = videoRepositorio;
        this.filaMensagemGateway = filaMensagemGateway;
        this.timeoutMinutos = timeoutMinutos;
    }

    @Scheduled(fixedDelayString = "${app.reprocessamento.intervalo-ms:60000}")
    public void verificarVideosTravados() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(timeoutMinutos);

        List<Video> videosTravados = videoRepositorio.buscarPorStatusEAtualizadoAntesDe(
                StatusVideo.PROCESSANDO, limite);

        for (Video video : videosTravados) {
            log.warn("Video travado detectado: id={}, atualizadoEm={}. Resubmetendo para processamento.",
                    video.getId(), video.getAtualizadoEm());

            video.setStatus(StatusVideo.PENDENTE);
            videoRepositorio.salvar(video);

            filaMensagemGateway.publicarParaProcessamento(video.getId(), video.getCaminhoArquivo(), video.getUsuarioId());
        }

        if (!videosTravados.isEmpty()) {
            log.info("Resubmetidos {} video(s) travado(s) para reprocessamento.", videosTravados.size());
        }
    }
}
