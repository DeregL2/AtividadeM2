const API = '/api/dashboard';

const STATUS = {
    ABERTO: { nome: 'Aberto', cor: '#0d6efd', classe: 'bg-primary' },
    EM_ANDAMENTO: { nome: 'Em andamento', cor: '#fd7e14', classe: 'bg-warning text-dark' },
    CONCLUIDO: { nome: 'Concluído', cor: '#198754', classe: 'bg-success' },
    CANCELADO: { nome: 'Cancelado', cor: '#6c757d', classe: 'bg-secondary' }
};

const PRIORIDADES = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };

const graficos = {};
let temporizador = null;

function parametros(extras = {}) {
    const params = new URLSearchParams();
    new FormData(document.getElementById('filtros')).forEach((valor, chave) => {
        if (valor) {
            params.append(chave, valor);
        }
    });
    Object.entries(extras).forEach(([chave, valor]) => params.set(chave, valor));
    return params.toString();
}

async function buscar(caminho, extras) {
    const resposta = await fetch(`${API}/${caminho}?${parametros(extras)}`, {
        headers: { Accept: 'application/json' }
    });
    if (resposta.status === 401 || (resposta.redirected && resposta.url.includes('/login'))) {
        window.location.href = '/login';
        throw new Error('Sessão expirada. Faça login novamente.');
    }
    const corpo = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        throw new Error(corpo && corpo.mensagem ? corpo.mensagem : `Erro ${resposta.status} ao consultar a API`);
    }
    return corpo;
}

function mostrarErro(mensagem) {
    const alerta = document.getElementById('alerta');
    alerta.textContent = mensagem;
    alerta.classList.toggle('d-none', !mensagem);
}

function preencherSelect(id, valores, rotulos = {}) {
    const select = document.getElementById(id);
    select.innerHTML = '';
    select.add(new Option('Todos', ''));
    valores.forEach(valor => select.add(new Option(rotulos[valor] ? rotulos[valor].nome || rotulos[valor] : valor, valor)));
}

async function carregarOpcoes() {
    try {
        const opcoes = await fetch(`${API}/opcoes-filtro`).then(resposta => resposta.json());
        preencherSelect('status', opcoes.status, STATUS);
        preencherSelect('prioridade', opcoes.prioridades, PRIORIDADES);
        preencherSelect('tipo', opcoes.tipos);
        preencherSelect('tecnico', opcoes.tecnicos);
    } catch (erro) {
        mostrarErro('Não foi possível carregar as opções de filtro.');
    }
}

function desenhar(id, configuracao) {
    if (graficos[id]) {
        graficos[id].destroy();
    }
    configuracao.options = Object.assign({ responsive: true, maintainAspectRatio: false }, configuracao.options);
    graficos[id] = new Chart(document.getElementById(id), configuracao);
}

function numero(valor) {
    return Number(valor).toLocaleString('pt-BR');
}

function exibirIndicadores(dados) {
    document.getElementById('kpiTotal').textContent = numero(dados.total);
    document.getElementById('kpiAbertos').textContent = numero(dados.abertos);
    document.getElementById('kpiAndamento').textContent = numero(dados.emAndamento);
    document.getElementById('kpiConcluidos').textContent = numero(dados.concluidos);
    document.getElementById('kpiCancelados').textContent = numero(dados.cancelados);
    document.getElementById('kpiCriticos').textContent = numero(dados.criticosPendentes);
    document.getElementById('kpiTaxa').textContent = `${numero(dados.taxaResolucao)}%`;
    document.getElementById('kpiTempo').textContent = `${numero(dados.tempoMedioResolucaoHoras)} h`;
}

