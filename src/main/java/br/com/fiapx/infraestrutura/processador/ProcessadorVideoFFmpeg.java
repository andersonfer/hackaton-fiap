package br.com.fiapx.infraestrutura.processador;

import br.com.fiapx.aplicacao.gateway.ProcessadorVideoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ProcessadorVideoFFmpeg implements ProcessadorVideoGateway {

    @Value("${app.armazenamento.diretorio-frames}")
    private String diretorioFrames;

    @Value("${app.armazenamento.diretorio-zips}")
    private String diretorioZips;

    @PostConstruct
    public void inicializar() throws IOException {
        Files.createDirectories(Paths.get(diretorioFrames));
        Files.createDirectories(Paths.get(diretorioZips));
    }

    @Override
    public Path processarVideo(Path caminhoVideo, Long videoId) {
        Path diretorioFramesVideo = Paths.get(diretorioFrames, videoId.toString());
        Path caminhoZip = Paths.get(diretorioZips, videoId + ".zip");

        try {
            Files.createDirectories(diretorioFramesVideo);
            extrairFrames(caminhoVideo, diretorioFramesVideo);
            criarZip(diretorioFramesVideo, caminhoZip);
            limparDiretorioFrames(diretorioFramesVideo);
            return caminhoZip;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Erro ao processar video: " + e.getMessage(), e);
        }
    }

    private void extrairFrames(Path caminhoVideo, Path diretorioSaida) throws IOException, InterruptedException {
        String comando = String.format(
                "ffmpeg -i %s -vf fps=1 %s/frame_%%04d.png",
                caminhoVideo.toAbsolutePath(),
                diretorioSaida.toAbsolutePath()
        );

        ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", comando);
        processBuilder.redirectErrorStream(true);
        Process processo = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(processo.getInputStream()))) {
            while (reader.readLine() != null) {
                // Consome a saida do processo
            }
        }

        int exitCode = processo.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg retornou codigo de erro: " + exitCode);
        }
    }

    private void criarZip(Path diretorioOrigem, Path arquivoZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(arquivoZip))) {
            Files.walk(diretorioOrigem)
                    .filter(Files::isRegularFile)
                    .forEach(arquivo -> {
                        try {
                            ZipEntry entry = new ZipEntry(arquivo.getFileName().toString());
                            zos.putNextEntry(entry);
                            Files.copy(arquivo, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Erro ao adicionar arquivo ao zip: " + e.getMessage(), e);
                        }
                    });
        }
    }

    private void limparDiretorioFrames(Path diretorio) throws IOException {
        Files.walk(diretorio)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignora erros ao limpar
                    }
                });
    }
}
