package br.com.fiapx.dominio.entidade;

import java.time.LocalDateTime;

public class Usuario {

    private Long id;
    private String email;
    private String senhaHash;
    private LocalDateTime criadoEm;

    public Usuario() {
        this.criadoEm = LocalDateTime.now();
    }

    public Usuario(String email, String senhaHash) {
        this.email = email;
        this.senhaHash = senhaHash;
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
