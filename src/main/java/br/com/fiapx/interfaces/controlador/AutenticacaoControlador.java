package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.casosdeuso.AutenticarUsuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacao")
public class AutenticacaoControlador {

    private final AutenticarUsuario autenticarUsuario;

    public AutenticacaoControlador(AutenticarUsuario autenticarUsuario) {
        this.autenticarUsuario = autenticarUsuario;
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> login(@RequestBody RequisicaoLogin requisicao) {
        String token = autenticarUsuario.executar(requisicao.email(), requisicao.senha());
        return ResponseEntity.ok(new RespostaLogin(token));
    }

    public record RequisicaoLogin(String email, String senha) {}

    public record RespostaLogin(String token) {}
}
