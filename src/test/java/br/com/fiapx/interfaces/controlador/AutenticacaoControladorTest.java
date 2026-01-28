package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.infraestrutura.persistencia.entidade.UsuarioEntidade;
import br.com.fiapx.infraestrutura.persistencia.repositorio.UsuarioRepositorioJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AutenticacaoControladorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepositorioJpa usuarioRepositorioJpa;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private FilaMensagemGateway filaMensagemGateway;

    private RestTemplate restTemplate;

    private static final String EMAIL_TESTE = "teste@email.com";
    private static final String SENHA_TESTE = "123456";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        UsuarioEntidade usuario = new UsuarioEntidade();
        usuario.setEmail(EMAIL_TESTE);
        usuario.setSenhaHash(passwordEncoder.encode(SENHA_TESTE));
        usuarioRepositorioJpa.save(usuario);
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void deveRetornarTokenQuandoCredenciaisValidas() {
        AutenticacaoControlador.RequisicaoLogin requisicao =
                new AutenticacaoControlador.RequisicaoLogin(EMAIL_TESTE, SENHA_TESTE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AutenticacaoControlador.RequisicaoLogin> request = new HttpEntity<>(requisicao, headers);

        ResponseEntity<AutenticacaoControlador.RespostaLogin> resposta =
                restTemplate.exchange(getBaseUrl() + "/api/autenticacao/login", HttpMethod.POST, request,
                        AutenticacaoControlador.RespostaLogin.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertNotNull(resposta.getBody().token());
        assertFalse(resposta.getBody().token().isEmpty());
    }

    @Test
    void deveRetornar401QuandoUsuarioNaoExiste() {
        AutenticacaoControlador.RequisicaoLogin requisicao =
                new AutenticacaoControlador.RequisicaoLogin("inexistente@email.com", SENHA_TESTE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AutenticacaoControlador.RequisicaoLogin> request = new HttpEntity<>(requisicao, headers);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.exchange(getBaseUrl() + "/api/autenticacao/login",
                        HttpMethod.POST, request, String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void deveRetornar401QuandoSenhaIncorreta() {
        AutenticacaoControlador.RequisicaoLogin requisicao =
                new AutenticacaoControlador.RequisicaoLogin(EMAIL_TESTE, "senhaErrada");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AutenticacaoControlador.RequisicaoLogin> request = new HttpEntity<>(requisicao, headers);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.exchange(getBaseUrl() + "/api/autenticacao/login",
                        HttpMethod.POST, request, String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void deveRetornarErroQuandoBodyVazio() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(null, headers);

        assertThrows(HttpClientErrorException.class,
                () -> restTemplate.exchange(getBaseUrl() + "/api/autenticacao/login",
                        HttpMethod.POST, request, String.class));
    }
}
