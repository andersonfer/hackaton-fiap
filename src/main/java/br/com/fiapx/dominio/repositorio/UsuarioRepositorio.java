package br.com.fiapx.dominio.repositorio;

import br.com.fiapx.dominio.entidade.Usuario;

import java.util.Optional;

public interface UsuarioRepositorio {

    Optional<Usuario> buscarPorEmail(String email);

    Usuario salvar(Usuario usuario);
}
