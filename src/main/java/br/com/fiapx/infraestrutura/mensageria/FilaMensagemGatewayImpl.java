package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mensageria.habilitado", havingValue = "true", matchIfMissing = true)
public class FilaMensagemGatewayImpl implements FilaMensagemGateway {

    private final PublicadorMensagemVideo publicador;

    public FilaMensagemGatewayImpl(PublicadorMensagemVideo publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicarParaProcessamento(Long videoId, String caminhoArquivo) {
        publicador.publicar(videoId, caminhoArquivo);
    }
}
