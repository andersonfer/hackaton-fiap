package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.gateway.FilaMensagemGateway;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.infraestrutura.persistencia.entidade.UsuarioEntidade;
import br.com.fiapx.infraestrutura.persistencia.entidade.VideoEntidade;
import br.com.fiapx.infraestrutura.persistencia.repositorio.UsuarioRepositorioJpa;
import br.com.fiapx.infraestrutura.persistencia.repositorio.VideoJpaRepositorio;
import br.com.fiapx.interfaces.dto.resposta.VideoResposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FluxoCompletoTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UsuarioRepositorioJpa usuarioRepositorioJpa;

    @Autowired
    private VideoJpaRepositorio videoRepositorioJpa;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private FilaMensagemGateway filaMensagemGateway;

    private static final String EMAIL_TESTE = "usuario-e2e@email.com";
    private static final String SENHA_TESTE = "senha123";

    @BeforeEach
    void setUp() {
        UsuarioEntidade usuario = new UsuarioEntidade();
        usuario.setEmail(EMAIL_TESTE);
        usuario.setSenhaHash(passwordEncoder.encode(SENHA_TESTE));
        usuarioRepositorioJpa.save(usuario);
    }

    @Test
    void fluxoCompleto_Login_EnviarVideo_ListarVideos_SimularProcessamento_Download() throws IOException {
        // 1. Login
        String token = realizarLogin(EMAIL_TESTE, SENHA_TESTE);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // 2. Enviar vídeo
        VideoResposta videoEnviado = enviarVideo(token, "meu-video.mp4");
        assertNotNull(videoEnviado);
        assertNotNull(videoEnviado.id());
        assertEquals("meu-video.mp4", videoEnviado.nomeOriginal());
        assertEquals(StatusVideo.PENDENTE, videoEnviado.status());
        assertNull(videoEnviado.urlDownload()); // Não deve ter URL enquanto não processar

        // 3. Listar vídeos - deve aparecer o vídeo enviado
        List<VideoResposta> videos = listarVideos(token);
        assertEquals(1, videos.size());
        assertEquals(videoEnviado.id(), videos.get(0).id());
        assertEquals(StatusVideo.PENDENTE, videos.get(0).status());

        // 4. Simular processamento (em produção isso seria feito pelo consumidor RabbitMQ)
        Path zipPath = simularProcessamento(videoEnviado.id());

        // 5. Listar vídeos novamente - deve mostrar status CONCLUIDO
        List<VideoResposta> videosAposProcessamento = listarVideos(token);
        assertEquals(1, videosAposProcessamento.size());
        assertEquals(StatusVideo.CONCLUIDO, videosAposProcessamento.get(0).status());
        assertNotNull(videosAposProcessamento.get(0).urlDownload());

        // 6. Download do ZIP
        byte[] zipContent = baixarVideo(token, videoEnviado.id());
        assertNotNull(zipContent);
        assertTrue(zipContent.length > 0);
    }

    @Test
    void fluxoCompleto_UsuariosSeparados_NaoCompartilhamVideos() {
        // Criar segundo usuário
        String emailUsuario2 = "usuario2@email.com";
        UsuarioEntidade usuario2 = new UsuarioEntidade();
        usuario2.setEmail(emailUsuario2);
        usuario2.setSenhaHash(passwordEncoder.encode(SENHA_TESTE));
        usuarioRepositorioJpa.save(usuario2);

        // Login de ambos usuários
        String tokenUsuario1 = realizarLogin(EMAIL_TESTE, SENHA_TESTE);
        String tokenUsuario2 = realizarLogin(emailUsuario2, SENHA_TESTE);

        // Usuário 1 envia vídeos
        enviarVideo(tokenUsuario1, "video-usuario1-a.mp4");
        enviarVideo(tokenUsuario1, "video-usuario1-b.mp4");

        // Usuário 2 envia vídeo
        enviarVideo(tokenUsuario2, "video-usuario2.mp4");

        // Verificar isolamento - Usuário 1 só vê seus vídeos
        List<VideoResposta> videosUsuario1 = listarVideos(tokenUsuario1);
        assertEquals(2, videosUsuario1.size());
        assertTrue(videosUsuario1.stream()
                .allMatch(v -> v.nomeOriginal().startsWith("video-usuario1")));

        // Usuário 2 só vê seu vídeo
        List<VideoResposta> videosUsuario2 = listarVideos(tokenUsuario2);
        assertEquals(1, videosUsuario2.size());
        assertEquals("video-usuario2.mp4", videosUsuario2.get(0).nomeOriginal());
    }

    @Test
    void fluxoCompleto_VideoComFalha_DeveExibirMensagemErro() {
        String token = realizarLogin(EMAIL_TESTE, SENHA_TESTE);

        // Enviar vídeo
        VideoResposta videoEnviado = enviarVideo(token, "video-com-problema.mp4");

        // Simular falha no processamento
        simularFalhaProcessamento(videoEnviado.id(), "Erro ao extrair frames: formato inválido");

        // Verificar que status é FALHA e mensagem de erro está presente
        List<VideoResposta> videos = listarVideos(token);
        assertEquals(1, videos.size());
        assertEquals(StatusVideo.FALHA, videos.get(0).status());
        assertEquals("Erro ao extrair frames: formato inválido", videos.get(0).mensagemErro());
        assertNull(videos.get(0).urlDownload());
    }

    private String realizarLogin(String email, String senha) {
        AutenticacaoControlador.RequisicaoLogin requisicao =
                new AutenticacaoControlador.RequisicaoLogin(email, senha);

        ResponseEntity<AutenticacaoControlador.RespostaLogin> resposta =
                restTemplate.postForEntity("/api/autenticacao/login", requisicao,
                        AutenticacaoControlador.RespostaLogin.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta.getBody().token();
    }

    private VideoResposta enviarVideo(String token, String nomeArquivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("video", new ByteArrayResource("conteudo fake de video para teste".getBytes()) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<VideoResposta> resposta =
                restTemplate.postForEntity("/api/videos/enviar", request, VideoResposta.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta.getBody();
    }

    private List<VideoResposta> listarVideos(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List<VideoResposta>> resposta = restTemplate.exchange(
                "/api/videos",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta.getBody();
    }

    private byte[] baixarVideo(String token, Long videoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<byte[]> resposta = restTemplate.exchange(
                "/api/videos/" + videoId + "/baixar",
                HttpMethod.GET,
                request,
                byte[].class
        );

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta.getBody();
    }

    private Path simularProcessamento(Long videoId) throws IOException {
        Path diretorioZips = Paths.get("/tmp/fiapx-test/zips");
        Files.createDirectories(diretorioZips);
        Path zipPath = diretorioZips.resolve(videoId + ".zip");
        Files.write(zipPath, "conteudo simulado do arquivo zip com frames".getBytes());

        Optional<VideoEntidade> videoOpt = videoRepositorioJpa.findById(videoId);
        assertTrue(videoOpt.isPresent());

        VideoEntidade video = videoOpt.get();
        video.setStatus(StatusVideo.CONCLUIDO);
        video.setCaminhoZip(zipPath.toString());
        videoRepositorioJpa.save(video);

        return zipPath;
    }

    private void simularFalhaProcessamento(Long videoId, String mensagemErro) {
        Optional<VideoEntidade> videoOpt = videoRepositorioJpa.findById(videoId);
        assertTrue(videoOpt.isPresent());

        VideoEntidade video = videoOpt.get();
        video.setStatus(StatusVideo.FALHA);
        video.setMensagemErro(mensagemErro);
        videoRepositorioJpa.save(video);
    }
}
