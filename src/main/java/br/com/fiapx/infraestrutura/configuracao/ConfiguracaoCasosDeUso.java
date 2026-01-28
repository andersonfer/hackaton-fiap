package br.com.fiapx.infraestrutura.configuracao;

import br.com.fiapx.aplicacao.casosdeuso.BaixarVideo;
import br.com.fiapx.aplicacao.casosdeuso.EnviarVideo;
import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoCasosDeUso {

    @Bean
    public EnviarVideo enviarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                                   ProcessadorVideoGateway processadorGateway) {
        return new EnviarVideo(armazenamentoGateway, processadorGateway);
    }

    @Bean
    public BaixarVideo baixarVideo(ArmazenamentoArquivoGateway armazenamentoGateway) {
        return new BaixarVideo(armazenamentoGateway);
    }
}
