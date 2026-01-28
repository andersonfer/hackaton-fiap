package br.com.fiapx.infraestrutura.mensageria;

import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.enums.StatusVideo;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Teste de integracao para validar processamento paralelo de videos com RabbitMQ.
 *
 * Requisitos:
 * - Docker deve estar instalado e rodando
 * - O teste sera ignorado automaticamente se Docker nao estiver disponivel
 *
 * Para executar: mvn test -Dtest=ProcessamentoParaleloIntegracaoTest
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIf("isDockerAvailable")
class ProcessamentoParaleloIntegracaoTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            System.out.println("Docker nao disponivel, ignorando testes com Testcontainers: " + e.getMessage());
            return false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("app.mensageria.habilitado", () -> "true");
        registry.add("app.mensageria.concorrencia", () -> "3");
        registry.add("app.mensageria.fila-processamento", () -> "fila.video.processamento.test");
        // Desabilita autoconfiguracao exclusion do application-test.yml
        registry.add("spring.autoconfigure.exclude", () -> "");
    }

    @Autowired
    private PublicadorMensagemVideo publicadorMensagemVideo;

    @Autowired
    private VideoRepositorio videoRepositorio;

    @MockBean
    private ProcessadorVideoGateway processadorGateway;

    private AtomicInteger processamentosSimultaneos;
    private AtomicInteger maxProcessamentosSimultaneos;

    @BeforeEach
    void setUp() {
        processamentosSimultaneos = new AtomicInteger(0);
        maxProcessamentosSimultaneos = new AtomicInteger(0);
    }

    @Test
    void deveProcessar3VideosEmParalelo() throws Exception {
        // Arrange: criar 3 videos PENDENTE
        Video video1 = criarVideo("video1.mp4");
        Video video2 = criarVideo("video2.mp4");
        Video video3 = criarVideo("video3.mp4");

        // Mock do processador que simula processamento de 500ms
        // e rastreia quantos estao executando simultaneamente
        when(processadorGateway.processarVideo(any(Path.class), anyLong()))
                .thenAnswer(invocation -> {
                    int atual = processamentosSimultaneos.incrementAndGet();
                    // Atualiza maximo se necessario
                    maxProcessamentosSimultaneos.updateAndGet(max -> Math.max(max, atual));

                    try {
                        Thread.sleep(500); // Simula processamento
                    } finally {
                        processamentosSimultaneos.decrementAndGet();
                    }

                    Long videoId = invocation.getArgument(1);
                    return Paths.get("/tmp/fiapx-test/zips/" + videoId + ".zip");
                });

        // Act: publicar 3 mensagens simultaneamente
        long inicio = System.currentTimeMillis();

        publicadorMensagemVideo.publicar(video1.getId(), video1.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video2.getId(), video2.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video3.getId(), video3.getCaminhoArquivo());

        // Aguarda processamento (polling)
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> todosProcessados(video1.getId(), video2.getId(), video3.getId()));

        long duracao = System.currentTimeMillis() - inicio;

        // Assert: todos devem estar CONCLUIDO
        Video v1Final = videoRepositorio.buscarPorId(video1.getId()).orElseThrow();
        Video v2Final = videoRepositorio.buscarPorId(video2.getId()).orElseThrow();
        Video v3Final = videoRepositorio.buscarPorId(video3.getId()).orElseThrow();

        assertThat(v1Final.getStatus()).isEqualTo(StatusVideo.CONCLUIDO);
        assertThat(v2Final.getStatus()).isEqualTo(StatusVideo.CONCLUIDO);
        assertThat(v3Final.getStatus()).isEqualTo(StatusVideo.CONCLUIDO);

        // Verifica paralelismo: 3 videos de 500ms cada
        // Se sequencial: ~1500ms, se paralelo: ~500-800ms
        // Usamos margem de seguranca de 1200ms
        assertThat(duracao)
                .as("Duracao total deve indicar processamento paralelo (< 1200ms)")
                .isLessThan(1200);

        // Verifica que pelo menos 2 processamentos ocorreram simultaneamente
        assertThat(maxProcessamentosSimultaneos.get())
                .as("Deve ter pelo menos 2 processamentos simultaneos")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void deveProcessar6VideosRespeitandoConcorrencia3() throws Exception {
        // Arrange: criar 6 videos PENDENTE
        Video video1 = criarVideo("video1.mp4");
        Video video2 = criarVideo("video2.mp4");
        Video video3 = criarVideo("video3.mp4");
        Video video4 = criarVideo("video4.mp4");
        Video video5 = criarVideo("video5.mp4");
        Video video6 = criarVideo("video6.mp4");

        // Mock do processador que simula processamento de 300ms
        when(processadorGateway.processarVideo(any(Path.class), anyLong()))
                .thenAnswer(invocation -> {
                    int atual = processamentosSimultaneos.incrementAndGet();
                    maxProcessamentosSimultaneos.updateAndGet(max -> Math.max(max, atual));

                    try {
                        Thread.sleep(300);
                    } finally {
                        processamentosSimultaneos.decrementAndGet();
                    }

                    Long videoId = invocation.getArgument(1);
                    return Paths.get("/tmp/fiapx-test/zips/" + videoId + ".zip");
                });

        // Act: publicar 6 mensagens
        long inicio = System.currentTimeMillis();

        publicadorMensagemVideo.publicar(video1.getId(), video1.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video2.getId(), video2.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video3.getId(), video3.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video4.getId(), video4.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video5.getId(), video5.getCaminhoArquivo());
        publicadorMensagemVideo.publicar(video6.getId(), video6.getCaminhoArquivo());

        // Aguarda processamento
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> todosProcessados(
                        video1.getId(), video2.getId(), video3.getId(),
                        video4.getId(), video5.getId(), video6.getId()));

        long duracao = System.currentTimeMillis() - inicio;

        // Assert: todos CONCLUIDO
        assertThat(videoRepositorio.buscarPorId(video1.getId()).get().getStatus())
                .isEqualTo(StatusVideo.CONCLUIDO);
        assertThat(videoRepositorio.buscarPorId(video6.getId()).get().getStatus())
                .isEqualTo(StatusVideo.CONCLUIDO);

        // 6 videos de 300ms com concorrencia 3 = 2 lotes = ~600ms
        // Sequencial seria ~1800ms
        // Com margem: < 1200ms indica paralelismo
        assertThat(duracao)
                .as("6 videos devem processar em ~2 lotes paralelos")
                .isLessThan(1200);

        // Verifica que concorrencia maxima foi 3 (ou proximo)
        assertThat(maxProcessamentosSimultaneos.get())
                .as("Concorrencia maxima deve ser 3")
                .isLessThanOrEqualTo(3);
    }

    private Video criarVideo(String nomeOriginal) {
        Video video = new Video(1L, nomeOriginal, "/tmp/fiapx-test/videos/" + nomeOriginal);
        return videoRepositorio.salvar(video);
    }

    private boolean todosProcessados(Long... ids) {
        return Arrays.stream(ids)
                .map(id -> videoRepositorio.buscarPorId(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .allMatch(v -> v.getStatus() != StatusVideo.PENDENTE
                        && v.getStatus() != StatusVideo.PROCESSANDO);
    }
}
