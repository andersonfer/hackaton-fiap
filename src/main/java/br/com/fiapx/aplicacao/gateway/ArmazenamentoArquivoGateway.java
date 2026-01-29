package br.com.fiapx.aplicacao.gateway;

import java.io.InputStream;
import java.nio.file.Path;

public interface ArmazenamentoArquivoGateway {

    Path salvarVideo(Long videoId, String nomeArquivo, InputStream conteudo);

    Path obterCaminhoZip(Long videoId);

    byte[] lerArquivo(Path caminho);

    void deletarArquivo(Path caminho);
}
