package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.casosdeuso.ProcessarVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class OuvinteMensagemVideo {

    private static final Logger log = LoggerFactory.getLogger(OuvinteMensagemVideo.class);

    private final ProcessarVideo processarVideo;

    public OuvinteMensagemVideo(ProcessarVideo processarVideo) {
        this.processarVideo = processarVideo;
    }

    @RabbitListener(queues = "${app.mensageria.fila-processamento}",
                    concurrency = "${app.processamento.max-paralelo:5}")
    public void processar(MensagemProcessamentoVideo mensagem) {
        log.info("Processando video: {} na thread {}", mensagem.videoId(), Thread.currentThread().getName());
        try {
            processarVideo.executar(mensagem.videoId(), mensagem.caminhoArquivo());
            log.info("Video processado com sucesso: {}", mensagem.videoId());
        } catch (Exception e) {
            log.error("Erro ao processar video {}: {}", mensagem.videoId(), e.getMessage());
        }
    }
}
