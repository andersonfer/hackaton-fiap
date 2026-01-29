package br.com.fiapx.interfaces.excecao;

import br.com.fiapx.aplicacao.casosdeuso.AutenticarUsuario;
import br.com.fiapx.dominio.excecao.AcessoNegadoException;
import br.com.fiapx.dominio.excecao.VideoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorGlobalExcecoes {

    @ExceptionHandler(AutenticarUsuario.CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> tratarCredenciaisInvalidas(AutenticarUsuario.CredenciaisInvalidasException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResposta(e.getMessage()));
    }

    @ExceptionHandler(VideoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarVideoNaoEncontrado(VideoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResposta(e.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResposta> tratarAcessoNegado(AcessoNegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroResposta(e.getMessage()));
    }

    public record ErroResposta(String mensagem) {}
}
