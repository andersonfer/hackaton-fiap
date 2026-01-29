package br.com.fiapx.dominio.excecao;

public class VideoNaoEncontradoException extends RuntimeException {

    public VideoNaoEncontradoException(Long videoId) {
        super("Video nao encontrado: " + videoId);
    }
}
