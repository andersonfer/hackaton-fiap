package br.com.fiapx.aplicacao.gateway;

public interface FilaMensagemGateway {

    void publicarParaProcessamento(Long videoId, String caminhoArquivo);
}
