package br.com.fiapx.infraestrutura.mensageria;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class ConfiguracaoRabbitMQ {

    @Value("${app.mensageria.fila-processamento}")
    private String filaProcessamento;

    @Bean
    public Queue filaProcessamentoVideo() {
        return new Queue(filaProcessamento, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
