document.addEventListener('DOMContentLoaded', function () {
    // Se ja esta autenticado, redireciona
    if (localStorage.getItem('token')) {
        window.location.href = '/index.html';
        return;
    }

    var formLogin = document.getElementById('formLogin');
    var mensagemErro = document.getElementById('mensagemErro');
    var btnEntrar = document.getElementById('btnEntrar');

    formLogin.addEventListener('submit', function (e) {
        e.preventDefault();
        esconderErro();

        var email = document.getElementById('email').value.trim();
        var senha = document.getElementById('senha').value;

        if (!email || !senha) {
            mostrarErro('Preencha todos os campos.');
            return;
        }

        btnEntrar.disabled = true;
        btnEntrar.innerHTML = '<span class="spinner"></span> Entrando...';

        fetch('/api/autenticacao/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, senha: senha })
        })
        .then(function (resp) {
            if (!resp.ok) {
                if (resp.status === 401) {
                    throw new Error('E-mail ou senha incorretos.');
                }
                throw new Error('Erro ao realizar login. Tente novamente.');
            }
            return resp.json();
        })
        .then(function (data) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('email', email);
            window.location.href = '/index.html';
        })
        .catch(function (err) {
            mostrarErro(err.message);
        })
        .finally(function () {
            btnEntrar.disabled = false;
            btnEntrar.textContent = 'Entrar';
        });
    });

    function mostrarErro(msg) {
        mensagemErro.textContent = msg;
        mensagemErro.classList.add('visivel');
    }

    function esconderErro() {
        mensagemErro.textContent = '';
        mensagemErro.classList.remove('visivel');
    }
});
