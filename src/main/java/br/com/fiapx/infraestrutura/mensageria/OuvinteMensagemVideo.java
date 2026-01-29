package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.casosdeuso.ProcessarVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class OuvinteMensagemVideo {

    private static final Logger log = LoggerFactory.getLogger(OuvinteMensagemVideo.class);

    private final ProcessarVideo processarVideo;
    private final TaskExecutor executorProcessamento;

    public OuvinteMensagemVideo(ProcessarVideo processarVideo,
                                @Qualifier("executorProcessamento") TaskExecutor executorProcessamento) {
        this.processarVideo = processarVideo;
        this.executorProcessamento = executorProcessamento;
    }

    @RabbitListener(queues = "${app.mensageria.fila-processamento}")
    public void processar(MensagemProcessamentoVideo mensagem) {
        log.info("Recebida mensagem para processar video: {}. Delegando ao executor.", mensagem.videoId());
        executorProcessamento.execute(() -> {
            try {
                processarVideo.executar(mensagem.videoId(), mensagem.caminhoArquivo());
                log.info("Video processado com sucesso: {}", mensagem.videoId());
            } catch (Exception e) {
                log.error("Erro ao processar video {}: {}", mensagem.videoId(), e.getMessage());
            }
        });
    }
}
