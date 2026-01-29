document.addEventListener('DOMContentLoaded', function () {
    // Se ja esta autenticado, redireciona
    if (localStorage.getItem('token')) {
        window.location.href = '/index.html';
        return;
    }

    var formLogin = document.getElementById('formLogin');
    var mensagemErro = document.getElementById('mensagemErro');
    var mensagemSucesso = document.getElementById('mensagemSucesso');
    var btnSubmit = document.getElementById('btnSubmit');
    var linkAlternar = document.getElementById('linkAlternar');
    var titulo = document.querySelector('.login-card h1');
    var subtitulo = document.querySelector('.login-card .subtitulo');

    var modoRegistro = false;

    // Event delegation para o link que e recriado no innerHTML
    linkAlternar.addEventListener('click', function (e) {
        if (e.target.id === 'alternarModo') {
            e.preventDefault();
            alternarFormulario();
        }
    });

    function alternarFormulario() {
        modoRegistro = !modoRegistro;
        esconderErro();
        esconderSucesso();
        formLogin.reset();

        if (modoRegistro) {
            titulo.textContent = 'Criar Conta';
            subtitulo.textContent = 'Cadastre-se no FIAP X';
            btnSubmit.textContent = 'Registrar';
            linkAlternar.innerHTML = 'Ja tem conta? <a href="#" id="alternarModo">Entrar</a>';
        } else {
            titulo.textContent = 'FIAP X';
            subtitulo.textContent = 'Processador de Videos';
            btnSubmit.textContent = 'Entrar';
            linkAlternar.innerHTML = 'Nao tem conta? <a href="#" id="alternarModo">Criar conta</a>';
        }
    }

    function voltarParaLogin() {
        modoRegistro = false;
        titulo.textContent = 'FIAP X';
        subtitulo.textContent = 'Processador de Videos';
        btnSubmit.textContent = 'Entrar';
        linkAlternar.innerHTML = 'Nao tem conta? <a href="#" id="alternarModo">Criar conta</a>';
    }

    formLogin.addEventListener('submit', function (e) {
        e.preventDefault();
        esconderErro();
        esconderSucesso();

        var email = document.getElementById('email').value.trim();
        var senha = document.getElementById('senha').value;

        if (!email || !senha) {
            mostrarErro('Preencha todos os campos.');
            return;
        }

        if (modoRegistro) {
            registrar(email, senha);
        } else {
            login(email, senha);
        }
    });

    function login(email, senha) {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner"></span> Entrando...';

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
            btnSubmit.disabled = false;
            btnSubmit.textContent = 'Entrar';
        });
    }

    function registrar(email, senha) {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner"></span> Registrando...';

        fetch('/api/autenticacao/registrar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, senha: senha })
        })
        .then(function (resp) {
            if (!resp.ok) {
                if (resp.status === 409) {
                    throw new Error('Este e-mail ja esta cadastrado.');
                }
                throw new Error('Erro ao registrar. Tente novamente.');
            }
            return resp.json();
        })
        .then(function () {
            mostrarSucesso('Conta criada com sucesso! Faca login para continuar.');
            voltarParaLogin();
            document.getElementById('senha').value = '';
        })
        .catch(function (err) {
            mostrarErro(err.message);
        })
        .finally(function () {
            btnSubmit.disabled = false;
            btnSubmit.textContent = modoRegistro ? 'Registrar' : 'Entrar';
        });
    }

    function mostrarErro(msg) {
        mensagemErro.textContent = msg;
        mensagemErro.classList.add('visivel');
    }

    function esconderErro() {
        mensagemErro.textContent = '';
        mensagemErro.classList.remove('visivel');
    }

    function mostrarSucesso(msg) {
        mensagemSucesso.textContent = msg;
        mensagemSucesso.classList.add('visivel');
    }

    function esconderSucesso() {
        mensagemSucesso.textContent = '';
        mensagemSucesso.classList.remove('visivel');
    }
});
