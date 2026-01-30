document.addEventListener('DOMContentLoaded', function () {
    var token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }

    // Elementos
    var emailUsuario = document.getElementById('emailUsuario');
    var btnSair = document.getElementById('btnSair');
    var inputArquivos = document.getElementById('inputArquivos');
    var zonaUpload = document.getElementById('zonaUpload');
    var arquivosSelecionados = document.getElementById('arquivosSelecionados');
    var listaArquivos = document.getElementById('listaArquivos');
    var contadorArquivos = document.getElementById('contadorArquivos');
    var btnEnviar = document.getElementById('btnEnviar');
    var progressoUpload = document.getElementById('progressoUpload');
    var barraProgresso = document.getElementById('barraProgresso');
    var textoProgresso = document.getElementById('textoProgresso');
    var mensagemErroUpload = document.getElementById('mensagemErroUpload');
    var corpoTabela = document.getElementById('corpoTabela');
    var tabelaVideos = document.getElementById('tabelaVideos');
    var mensagemVazia = document.getElementById('mensagemVazia');

    var arquivosParaEnviar = [];
    var intervaloAtualizacao = null;
    var LIMITE_TOTAL_BYTES = 1024 * 1024 * 1024; // 1GB

    // Mostrar email do usuario
    emailUsuario.textContent = localStorage.getItem('email') || '';

    // Logout
    btnSair.addEventListener('click', function () {
        localStorage.removeItem('token');
        localStorage.removeItem('email');
        window.location.href = '/login.html';
    });

    // Drag and drop
    zonaUpload.addEventListener('dragover', function (e) {
        e.preventDefault();
        zonaUpload.classList.add('arrastando');
    });

    zonaUpload.addEventListener('dragleave', function () {
        zonaUpload.classList.remove('arrastando');
    });

    zonaUpload.addEventListener('drop', function (e) {
        e.preventDefault();
        zonaUpload.classList.remove('arrastando');
        var files = Array.from(e.dataTransfer.files).filter(function (f) {
            return f.type.startsWith('video/');
        });
        adicionarArquivos(files);
    });

    // Selecao de arquivos
    inputArquivos.addEventListener('change', function () {
        adicionarArquivos(Array.from(inputArquivos.files));
        inputArquivos.value = '';
    });

    function adicionarArquivos(novos) {
        var ignorados = 0;
        for (var i = 0; i < novos.length; i++) {
            if (arquivosParaEnviar.length >= 20) {
                ignorados += novos.length - i;
                break;
            }
            // Evitar duplicatas pelo nome
            var duplicado = false;
            for (var j = 0; j < arquivosParaEnviar.length; j++) {
                if (arquivosParaEnviar[j].name === novos[i].name) {
                    duplicado = true;
                    break;
                }
            }
            if (!duplicado) {
                arquivosParaEnviar.push(novos[i]);
            }
        }
        renderizarArquivosSelecionados();
        if (ignorados > 0) {
            mostrarErroUpload('Limite de 20 videos por envio. ' + ignorados + (ignorados === 1 ? ' arquivo foi ignorado.' : ' arquivos foram ignorados.'));
        }
    }

    function removerArquivo(index) {
        arquivosParaEnviar.splice(index, 1);
        renderizarArquivosSelecionados();
    }

    function formatarTamanho(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    }

    function calcularTamanhoTotal() {
        var total = 0;
        for (var i = 0; i < arquivosParaEnviar.length; i++) {
            total += arquivosParaEnviar[i].size;
        }
        return total;
    }

    function renderizarArquivosSelecionados() {
        esconderErroUpload();

        if (arquivosParaEnviar.length === 0) {
            arquivosSelecionados.classList.remove('visivel');
            btnEnviar.disabled = true;
            return;
        }

        arquivosSelecionados.classList.add('visivel');
        var tamanhoTotal = calcularTamanhoTotal();
        var excedeLimite = tamanhoTotal > LIMITE_TOTAL_BYTES;

        if (excedeLimite) {
            mostrarErroUpload('Tamanho total (' + formatarTamanho(tamanhoTotal) + ') excede o limite de 500 MB. Remova alguns arquivos antes de enviar.');
        }

        btnEnviar.disabled = excedeLimite;

        var html = '';
        for (var i = 0; i < arquivosParaEnviar.length; i++) {
            var f = arquivosParaEnviar[i];
            html += '<div class="arquivo-item">' +
                '<span class="arquivo-item-nome">' + escapeHtml(f.name) + '</span>' +
                '<span class="arquivo-item-tamanho">' + formatarTamanho(f.size) + '</span>' +
                '<button class="arquivo-item-remover" data-index="' + i + '" title="Remover">&times;</button>' +
                '</div>';
        }
        listaArquivos.innerHTML = html;
        contadorArquivos.textContent = arquivosParaEnviar.length + ' de 20 arquivos';

        // Bind botoes remover
        var botoes = listaArquivos.querySelectorAll('.arquivo-item-remover');
        for (var k = 0; k < botoes.length; k++) {
            botoes[k].addEventListener('click', function () {
                removerArquivo(parseInt(this.getAttribute('data-index')));
            });
        }
    }

    // Enviar arquivos
    btnEnviar.addEventListener('click', function () {
        if (arquivosParaEnviar.length === 0) return;
        enviarArquivos();
    });

    function enviarArquivos() {
        esconderErroUpload();
        btnEnviar.disabled = true;
        inputArquivos.disabled = true;
        zonaUpload.classList.add('desabilitado');
        progressoUpload.classList.add('visivel');
        barraProgresso.style.width = '0%';

        var formData = new FormData();
        for (var i = 0; i < arquivosParaEnviar.length; i++) {
            formData.append('videos', arquivosParaEnviar[i]);
        }

        var xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/videos/enviar-lote', true);
        xhr.setRequestHeader('Authorization', 'Bearer ' + token);

        xhr.upload.addEventListener('progress', function (e) {
            if (e.lengthComputable) {
                var pct = Math.round((e.loaded / e.total) * 100);
                barraProgresso.style.width = pct + '%';
                textoProgresso.textContent = 'Enviando... ' + pct + '%';
            }
        });

        xhr.addEventListener('load', function () {
            progressoUpload.classList.remove('visivel');
            btnEnviar.disabled = false;
            inputArquivos.disabled = false;
            zonaUpload.classList.remove('desabilitado');

            if (xhr.status === 200) {
                arquivosParaEnviar = [];
                renderizarArquivosSelecionados();
                carregarVideos();
            } else if (xhr.status === 401 || xhr.status === 403) {
                localStorage.removeItem('token');
                localStorage.removeItem('email');
                window.location.href = '/login.html';
            } else if (xhr.status === 400) {
                mostrarErroUpload('Selecione no maximo 20 arquivos de video.');
            } else {
                mostrarErroUpload('Erro ao enviar videos. Tente novamente.');
            }
        });

        xhr.addEventListener('error', function () {
            progressoUpload.classList.remove('visivel');
            btnEnviar.disabled = false;
            inputArquivos.disabled = false;
            zonaUpload.classList.remove('desabilitado');
            mostrarErroUpload('Erro de conexao. Verifique sua rede e tente novamente.');
        });

        xhr.send(formData);
    }

    function mostrarErroUpload(msg) {
        mensagemErroUpload.textContent = msg;
        mensagemErroUpload.classList.add('visivel');
    }

    function esconderErroUpload() {
        mensagemErroUpload.textContent = '';
        mensagemErroUpload.classList.remove('visivel');
    }

    // Carregar lista de videos
    function carregarVideos() {
        fetch('/api/videos', {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(function (resp) {
            if (resp.status === 401 || resp.status === 403) {
                localStorage.removeItem('token');
                localStorage.removeItem('email');
                window.location.href = '/login.html';
                return null;
            }
            if (!resp.ok) throw new Error('Erro ao carregar videos');
            return resp.json();
        })
        .then(function (videos) {
            if (videos === null) return;
            renderizarTabela(videos);
        })
        .catch(function () {
            // Silencioso - tenta novamente no proximo ciclo
        });
    }

    function renderizarTabela(videos) {
        if (!videos || videos.length === 0) {
            tabelaVideos.style.display = 'none';
            mensagemVazia.style.display = 'block';
            return;
        }

        tabelaVideos.style.display = 'table';
        mensagemVazia.style.display = 'none';

        var html = '';
        for (var i = 0; i < videos.length; i++) {
            var v = videos[i];
            html += '<tr>' +
                '<td>' + escapeHtml(v.nomeOriginal) + '</td>' +
                '<td>' + renderizarStatus(v.status) + '</td>' +
                '<td>' + formatarData(v.criadoEm) + '</td>' +
                '<td>' + renderizarAcao(v) + '</td>' +
                '</tr>';
        }
        corpoTabela.innerHTML = html;

        // Bind botoes download
        var btnsDownload = corpoTabela.querySelectorAll('.btn-download');
        for (var k = 0; k < btnsDownload.length; k++) {
            btnsDownload[k].addEventListener('click', function () {
                baixarVideo(this.getAttribute('data-url'), this.getAttribute('data-nome'));
            });
        }
    }

    function renderizarStatus(status) {
        var labels = {
            'PENDENTE': 'Pendente',
            'PROCESSANDO': 'Processando',
            'CONCLUIDO': 'Concluido',
            'FALHA': 'Falha'
        };
        return '<span class="status-badge status-' + status + '">' +
            '<span class="status-ponto"></span>' +
            (labels[status] || status) +
            '</span>';
    }

    function renderizarAcao(video) {
        if (video.status === 'CONCLUIDO' && video.urlDownload) {
            return '<button class="btn btn-download" data-url="' + escapeHtml(video.urlDownload) + '" data-nome="' + escapeHtml(video.nomeOriginal) + '">' +
                'Baixar ZIP' +
                '</button>';
        }
        if (video.status === 'FALHA' && video.mensagemErro) {
            return '<span class="erro-texto" title="' + escapeHtml(video.mensagemErro) + '">' +
                escapeHtml(video.mensagemErro) +
                '</span>';
        }
        if (video.status === 'PROCESSANDO') {
            return '<span style="color:var(--cor-texto-secundario);font-size:0.8125rem">' +
                '<span class="spinner" style="width:0.75em;height:0.75em;border-width:1.5px"></span> Processando...' +
                '</span>';
        }
        return '<span style="color:var(--cor-texto-secundario);font-size:0.8125rem">Aguardando...</span>';
    }

    function baixarVideo(url, nomeOriginal) {
        var a = document.createElement('a');
        // Usar fetch para incluir o token de autorizacao
        fetch(url, {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(function (resp) {
            if (resp.status === 401 || resp.status === 403) {
                localStorage.removeItem('token');
                localStorage.removeItem('email');
                window.location.href = '/login.html';
                return null;
            }
            if (!resp.ok) throw new Error('Erro ao baixar');
            return resp.blob();
        })
        .then(function (blob) {
            if (!blob) return;
            var urlBlob = URL.createObjectURL(blob);
            a.href = urlBlob;
            var nomeZip = nomeOriginal ? nomeOriginal.replace(/\.[^.]+$/, '') + '_frames.zip' : 'frames.zip';
            a.download = nomeZip;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(urlBlob);
        })
        .catch(function () {
            alert('Erro ao baixar o arquivo. Tente novamente.');
        });
    }

    function formatarData(dataStr) {
        if (!dataStr) return '-';
        var d = new Date(dataStr);
        if (isNaN(d.getTime())) return dataStr;
        var dia = String(d.getDate()).padStart(2, '0');
        var mes = String(d.getMonth() + 1).padStart(2, '0');
        var ano = d.getFullYear();
        var hora = String(d.getHours()).padStart(2, '0');
        var min = String(d.getMinutes()).padStart(2, '0');
        return dia + '/' + mes + '/' + ano + ' ' + hora + ':' + min;
    }

    function escapeHtml(str) {
        if (!str) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    // Iniciar
    carregarVideos();

    // Auto-refresh a cada 5 segundos
    intervaloAtualizacao = setInterval(carregarVideos, 5000);
});
