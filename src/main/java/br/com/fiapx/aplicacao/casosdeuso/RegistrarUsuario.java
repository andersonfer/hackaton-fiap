package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Usuario;
import br.com.fiapx.dominio.excecao.EmailJaCadastradoException;
import br.com.fiapx.dominio.repositorio.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrarUsuario {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    public RegistrarUsuario(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario executar(String email, String senha) {
        usuarioRepositorio.buscarPorEmail(email).ifPresent(u -> {
            throw new EmailJaCadastradoException();
        });

        String senhaHash = passwordEncoder.encode(senha);
        Usuario usuario = new Usuario(email, senhaHash);
        return usuarioRepositorio.salvar(usuario);
    }
}
