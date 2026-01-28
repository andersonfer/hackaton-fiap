package br.com.fiapx.infraestrutura.persistencia.repositorio;

import br.com.fiapx.dominio.entidade.Usuario;
import br.com.fiapx.dominio.repositorio.UsuarioRepositorio;
import br.com.fiapx.infraestrutura.persistencia.entidade.UsuarioEntidade;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositorioImpl implements UsuarioRepositorio {

    private final UsuarioRepositorioJpa repositorioJpa;

    public UsuarioRepositorioImpl(UsuarioRepositorioJpa repositorioJpa) {
        this.repositorioJpa = repositorioJpa;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return repositorioJpa.findByEmail(email)
                .map(this::paraDominio);
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        UsuarioEntidade entidade = paraEntidade(usuario);
        UsuarioEntidade salvo = repositorioJpa.save(entidade);
        return paraDominio(salvo);
    }

    private Usuario paraDominio(UsuarioEntidade entidade) {
        Usuario usuario = new Usuario();
        usuario.setId(entidade.getId());
        usuario.setEmail(entidade.getEmail());
        usuario.setSenhaHash(entidade.getSenhaHash());
        usuario.setCriadoEm(entidade.getCriadoEm());
        return usuario;
    }

    private UsuarioEntidade paraEntidade(Usuario usuario) {
        UsuarioEntidade entidade = new UsuarioEntidade();
        entidade.setId(usuario.getId());
        entidade.setEmail(usuario.getEmail());
        entidade.setSenhaHash(usuario.getSenhaHash());
        entidade.setCriadoEm(usuario.getCriadoEm());
        return entidade;
    }
}
