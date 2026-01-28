package br.com.fiapx.dominio.entidade;

import br.com.fiapx.dominio.enums.StatusVideo;

public class Video {

    private Long id;
    private String nomeOriginal;
    private StatusVideo status;
    private String caminhoArquivo;
    private String caminhoZip;
    private String mensagemErro;

    public Video() {
        this.status = StatusVideo.PENDENTE;
    }

    public Video(String nomeOriginal, String caminhoArquivo) {
        this.nomeOriginal = nomeOriginal;
        this.caminhoArquivo = caminhoArquivo;
        this.status = StatusVideo.PENDENTE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public StatusVideo getStatus() {
        return status;
    }

    public void setStatus(StatusVideo status) {
        this.status = status;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public String getCaminhoZip() {
        return caminhoZip;
    }

    public void setCaminhoZip(String caminhoZip) {
        this.caminhoZip = caminhoZip;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public void marcarComoProcessando() {
        this.status = StatusVideo.PROCESSANDO;
    }

    public void marcarComoConcluido(String caminhoZip) {
        this.status = StatusVideo.CONCLUIDO;
        this.caminhoZip = caminhoZip;
    }

    public void marcarComoFalha(String mensagemErro) {
        this.status = StatusVideo.FALHA;
        this.mensagemErro = mensagemErro;
    }
}
