package br.com.fiapx.aplicacao.casosdeuso;

import br.com.fiapx.dominio.entidade.Video;
import br.com.fiapx.dominio.repositorio.VideoRepositorio;

import java.util.List;

public class ListarVideos {

    private final VideoRepositorio videoRepositorio;

    public ListarVideos(VideoRepositorio videoRepositorio) {
        this.videoRepositorio = videoRepositorio;
    }

    public List<Video> executar() {
        return videoRepositorio.listarTodos();
    }
}
