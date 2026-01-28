package br.com.fiapx.infraestrutura.mensageria;

import java.io.Serializable;

public record MensagemProcessamentoVideo(
        Long videoId,
        String caminhoArquivo
) implements Serializable {
}
