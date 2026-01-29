package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Usuario;
import br.com.fiapx.dominio.excecao.EmailJaCadastradoException;
import br.com.fiapx.dominio.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrarUsuario registrarUsuario;

    @BeforeEach
    void setUp() {
        registrarUsuario = new RegistrarUsuario(usuarioRepositorio, passwordEncoder);
    }

    @Test
    void deveRegistrarUsuarioComSucesso() {
        String email = "novo@email.com";
        String senha = "123456";
        String senhaHash = "$2a$10$hashgerado";

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(senha)).thenReturn(senhaHash);
        when(usuarioRepositorio.salvar(any())).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        Usuario resultado = registrarUsuario.executar(email, senha);

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());
        assertEquals(senhaHash, resultado.getSenhaHash());
    }

    @Test
    void deveHashearSenhaAntesdeSalvar() {
        String email = "novo@email.com";
        String senha = "minhaSenha";
        String senhaHash = "$2a$10$hashgerado";

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(senha)).thenReturn(senhaHash);
        when(usuarioRepositorio.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        registrarUsuario.executar(email, senha);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositorio).salvar(captor.capture());
        assertEquals(senhaHash, captor.getValue().getSenhaHash());
        verify(passwordEncoder).encode(senha);
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        String email = "existente@email.com";
        String senha = "123456";
        Usuario usuarioExistente = new Usuario(email, "hashExistente");

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.of(usuarioExistente));

        assertThrows(EmailJaCadastradoException.class, () ->
                registrarUsuario.executar(email, senha));

        verify(usuarioRepositorio, never()).salvar(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
