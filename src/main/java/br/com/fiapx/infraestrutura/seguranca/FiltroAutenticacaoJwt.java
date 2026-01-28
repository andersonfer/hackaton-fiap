package br.com.fiapx.infraestrutura.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private final ServicoJwt servicoJwt;

    public FiltroAutenticacaoJwt(ServicoJwt servicoJwt) {
        this.servicoJwt = servicoJwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (servicoJwt.validarToken(token)) {
                Long usuarioId = servicoJwt.extrairUsuarioId(token);
                String email = servicoJwt.extrairEmail(token);

                UsuarioAutenticado usuario = new UsuarioAutenticado(usuarioId, email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
