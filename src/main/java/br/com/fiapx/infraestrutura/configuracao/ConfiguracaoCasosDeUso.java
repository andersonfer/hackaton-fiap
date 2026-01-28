package br.com.fiapx.infraestrutura.configuracao;

import br.com.fiapx.aplicacao.casosdeuso.BaixarVideo;
import br.com.fiapx.aplicacao.casosdeuso.EnviarVideo;
import br.com.fiapx.aplicacao.casosdeuso.ListarVideos;
import br.com.fiapx.aplicacao.casosdeuso.ProcessarVideo;
import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoCasosDeUso {

    @Bean
    public EnviarVideo enviarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                                   FilaMensagemGateway filaMensagemGateway,
                                   VideoRepositorio videoRepositorio) {
        return new EnviarVideo(armazenamentoGateway, filaMensagemGateway, videoRepositorio);
    }

    @Bean
    public ProcessarVideo processarVideo(ArmazenamentoArquivoGateway armazenamentoGateway,
                                         ProcessadorVideoGateway processadorGateway,
                                         VideoRepositorio videoRepositorio) {
        return new ProcessarVideo(armazenamentoGateway, processadorGateway, videoRepositorio);
    }

    @Bean
    public BaixarVideo baixarVideo(ArmazenamentoArquivoGateway armazenamentoGateway) {
        return new BaixarVideo(armazenamentoGateway);
    }

    @Bean
    public ListarVideos listarVideos(VideoRepositorio videoRepositorio) {
        return new ListarVideos(videoRepositorio);
    }
}
