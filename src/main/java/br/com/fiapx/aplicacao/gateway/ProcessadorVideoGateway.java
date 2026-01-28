package br.com.fiapx.aplicacao.gateway;

import java.nio.file.Path;

public interface ProcessadorVideoGateway {

    Path processarVideo(Path caminhoVideo, Long videoId);
}
