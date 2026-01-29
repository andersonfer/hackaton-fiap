package br.com.fiapx.interfaces.controlador;

import br.com.fiapx.aplicacao.casosdeuso.AutenticarUsuario;
import br.com.fiapx.aplicacao.casosdeuso.RegistrarUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacao")
public class AutenticacaoControlador {

    private final AutenticarUsuario autenticarUsuario;
    private final RegistrarUsuario registrarUsuario;

    public AutenticacaoControlador(AutenticarUsuario autenticarUsuario, RegistrarUsuario registrarUsuario) {
        this.autenticarUsuario = autenticarUsuario;
        this.registrarUsuario = registrarUsuario;
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> login(@RequestBody RequisicaoLogin requisicao) {
        String token = autenticarUsuario.executar(requisicao.email(), requisicao.senha());
        return ResponseEntity.ok(new RespostaLogin(token));
    }

    @PostMapping("/registrar")
    public ResponseEntity<RespostaRegistro> registrar(@RequestBody RequisicaoLogin requisicao) {
        registrarUsuario.executar(requisicao.email(), requisicao.senha());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespostaRegistro("Usuário registrado com sucesso"));
    }

    public record RequisicaoLogin(String email, String senha) {}

    public record RespostaLogin(String token) {}

    public record RespostaRegistro(String mensagem) {}
}
