package br.com.fiapx.infraestrutura.mensageria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class PublicadorMensagemVideo {

    private final RabbitTemplate rabbitTemplate;
    private final String filaProcessamento;

    public PublicadorMensagemVideo(RabbitTemplate rabbitTemplate,
                                   @Value("${app.mensageria.fila-processamento}") String filaProcessamento) {
        this.rabbitTemplate = rabbitTemplate;
        this.filaProcessamento = filaProcessamento;
    }

    public void publicar(Long videoId, String caminhoArquivo, Long usuarioId) {
        MensagemProcessamentoVideo mensagem = new MensagemProcessamentoVideo(videoId, caminhoArquivo, usuarioId);
        rabbitTemplate.convertAndSend(filaProcessamento, mensagem);
    }
}