function exibirPeriodo(dados) {
    desenhar('graficoPeriodo', {
        type: 'line',
        data: {
            labels: dados.map(item => item.periodo),
            datasets: [
                { label: 'Abertos', data: dados.map(item => item.abertos), borderColor: '#0d6efd', backgroundColor: '#0d6efd', tension: 0.3 },
                { label: 'Concluídos', data: dados.map(item => item.concluidos), borderColor: '#198754', backgroundColor: '#198754', tension: 0.3 }
            ]
        },
        options: { scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
}

function exibirStatus(dados) {
    desenhar('graficoStatus', {
        type: 'doughnut',
        data: {
            labels: dados.map(item => STATUS[item.nome].nome),
            datasets: [{ data: dados.map(item => item.total), backgroundColor: dados.map(item => STATUS[item.nome].cor) }]
        },
        options: { plugins: { legend: { position: 'bottom' } } }
    });
}

function exibirRanking(id, dados, cor) {
    desenhar(id, {
        type: 'bar',
        data: {
            labels: dados.map(item => item.nome),
            datasets: [{ label: 'Chamados', data: dados.map(item => item.total), backgroundColor: cor }]
        },
        options: {
            indexAxis: 'y',
            plugins: { legend: { display: false } },
            scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
        }
    });
}

function celula(linha, conteudo) {
    const td = document.createElement('td');
    if (conteudo instanceof Node) {
        td.appendChild(conteudo);
    } else {
        td.textContent = conteudo;
    }
    linha.appendChild(td);
}

function exibirTabela(chamados) {
    const corpo = document.getElementById('tabelaChamados');
    corpo.innerHTML = '';
    if (chamados.length === 0) {
        const linha = corpo.insertRow();
        const td = linha.insertCell();
        td.colSpan = 6;
        td.className = 'text-center text-muted';
        td.textContent = 'Nenhum chamado encontrado para os filtros selecionados.';
        return;
    }
    chamados.forEach(chamado => {
        const linha = document.createElement('tr');
        const status = STATUS[chamado.status] || { nome: chamado.status, classe: 'bg-dark' };
        const badge = document.createElement('span');
        badge.className = `badge ${status.classe}`;
        badge.textContent = status.nome;
        celula(linha, chamado.dataAbertura ? new Date(chamado.dataAbertura).toLocaleString('pt-BR') : '-');
        celula(linha, chamado.descricao);
        celula(linha, chamado.tipo);
        celula(linha, chamado.tecnico);
        celula(linha, PRIORIDADES[chamado.prioridade] || chamado.prioridade);
        celula(linha, badge);
        corpo.appendChild(linha);
    });
}

async function carregarDashboard() {
    mostrarErro('');
    const apenasConcluidos = document.getElementById('apenasConcluidos').checked;
    try {
        const [indicadores, periodo, status, tipos, tecnicos, chamados] = await Promise.all([
            buscar('indicadores'),
            buscar('por-periodo'),
            buscar('por-status'),
            buscar('tipos-mais-atendidos', { limite: 5, apenasConcluidos }),
            buscar('por-tecnico', { limite: 5 }),
            buscar('chamados', { limite: 10 })
        ]);
        exibirIndicadores(indicadores);
        exibirPeriodo(periodo);
        exibirStatus(status);
        exibirRanking('graficoTipos', tipos, '#6f42c1');
        exibirRanking('graficoTecnicos', tecnicos, '#20c997');
        exibirTabela(chamados);
        document.getElementById('ultimaAtualizacao').textContent =
            `Última atualização: ${new Date().toLocaleTimeString('pt-BR')}`;
    } catch (erro) {
        mostrarErro(`Não foi possível carregar os dados do dashboard: ${erro.message}`);
    }
}

function configurarAtualizacaoAutomatica() {
    clearInterval(temporizador);
    if (document.getElementById('autoAtualizar').checked) {
        temporizador = setInterval(carregarDashboard, 60000);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    const formulario = document.getElementById('filtros');

    formulario.addEventListener('submit', evento => {
        evento.preventDefault();
        const inicio = document.getElementById('dataInicio').value;
        const fim = document.getElementById('dataFim').value;
        if (inicio && fim && inicio > fim) {
            mostrarErro('A data de início não pode ser maior que a data de fim.');
            return;
        }
        carregarDashboard();
    });

    document.getElementById('btnLimpar').addEventListener('click', () => {
        formulario.reset();
        carregarDashboard();
    });

    document.getElementById('btnAtualizar').addEventListener('click', carregarDashboard);
    document.getElementById('apenasConcluidos').addEventListener('change', carregarDashboard);
    document.getElementById('autoAtualizar').addEventListener('change', configurarAtualizacaoAutomatica);

    await carregarOpcoes();
    await carregarDashboard();
    configurarAtualizacaoAutomatica();
});