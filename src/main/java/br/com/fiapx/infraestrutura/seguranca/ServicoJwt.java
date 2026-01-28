package br.com.fiapx.infraestrutura.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class ServicoJwt {

    private final SecretKey chave;
    private final long expiracaoMs;

    public ServicoJwt(
            @Value("${app.jwt.segredo}") String segredo,
            @Value("${app.jwt.expiracao-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public Long extrairUsuarioId(String token) {
        Claims claims = extrairClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String extrairEmail(String token) {
        Claims claims = extrairClaims(token);
        return claims.get("email", String.class);
    }

    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
