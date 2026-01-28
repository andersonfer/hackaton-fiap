package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Usuario;
import br.com.fiapx.dominio.repositorio.UsuarioRepositorio;
import br.com.fiapx.infraestrutura.seguranca.ServicoJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ServicoJwt servicoJwt;

    private AutenticarUsuario autenticarUsuario;

    @BeforeEach
    void setUp() {
        autenticarUsuario = new AutenticarUsuario(usuarioRepositorio, passwordEncoder, servicoJwt);
    }

    @Test
    void deveAutenticarUsuarioComCredenciaisValidas() {
        String email = "teste@email.com";
        String senha = "123456";
        String senhaHash = "hash123";
        String tokenEsperado = "jwt.token.aqui";

        Usuario usuario = new Usuario(email, senhaHash);
        usuario.setId(1L);

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senha, senhaHash)).thenReturn(true);
        when(servicoJwt.gerarToken(1L, email)).thenReturn(tokenEsperado);

        String token = autenticarUsuario.executar(email, senha);

        assertEquals(tokenEsperado, token);
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        String email = "inexistente@email.com";
        String senha = "123456";

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.empty());

        assertThrows(AutenticarUsuario.CredenciaisInvalidasException.class,
                () -> autenticarUsuario.executar(email, senha));
    }

    @Test
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        String email = "teste@email.com";
        String senha = "senhaErrada";
        String senhaHash = "hash123";

        Usuario usuario = new Usuario(email, senhaHash);
        usuario.setId(1L);

        when(usuarioRepositorio.buscarPorEmail(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senha, senhaHash)).thenReturn(false);

        assertThrows(AutenticarUsuario.CredenciaisInvalidasException.class,
                () -> autenticarUsuario.executar(email, senha));
    }
}
