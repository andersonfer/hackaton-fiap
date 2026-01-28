package br.com.fiapx.infraestrutura.armazenamento;

import br.com.fiapx.aplicacao.gateway.ArmazenamentoArquivoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class ArmazenamentoArquivoLocal implements ArmazenamentoArquivoGateway {

    @Value("${app.armazenamento.diretorio-videos}")
    private String diretorioVideos;

    @Value("${app.armazenamento.diretorio-zips}")
    private String diretorioZips;

    @PostConstruct
    public void inicializar() throws IOException {
        Files.createDirectories(Paths.get(diretorioVideos));
        Files.createDirectories(Paths.get(diretorioZips));
    }

    @Override
    public Path salvarVideo(String nomeArquivo, InputStream conteudo) {
        try {
            Path destino = Paths.get(diretorioVideos, System.currentTimeMillis() + "_" + nomeArquivo);
            Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
            return destino;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar video: " + e.getMessage(), e);
        }
    }

    @Override
    public Path obterCaminhoZip(Long videoId) {
        return Paths.get(diretorioZips, videoId + ".zip");
    }

    @Override
    public byte[] lerArquivo(Path caminho) {
        try {
            return Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletarArquivo(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao deletar arquivo: " + e.getMessage(), e);
        }
    }
}
