package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;

import java.nio.file.Path;

public class BaixarVideo {

    private final ArmazenamentoArquivoGateway armazenamentoGateway;

    public BaixarVideo(ArmazenamentoArquivoGateway armazenamentoGateway) {
        this.armazenamentoGateway = armazenamentoGateway;
    }

    public byte[] executar(Long videoId) {
        Path caminhoZip = armazenamentoGateway.obterCaminhoZip(videoId);
        return armazenamentoGateway.lerArquivo(caminhoZip);
    }
}
