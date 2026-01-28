package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Usuario;
import br.com.fiapx.dominio.repositorio.UsuarioRepositorio;
import br.com.fiapx.infraestrutura.seguranca.ServicoJwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUsuario {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ServicoJwt servicoJwt;

    public AutenticarUsuario(UsuarioRepositorio usuarioRepositorio,
                             PasswordEncoder passwordEncoder,
                             ServicoJwt servicoJwt) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.servicoJwt = servicoJwt;
    }

    public String executar(String email, String senha) {
        Usuario usuario = usuarioRepositorio.buscarPorEmail(email)
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }

        return servicoJwt.gerarToken(usuario.getId(), usuario.getEmail());
    }

    public static class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException(String mensagem) {
            super(mensagem);
        }
    }
}
