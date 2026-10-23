const API_PADRAO = 'http://localhost:8080';
const tokenKey = 'oficina.jwt';
const apiKey = 'oficina.apiUrl';
const pageContent = document.querySelector('.page-content');
const dialog = document.querySelector('#order-dialog');
const toast = document.querySelector('#toast');
const searchInput = document.querySelector('#global-search');
const sidebar = document.querySelector('#sidebar');
const nomeView = { inicio: 'Visão geral', ordens: 'Ordens de serviço', clientes: 'Clientes', veiculos: 'Veículos', estoque: 'Estoque', servicos: 'Serviços', indicadores: 'Indicadores', configuracoes: 'Configurações' };
let token = sessionStorage.getItem(tokenKey);
let dadosEmCache = {};
let timerToast;

function baseApi() { return (localStorage.getItem(apiKey) || API_PADRAO).replace(/\/$/, ''); }
function aviso(texto, erro = false) {
  toast.textContent = texto;
  toast.classList.toggle('toast-error', erro);
  toast.classList.add('is-visible');
  clearTimeout(timerToast);
  timerToast = setTimeout(() => toast.classList.remove('is-visible'), 4200);
}

async function requisicao(caminho, opcoes = {}) {
  const cabecalhos = new Headers(opcoes.headers || {});
  if (token) cabecalhos.set('Authorization', `Bearer ${token}`);
  if (opcoes.body && !(opcoes.body instanceof FormData)) cabecalhos.set('Content-Type', 'application/json');
  let resposta;
  try { resposta = await fetch(`${baseApi()}${caminho}`, { ...opcoes, headers: cabecalhos }); }
  catch { throw new Error(`Não foi possível conectar à API em ${baseApi()}. Confira se o backend está ativo e a URL está correta.`); }
  if (resposta.status === 401 && !caminho.startsWith('/autenticacao/')) {
    sair(false);
    throw new Error('Sua sessão expirou. Entre novamente.');
  }
  if (!resposta.ok) {
    let mensagem = `Erro ${resposta.status}`;
    const textoErro = await resposta.text();
    try {
      const corpo = JSON.parse(textoErro);
      mensagem = corpo.detail || corpo.mensagem || corpo.message || corpo.erro || corpo.title || mensagem;
      if (Array.isArray(corpo.erros)) mensagem = corpo.erros.map((e) => e.mensagem || e.defaultMessage || e).join(' · ');
    } catch { if (textoErro) mensagem = textoErro; }
    if (resposta.status === 403) mensagem = 'Seu perfil não tem permissão para esta ação.';
    throw new Error(mensagem);
  }
  if (resposta.status === 204) return null;
  const texto = await resposta.text();
  return texto ? JSON.parse(texto) : null;
}

function renderLogin(mensagem = '') {
  document.querySelector('#login-screen')?.remove();
  const tela = document.createElement('section');
  tela.id = 'login-screen';
  tela.className = 'login-screen';
  tela.innerHTML = `<div class="login-card glass-card"><a class="brand login-brand" href="#"><span class="brand-mark"><svg viewBox="0 0 48 48" fill="none"><path d="M9 28.5 12.3 19a4 4 0 0 1 3.8-2.7h15.8a4 4 0 0 1 3.8 2.7l3.3 9.5v8a2.5 2.5 0 0 1-2.5 2.5h-2a2.5 2.5 0 0 1-2.5-2.5v-2H16v2a2.5 2.5 0 0 1-2.5 2.5h-2A2.5 2.5 0 0 1 9 36.5v-8Z" stroke="currentColor" stroke-width="2.2"/><path d="M12 27h24M16 16l2-5h12l2 5" stroke="currentColor" stroke-width="2.2"/></svg></span><span class="brand-copy"><strong>oficina<span>digital</span></strong><small>GESTÃO INTELIGENTE</small></span></a><p class="eyebrow"><span class="live-dot"></span> ACESSO SEGURO</p><h1>Bom ter você<br/>de <em>volta.</em></h1><p class="login-copy">Entre para acompanhar a operação da sua oficina.</p><form id="login-form"><label>E-mail<input name="email" type="email" autocomplete="username" required placeholder="voce@oficina.com.br"/></label><label>Senha<input name="senha" type="password" autocomplete="current-password" required placeholder="Sua senha"/></label><p class="login-error" id="login-error">${esc(mensagem)}</p><button class="button button-primary" type="submit">Entrar na oficina <span>→</span></button></form><button class="api-config-link" id="api-config">API: ${esc(baseApi())} · alterar</button></div><div class="login-art"><div class="login-art-glow"></div><span>OFICINA DIGITAL</span><strong>Precisão no serviço.<br/><em>Confiança no cuidado.</em></strong><small>GESTÃO DE PONTA A PONTA</small></div>`;
  document.body.append(tela);
  document.querySelectorAll('#login-form label').forEach((label) => label.insertAdjacentHTML('beforeend', '<span class="field-error" role="alert"></span>'));
  document.querySelector('#login-error').setAttribute('aria-live', 'assertive');
  document.querySelector('#login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!validarFormulario(e.currentTarget)) return;
    const botao = e.currentTarget.querySelector('button[type=submit]');
    botao.disabled = true; botao.textContent = 'Conectando…';
    const dados = Object.fromEntries(new FormData(e.currentTarget));
    try {
      const sessao = await requisicao('/autenticacao/login', { method: 'POST', body: JSON.stringify(dados) });
      token = sessao.token;
      sessionStorage.setItem(tokenKey, token);
      sessionStorage.setItem('oficina.email', dados.email);
      sessionStorage.setItem('oficina.jwtExpires', String(Date.now() + sessao.expiraEmSegundos * 1000));
      document.querySelector('#login-screen').remove();
      const perfil = lerPerfil(token);
      document.querySelector('.profile-card strong').textContent = dados.email;
      document.querySelector('.profile-card small').textContent = perfil || 'Usuário';
      await carregarInicio();
      aviso('Login realizado. Conectado à API da Oficina Digital.');
    } catch (erro) {
      document.querySelector('#login-error').textContent = erro.message;
      botao.disabled = false; botao.innerHTML = 'Entrar na oficina <span>→</span>';
    }
  });
  document.querySelector('#login-form').addEventListener('input', (evento) => {
    const campo = evento.target;
    if (!campo.matches('input')) return;
    const erro = campo.closest('label')?.querySelector('.field-error');
    if (erro) erro.textContent = campo.validity.valid ? '' : campo.validationMessage;
    if (campo.validity.valid) document.querySelector('#login-error').textContent = '';
  });
  document.querySelector('#api-config').addEventListener('click', () => {
    const novaUrl = window.prompt('URL base da API (ex.: http://localhost:8080)', baseApi());
    if (novaUrl?.trim()) { localStorage.setItem(apiKey, novaUrl.trim()); renderLogin(); }
  });
}

function lerPerfil(jwt) {
  try {
    const payload = JSON.parse(decodeURIComponent(escape(atob(jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))));
    return (payload.authorities || []).map((p) => p.replace('ROLE_', '')).join(', ') || 'Usuário';
  } catch { return 'Usuário'; }
}
function sair(mostrar = true) {
  token = null; dadosEmCache = {};
  sessionStorage.removeItem(tokenKey); sessionStorage.removeItem('oficina.jwtExpires'); sessionStorage.removeItem('oficina.email');
  renderLogin();
  if (mostrar) aviso('Você saiu da Oficina Digital.');
}
function esc(valor = '') { return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function moeda(valor) { return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }); }
function formatarData(valor) { return valor ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' }).format(new Date(valor)) : '—'; }
function valorPlaca(veiculo) { return typeof veiculo?.placa === 'string' ? veiculo.placa : veiculo?.placa?.valor || '—'; }
function statusNome(valor = '') { return valor.replaceAll('_', ' ').toLocaleLowerCase('pt-BR').replace(/^./, (c) => c.toLocaleUpperCase('pt-BR')); }
function validarFormulario(form) {
  form.classList.add('was-validated');
  let primeiroInvalido = null;
  form.querySelectorAll('input, select, textarea').forEach((campo) => {
    const mensagem = campo.validity.valid ? '' : campo.validationMessage;
    campo.setAttribute('aria-invalid', String(Boolean(mensagem)));
    const erro = campo.closest('.form-field, label')?.querySelector('.field-error');
    if (erro) erro.textContent = mensagem;
    if (mensagem && !primeiroInvalido) primeiroInvalido = campo;
  });
  if (!primeiroInvalido) return true;
  primeiroInvalido.focus();
  primeiroInvalido.reportValidity();
  return false;
}

function atualizarData() {
  const agora = new Date();
  const partes = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' }).formatToParts(agora);
  const get = (tipo) => partes.find((p) => p.type === tipo)?.value || '';
  document.querySelector('#today-label').textContent = `${get('weekday')}, ${get('day')} ${new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(agora)}`.toLocaleUpperCase('pt-BR').replace('.', '');
  document.querySelector('.hero-kicker').innerHTML = `<span class="hero-kicker-icon">✦</span> ${get('weekday').toLocaleUpperCase('pt-BR')} · OPERAÇÃO EM DIA`;
  document.querySelector('.date-day').textContent = get('day');
  document.querySelector('.delivery-date strong').textContent = `${get('month')}, ${get('weekday')}`;
}

function estruturaView(titulo, subtitulo, acao = '') {
  pageContent.innerHTML = `<section class="workspace-view"><div class="view-heading"><div><p class="eyebrow">OFICINA DIGITAL · GESTÃO</p><h1>${esc(titulo)}</h1><p>${esc(subtitulo)}</p></div><div class="view-actions">${acao}</div></div><div class="section-card glass-card data-panel"><div class="data-toolbar"><span class="data-summary" id="data-summary">Carregando dados da API…</span><button class="select-button" id="refresh-view">↻ Atualizar</button></div><div class="table-wrap" id="table-content"><div class="loading-state"><span class="spinner"></span>Consultando a API…</div></div></div></section>`;
  document.querySelector('#refresh-view').addEventListener('click', () => abrirView(abaAtual, true));
}
let abaAtual = 'inicio';
function mostrarTabela(colunas, linhas, resumo) {
  document.querySelector('#data-summary').textContent = resumo || `${linhas.length} registro(s)`;
  document.querySelector('#table-content').innerHTML = linhas.length ? `<table><thead><tr>${colunas.map((c) => `<th>${esc(c)}</th>`).join('')}</tr></thead><tbody>${linhas.join('')}</tbody></table>` : '<div class="empty-state"><span>✦</span><strong>Nenhum registro encontrado</strong><small>Quando houver dados cadastrados, eles aparecerão aqui.</small></div>';
}
function erroView(erro) {
  const alvo = document.querySelector('#table-content');
  if (alvo) alvo.innerHTML = `<div class="error-state"><strong>Não foi possível carregar esta área</strong><p>${esc(erro.message)}</p><button class="button button-quiet" onclick="location.reload()">Tentar novamente</button></div>`;
}

async function carregarInicio() {
  abaAtual = 'inicio';
  pageContent.innerHTML = `<section class="welcome-row"><div><p class="eyebrow"><span class="live-dot"></span> CONECTADO À API · TEMPO REAL</p><h1>Bom dia, <span id="welcome-name">equipe</span> <span>✦</span></h1><p class="welcome-copy">Resumo atualizado diretamente com os dados da sua oficina.</p></div><button class="button button-primary" id="new-order-button"><span>＋</span> Nova ordem de serviço</button></section><section class="metrics-grid api-metrics"><article class="metric-card glass-card"><div class="metric-head">Clientes cadastrados</div><div class="metric-value" id="metric-clients">—</div><div class="metric-foot">Base ativa da oficina</div></article><article class="metric-card glass-card"><div class="metric-head">Ordens de serviço</div><div class="metric-value" id="metric-orders">—</div><div class="metric-foot">Registros carregados</div></article><article class="metric-card glass-card"><div class="metric-head">Veículos</div><div class="metric-value" id="metric-vehicles">—</div><div class="metric-foot">Vinculados aos clientes</div></article><article class="metric-card glass-card"><div class="metric-head">Itens no estoque</div><div class="metric-value" id="metric-stock">—</div><div class="metric-foot">Itens cadastrados</div></article></section><section class="section-card glass-card dashboard-live"><div class="section-heading"><div><p class="eyebrow">DADOS DA API</p><h3>Ordens recentes</h3></div><button class="text-link" data-view="ordens">Ver todas <span>↗</span></button></div><div id="recent-orders" class="table-wrap"><div class="loading-state"><span class="spinner"></span>Atualizando painel…</div></div></section><footer class="page-footer"><span>© 2026 Oficina Digital</span><span><i></i> API conectada</span><span>Dados reais do seu ambiente.</span></footer>`;
  document.querySelector('#welcome-name').textContent = sessionStorage.getItem('oficina.email')?.split('@')[0] || 'equipe';
  document.querySelector('#new-order-button').addEventListener('click', () => abrirFormularioOrdem());
  try {
    const [clientes, ordens, estoque] = await Promise.all([
      requisicao('/clientes?pagina=0&tamanho=100'), requisicao('/ordens-servico?pagina=0&tamanho=100'), requisicao('/estoque/itens').catch(() => null),
    ]);
    dadosEmCache.clientes = clientes;
    const veiculos = (await Promise.all(clientes.map((c) => requisicao(`/veiculos?clienteId=${c.id}`).catch(() => [])))).flat();
    dadosEmCache.veiculos = veiculos;
    document.querySelector('#metric-clients').textContent = clientes.length;
    document.querySelector('#metric-orders').textContent = ordens.length;
    document.querySelector('#metric-vehicles').textContent = veiculos.length;
    document.querySelector('#metric-stock').textContent = estoque ? estoque.length : '—';
    const recentes = ordens.slice(0, 5).map((os) => `<tr><td><strong>${esc(os.numero)}</strong></td><td>${esc(os.clienteId)}</td><td>${esc(statusNome(os.status))}</td><td>${formatarData(os.recebidaEm)}</td><td><button class="table-action" data-order-id="${esc(os.id)}">Detalhes</button></td></tr>`);
    document.querySelector('#recent-orders').innerHTML = recentes.length ? `<table><thead><tr><th>OS</th><th>Cliente (ID)</th><th>Status</th><th>Entrada</th><th></th></tr></thead><tbody>${recentes.join('')}</tbody></table>` : '<div class="empty-state">Ainda não há ordens cadastradas.</div>';
    document.querySelectorAll('[data-order-id]').forEach((b) => b.addEventListener('click', () => abrirDetalheOS(b.dataset.orderId)));
  } catch (erro) { aviso(erro.message, true); document.querySelector('#recent-orders').textContent = erro.message; }
}

async function abrirView(view, refresh = false) {
  abaAtual = view;
  sidebar.classList.remove('is-open');
  document.querySelector('#mobile-menu').setAttribute('aria-expanded', 'false');
  document.querySelectorAll('.main-nav .nav-item').forEach((item) => {
    const ativa = item.dataset.view === view;
    item.classList.toggle('is-active', ativa);
    if (ativa) item.setAttribute('aria-current', 'page'); else item.removeAttribute('aria-current');
  });
  document.querySelector('#breadcrumb-current').textContent = nomeView[view] || 'Visão geral';
  if (view === 'inicio') return carregarInicio();
  const handlers = { clientes: carregarClientes, veiculos: carregarVeiculos, ordens: carregarOrdens, servicos: carregarServicos, estoque: carregarEstoque, indicadores: carregarIndicadores, configuracoes: carregarConfiguracoes };
  if (handlers[view]) await handlers[view](refresh);
}

function botaoNovo(legenda, id = 'new-record') { return `<button class="button button-primary" id="${id}"><span>＋</span> ${esc(legenda)}</button>`; }
function ligarNovo(callback, id = 'new-record') { document.querySelector(`#${id}`)?.addEventListener('click', callback); }

async function carregarClientes() {
  estruturaView('Clientes', 'Cadastros de pessoas e empresas que atendemos.', botaoNovo('Novo cliente'));
  ligarNovo(() => formularioCliente());
  try {
    const dados = await requisicao('/clientes?pagina=0&tamanho=100'); dadosEmCache.clientes = dados;
    mostrarTabela(['Nome', 'Documento', 'E-mail', 'Telefone', 'Nascimento', 'Ações'], dados.map((c) => `<tr><td><strong>${esc(c.nome)}</strong></td><td>${esc(c.documento)}</td><td>${esc(c.email)}</td><td>${esc(c.telefone)}</td><td>${formatarData(c.nascimento)}</td><td class="row-actions"><button data-edit-client="${c.id}">Editar</button><button data-delete-client="${c.id}" class="danger-link">Inativar</button></td></tr>`));
    document.querySelectorAll('[data-edit-client]').forEach((b) => b.addEventListener('click', () => formularioCliente(dados.find((c) => c.id === b.dataset.editClient))));
    document.querySelectorAll('[data-delete-client]').forEach((b) => b.addEventListener('click', async () => { if (confirm('Inativar este cliente?')) try { await requisicao(`/clientes/${b.dataset.deleteClient}`, { method: 'DELETE' }); aviso('Cliente inativado.'); carregarClientes(); } catch (e) { aviso(e.message, true); } }));
  } catch (e) { erroView(e); }
}

function formularioCliente(cliente) {
  const c = cliente || {};
  const campos = [
    { name: 'nome', label: 'Nome completo', required: true, value: c.nome },
    ...(!cliente ? [{ name: 'documento', label: 'CPF ou CNPJ', required: true, value: c.documento }] : []),
    { name: 'email', label: 'E-mail', type: 'email', required: true, value: c.email },
    { name: 'telefone', label: 'Telefone', required: true, value: c.telefone },
    { name: 'nascimento', label: 'Nascimento', type: 'date', value: c.nascimento },
    { name: 'cep', label: 'CEP', value: c.endereco?.cep }, { name: 'logradouro', label: 'Logradouro', value: c.endereco?.logradouro },
    { name: 'numero', label: 'Número', value: c.endereco?.numero }, { name: 'complemento', label: 'Complemento', value: c.endereco?.complemento },
    { name: 'bairro', label: 'Bairro', value: c.endereco?.bairro }, { name: 'cidade', label: 'Cidade', value: c.endereco?.cidade }, { name: 'uf', label: 'UF', value: c.endereco?.uf, maxlength: 2 },
  ];
  abrirFormulario(cliente ? 'Editar cliente' : 'Cadastrar cliente', campos, async (d) => {
    const body = { nome: d.nome, email: d.email, telefone: d.telefone, nascimento: d.nascimento || null, endereco: d.cep ? { cep: d.cep, logradouro: d.logradouro, numero: d.numero, complemento: d.complemento || null, bairro: d.bairro, cidade: d.cidade, uf: d.uf.toUpperCase() } : null };
    if (!cliente) body.documento = d.documento;
    await requisicao(cliente ? `/clientes/${cliente.id}` : '/clientes', { method: cliente ? 'PUT' : 'POST', body: JSON.stringify(body) });
    aviso(cliente ? 'Cadastro de cliente atualizado.' : 'Cliente cadastrado.'); await carregarClientes();
  });
}

async function carregarVeiculos() {
  estruturaView('Veículos', 'Veículos cadastrados e associados aos seus clientes.', botaoNovo('Novo veículo'));
  ligarNovo(() => formularioVeiculo());
  try {
    const clientes = await requisicao('/clientes?pagina=0&tamanho=100');
    const grupos = await Promise.all(clientes.map(async (c) => (await requisicao(`/veiculos?clienteId=${c.id}`)).map((v) => ({ ...v, clienteNome: c.nome }))));
    const veiculos = grupos.flat(); dadosEmCache.veiculos = veiculos; dadosEmCache.clientes = clientes;
    mostrarTabela(['Veículo', 'Placa', 'Ano', 'Cliente', 'Ações'], veiculos.map((v) => `<tr><td><strong>${esc(v.marca)} ${esc(v.modelo)}</strong><small class="table-secondary">${esc(v.categoria || 'Sem categoria')}</small></td><td>${esc(valorPlaca(v))}</td><td>${esc(v.anoFabricacao)}</td><td>${esc(v.clienteNome)}</td><td class="row-actions"><button data-edit-vehicle="${v.id}">Editar</button><button data-delete-vehicle="${v.id}" class="danger-link">Inativar</button></td></tr>`));
    document.querySelectorAll('[data-edit-vehicle]').forEach((b) => b.addEventListener('click', () => formularioVeiculo(veiculos.find((v) => v.id === b.dataset.editVehicle))));
    document.querySelectorAll('[data-delete-vehicle]').forEach((b) => b.addEventListener('click', async () => { if (confirm('Inativar este veículo?')) try { await requisicao(`/veiculos/${b.dataset.deleteVehicle}`, { method: 'DELETE' }); aviso('Veículo inativado.'); carregarVeiculos(); } catch (e) { aviso(e.message, true); } }));
  } catch (e) { erroView(e); }
}
async function formularioVeiculo(veiculo) {
  const clientes = dadosEmCache.clientes || await requisicao('/clientes?pagina=0&tamanho=100');
  if (!clientes.length) { aviso('Cadastre um cliente antes de cadastrar um veículo.', true); return; }
  const campos = [
    ...(!veiculo ? [{ name: 'clienteId', label: 'Cliente', type: 'select', required: true, options: clientes.map((c) => [c.id, `${c.nome} · ${c.documento}`]) }] : []),
    ...(!veiculo ? [{ name: 'placa', label: 'Placa', required: true }] : []),
    { name: 'marca', label: 'Marca', required: true, value: veiculo?.marca }, { name: 'modelo', label: 'Modelo', required: true, value: veiculo?.modelo },
    { name: 'anoFabricacao', label: 'Ano', type: 'number', min: 1950, max: 2100, required: true, value: veiculo?.anoFabricacao }, { name: 'categoria', label: 'Categoria', value: veiculo?.categoria },
  ];
  abrirFormulario(veiculo ? 'Editar veículo' : 'Cadastrar veículo', campos, async (d) => {
    const body = veiculo ? { marca: d.marca, modelo: d.modelo, anoFabricacao: Number(d.anoFabricacao), categoria: d.categoria } : { clienteId: d.clienteId, placa: d.placa.toUpperCase(), marca: d.marca, modelo: d.modelo, anoFabricacao: Number(d.anoFabricacao), categoria: d.categoria };
    await requisicao(veiculo ? `/veiculos/${veiculo.id}` : '/veiculos', { method: veiculo ? 'PUT' : 'POST', body: JSON.stringify(body) });
    aviso(veiculo ? 'Veículo atualizado.' : 'Veículo cadastrado.'); await carregarVeiculos();
  });
}

async function carregarServicos() {
  estruturaView('Catálogo de serviços', 'Serviços disponíveis para compor os orçamentos.', botaoNovo('Novo serviço'));
  ligarNovo(() => formularioServico());
  try {
    const dados = await requisicao('/servicos'); dadosEmCache.servicos = dados;
    mostrarTabela(['Serviço', 'Categoria', 'Valor base', 'Duração', 'Ações'], dados.map((s) => `<tr><td><strong>${esc(s.nome)}</strong><small class="table-secondary">${esc(s.descricao || '')}</small></td><td>${esc(s.categoria)}</td><td>${moeda(s.valorBase)}</td><td>${s.duracaoEstimadaMinutos} min</td><td class="row-actions"><button data-edit-service="${s.id}">Editar</button><button data-delete-service="${s.id}" class="danger-link">Inativar</button></td></tr>`));
    document.querySelectorAll('[data-edit-service]').forEach((b) => b.addEventListener('click', () => formularioServico(dados.find((s) => s.id === b.dataset.editService))));
    document.querySelectorAll('[data-delete-service]').forEach((b) => b.addEventListener('click', async () => { if (confirm('Inativar este serviço do catálogo?')) try { await requisicao(`/servicos/${b.dataset.deleteService}`, { method: 'DELETE' }); aviso('Serviço inativado.'); carregarServicos(); } catch (e) { aviso(e.message, true); } }));
  } catch (e) { erroView(e); }
}
function formularioServico(s) {
  abrirFormulario(s ? 'Editar serviço' : 'Novo serviço', [
    { name: 'nome', label: 'Nome', required: true, value: s?.nome }, { name: 'descricao', label: 'Descrição', type: 'textarea', value: s?.descricao },
    { name: 'categoria', label: 'Categoria', required: true, value: s?.categoria }, { name: 'valorBase', label: 'Valor base (R$)', type: 'number', min: 0, step: '.01', required: true, value: s?.valorBase },
    { name: 'duracaoEstimadaMinutos', label: 'Duração estimada (minutos)', type: 'number', min: 1, required: true, value: s?.duracaoEstimadaMinutos || 60 },
  ], async (d) => { const body = { ...d, valorBase: Number(d.valorBase), duracaoEstimadaMinutos: Number(d.duracaoEstimadaMinutos) }; await requisicao(s ? `/servicos/${s.id}` : '/servicos', { method: s ? 'PUT' : 'POST', body: JSON.stringify(body) }); aviso(s ? 'Serviço atualizado.' : 'Serviço cadastrado.'); await carregarServicos(); });
}

async function carregarEstoque() {
  estruturaView('Estoque', 'Peças e insumos, com níveis e disponibilidade atual.', botaoNovo('Cadastrar item'));
  ligarNovo(() => formularioItemEstoque());
  try {
    const dados = await requisicao('/estoque/itens'); dadosEmCache.estoque = dados;
    mostrarTabela(['Item', 'Tipo', 'Disponível', 'Estoque mínimo', 'Valor unitário', 'Ações'], dados.map((i) => `<tr><td><strong>${esc(i.descricao)}</strong><small class="table-secondary">${esc(i.codigo)} · ${esc(i.unidadeMedida)}</small></td><td>${esc(i.tipo)}</td><td><span class="stock-quantity ${Number(i.quantidadeDisponivel) <= Number(i.estoqueMinimo) ? 'stock-low' : ''}">${i.quantidadeDisponivel} ${esc(i.unidadeMedida)}</span></td><td>${i.estoqueMinimo}</td><td>${moeda(i.precoUnitario)}</td><td class="row-actions"><button data-stock-entry="${i.id}">Registrar entrada</button></td></tr>`));
    document.querySelectorAll('[data-stock-entry]').forEach((b) => b.addEventListener('click', () => formularioEntrada(dados.find((i) => i.id === b.dataset.stockEntry))));
  } catch (e) { erroView(e); }
}
function formularioItemEstoque() {
  abrirFormulario('Cadastrar item de estoque', [
    { name: 'codigo', label: 'Código', required: true }, { name: 'descricao', label: 'Descrição', required: true },
    { name: 'tipo', label: 'Tipo', type: 'select', required: true, options: [['PECA', 'Peça'], ['INSUMO', 'Insumo']] },
    { name: 'unidadeMedida', label: 'Unidade de medida', required: true, value: 'unidade' }, { name: 'precoUnitario', label: 'Preço unitário (R$)', type: 'number', min: 0, step: '.01', required: true },
    { name: 'quantidadeInicial', label: 'Quantidade inicial', type: 'number', min: 0, step: '.001', required: true, value: 0 }, { name: 'estoqueMinimo', label: 'Estoque mínimo', type: 'number', min: 0, step: '.001', required: true, value: 0 },
  ], async (d) => { const body = { ...d, precoUnitario: Number(d.precoUnitario), quantidadeInicial: Number(d.quantidadeInicial), estoqueMinimo: Number(d.estoqueMinimo) }; await requisicao('/estoque/itens', { method: 'POST', body: JSON.stringify(body) }); aviso('Item de estoque cadastrado.'); await carregarEstoque(); });
}
function formularioEntrada(item) {
  abrirFormulario(`Entrada · ${item.descricao}`, [{ name: 'quantidade', label: `Quantidade (${item.unidadeMedida})`, type: 'number', min: '.001', step: '.001', required: true }, { name: 'observacao', label: 'Observação', type: 'textarea' }], async (d) => { await requisicao(`/estoque/itens/${item.id}/entradas`, { method: 'POST', body: JSON.stringify({ quantidade: Number(d.quantidade), observacao: d.observacao }) }); aviso('Entrada de estoque registrada.'); await carregarEstoque(); });
}

async function carregarOrdens() {
  estruturaView('Ordens de serviço', 'Acompanhe o ciclo completo de atendimento e execução.', `<button class="button button-quiet" id="track-order">Acompanhar por código</button>${botaoNovo('Abrir ordem')}`);
  ligarNovo(abrirFormularioOrdem);
  document.querySelector('#track-order').addEventListener('click', () => abrirFormulario('Acompanhar ordem', [{ name: 'codigo', label: 'Código de acompanhamento', required: true }], async (d) => { dialog.close(); await abrirDetalheOS(null, d.codigo.trim()); }));
  try {
    const ordens = await requisicao('/ordens-servico?pagina=0&tamanho=100'); dadosEmCache.ordens = ordens;
    const [clientes, servicos, estoque] = await Promise.all([requisicao('/clientes?pagina=0&tamanho=100'), requisicao('/servicos').catch(() => []), requisicao('/estoque/itens').catch(() => [])]);
    dadosEmCache.clientes = clientes; dadosEmCache.servicos = servicos; dadosEmCache.estoque = estoque;
    const veiculos = (await Promise.all(clientes.map((c) => requisicao(`/veiculos?clienteId=${c.id}`).catch(() => [])))).flat();
    dadosEmCache.veiculos = veiculos;
    mostrarTabela(['Número', 'Cliente', 'Veículo', 'Status', 'Recebida em', 'Ações'], ordens.map((o) => { const c = clientes.find((x) => x.id === o.clienteId); const v = veiculos.find((x) => x.id === o.veiculoId); return `<tr><td><strong>${esc(o.numero)}</strong></td><td>${esc(c?.nome || o.clienteId)}</td><td>${esc(v ? `${v.marca} ${v.modelo} · ${valorPlaca(v)}` : o.veiculoId)}</td><td><span class="status-pill">${esc(statusNome(o.status))}</span></td><td>${formatarData(o.recebidaEm)}</td><td class="row-actions"><button data-open-os="${o.id}">Detalhes e ações</button></td></tr>`; }));
    document.querySelectorAll('[data-open-os]').forEach((b) => b.addEventListener('click', () => abrirDetalheOS(b.dataset.openOs)));
  } catch (e) { erroView(e); }
}
async function abrirFormularioOrdem() {
  try {
    const clientes = dadosEmCache.clientes || await requisicao('/clientes?pagina=0&tamanho=100');
    const veiculos = dadosEmCache.veiculos || (await Promise.all(clientes.map((c) => requisicao(`/veiculos?clienteId=${c.id}`).catch(() => [])))).flat();
    if (!clientes.length || !veiculos.length) { aviso('Cadastre um cliente e pelo menos um veículo antes de abrir uma OS.', true); return; }
    abrirFormulario('Abrir ordem de serviço', [
      { name: 'clienteId', label: 'Cliente', type: 'select', required: true, options: clientes.map((c) => [c.id, `${c.nome} · ${c.documento}`]) },
      { name: 'veiculoId', label: 'Veículo', type: 'select', required: true, options: veiculos.map((v) => [v.id, `${v.marca} ${v.modelo} · ${valorPlaca(v)}`]) },
      { name: 'descricaoInicial', label: 'Relato do cliente', type: 'textarea', required: true, maxlength: 1000 },
    ], async (d) => { const criada = await requisicao('/ordens-servico', { method: 'POST', body: JSON.stringify(d) }); sessionStorage.setItem(`oficina.os.${criada.ordemServico.id}`, criada.codigoAcompanhamento); dialog.close(); await carregarOrdens(); await abrirDetalheOS(criada.ordemServico.id); aviso(`OS ${criada.ordemServico.numero} criada. O código está nos detalhes da ordem.`); });
  } catch (e) { aviso(e.message, true); }
}

async function abrirDetalheOS(id, codigoAcompanhamento = null) {
  try {
    const os = await requisicao(codigoAcompanhamento ? `/ordens-servico/acompanhamento/${encodeURIComponent(codigoAcompanhamento)}` : `/ordens-servico/${id}`);
    const servicosOS = Array.isArray(os.servicos) ? os.servicos : [];
    const materiaisOS = Array.isArray(os.materiais) ? os.materiais : [];
    const historicoOS = Array.isArray(os.historicoStatus) ? os.historicoStatus : [];
    const orcamentoAtual = os.orcamento || null;
    const cliente = (dadosEmCache.clientes || []).find((item) => item.id === os.clienteId) || null;
    const veiculo = (dadosEmCache.veiculos || []).find((item) => item.id === os.veiculoId) || null;
    let botoes = '';
    if (os.status === 'RECEBIDA') botoes += `<button class="button button-primary" data-os-action="diagnostico/iniciar">Iniciar diagnóstico</button>`;
    if (os.status === 'EM_DIAGNOSTICO') {
      botoes += `<button class="button button-quiet" data-os-action="diagnostico">Registrar diagnóstico</button><button class="button button-quiet" data-os-action="servicos">Adicionar serviço</button><button class="button button-quiet" data-os-action="materiais">Adicionar material</button>`;
      botoes += orcamentoAtual?.status === 'GERADO' ? `<button class="button button-primary" data-os-action="orcamentos/disponibilizar">Disponibilizar orçamento</button>` : `<button class="button button-primary" data-os-action="orcamentos">Gerar orçamento</button>`;
    }
    if (os.status === 'AGUARDANDO_APROVACAO') {
      const codigoDisponivel = codigoAcompanhamento || sessionStorage.getItem(`oficina.os.${os.id}`);
      if (orcamentoAtual?.status === 'GERADO') botoes += `<button class="button button-primary" data-os-action="orcamentos/disponibilizar">Enviar orçamento</button>`;
      else if (orcamentoAtual?.status === 'APROVADO') botoes += `<button class="button button-primary" data-os-action="execucao/iniciar">Iniciar execução</button>`;
      else if (orcamentoAtual?.status === 'DISPONIBILIZADO' && codigoDisponivel) botoes += `<button class="button button-quiet" data-os-decision="aprovar">Aprovar orçamento (cliente)</button><button class="button button-quiet" data-os-decision="rejeitar">Rejeitar orçamento (cliente)</button>`;
      else botoes += '<span class="approval-wait">Aguardando decisão do cliente pelo código de acompanhamento.</span>';
    }
    if (os.status === 'EM_EXECUCAO') botoes += `<button class="button button-primary" data-os-action="finalizar">Finalizar serviço</button>`;
    if (os.status === 'FINALIZADA') botoes += `<button class="button button-primary" data-os-action="entrega">Registrar entrega</button>`;
    if (!['CANCELADA', 'ENTREGUE', 'EM_EXECUCAO', 'FINALIZADA'].includes(os.status)) botoes += `<button class="button button-danger" data-os-action="cancelar">Cancelar OS</button>`;
    const itensOS = [...servicosOS.map((x) => `<span>${esc(x.nome)} · ${moeda(x.valorUnitario)}</span>`), ...materiaisOS.map((x) => `<span>${esc(x.descricao)} · ${x.quantidade} ${esc(x.unidadeMedida)}</span>`)].join('') || '<span>Sem itens adicionados.</span>';
    const resumoOrcamento = orcamentoAtual
      ? `<span>${esc(statusNome(orcamentoAtual.status))} · ${moeda(orcamentoAtual.valorTotal)} · gerado em ${formatarData(orcamentoAtual.geradoEm)}</span>`
      : '<span>Nenhum orçamento gerado.</span>';
    const detalhe = document.createElement('dialog'); detalhe.className = 'order-dialog detail-dialog'; detalhe.innerHTML = `<div class="dialog-card"><button class="dialog-close" aria-label="Fechar">×</button><p class="eyebrow">ORDEM DE SERVIÇO · ${esc(os.numero)}</p><h2>${esc(cliente?.nome || 'Cliente')} · ${esc(statusNome(os.status))}</h2><p class="dialog-copy">${esc(veiculo ? `${veiculo.marca} ${veiculo.modelo} · ${valorPlaca(veiculo)}` : os.veiculoId)}</p><div class="detail-description"><strong>Relato inicial</strong><p>${esc(os.descricaoInicial)}</p>${os.diagnostico ? `<strong>Diagnóstico</strong><p>${esc(os.diagnostico)}</p>` : ''}</div><div class="detail-list"><strong>Serviços e materiais</strong>${itensOS}</div><div class="detail-list"><strong>Orçamento</strong>${resumoOrcamento}</div><div class="detail-timeline"><strong>Histórico</strong>${historicoOS.map((x) => `<span><i></i>${esc(statusNome(x.status))} · ${formatarData(x.atualizadoEm)} · ${esc(x.ator)}</span>`).join('')}</div><div class="dialog-actions detail-actions">${botoes}</div></div>`;
    const codigoExibido = codigoAcompanhamento || sessionStorage.getItem(`oficina.os.${os.id}`);
    if (codigoExibido) {
      const caixaCodigo = document.createElement('div'); caixaCodigo.className = 'detail-code';
      caixaCodigo.innerHTML = `<span><strong>Código de acompanhamento</strong><small>Compartilhe com o cliente para consultar e decidir o orçamento.</small><code>${esc(codigoExibido)}</code></span><button type="button" class="button button-quiet">Copiar</button>`;
      detalhe.querySelector('.dialog-copy').after(caixaCodigo);
      caixaCodigo.querySelector('button').addEventListener('click', async () => { try { await navigator.clipboard.writeText(codigoExibido); aviso('Código de acompanhamento copiado.'); } catch { aviso(`Código: ${codigoExibido}`); } });
    }
    document.body.append(detalhe); detalhe.showModal();
    detalhe.querySelector('.dialog-close').addEventListener('click', () => detalhe.close());
    detalhe.addEventListener('close', () => detalhe.remove());
    detalhe.querySelectorAll('[data-os-decision]').forEach((b) => b.addEventListener('click', async () => {
      const codigo = codigoAcompanhamento || sessionStorage.getItem(`oficina.os.${os.id}`);
      if (!codigo) { aviso('Para aprovar ou rejeitar, informe o código de acompanhamento recebido na abertura da OS.', true); return; }
      try {
        await requisicao(`/ordens-servico/acompanhamento/${encodeURIComponent(codigo)}/orcamento/${b.dataset.osDecision}`, { method: 'POST' });
        detalhe.close(); aviso(`Orçamento ${b.dataset.osDecision === 'aprovar' ? 'aprovado' : 'rejeitado'} pelo fluxo de acompanhamento.`); await carregarOrdens(); await abrirDetalheOS(os.id, codigoAcompanhamento);
      } catch (e) { aviso(e.message, true); }
    }));
    detalhe.querySelectorAll('[data-os-action]').forEach((b) => b.addEventListener('click', async () => {
      const acao = b.dataset.osAction;
      if (acao === 'diagnostico' || acao === 'servicos' || acao === 'materiais') {
        detalhe.close();
        if (acao === 'diagnostico') abrirFormulario('Registrar diagnóstico', [{ name: 'texto', label: 'Diagnóstico técnico', type: 'textarea', required: true, maxlength: 3000 }], async (d) => { await requisicao(`/ordens-servico/${id}/diagnostico`, { method: 'POST', body: JSON.stringify(d) }); aviso('Diagnóstico salvo.'); abrirDetalheOS(id); });
        if (acao === 'servicos') { const lista = dadosEmCache.servicos || await requisicao('/servicos'); abrirFormulario('Adicionar serviço à OS', [{ name: 'servicoId', label: 'Serviço', type: 'select', required: true, options: lista.map((s) => [s.id, `${s.nome} · ${moeda(s.valorBase)}`]) }], async (d) => { await requisicao(`/ordens-servico/${id}/servicos`, { method: 'POST', body: JSON.stringify(d) }); aviso('Serviço adicionado à OS.'); abrirDetalheOS(id); }); }
        if (acao === 'materiais') { const lista = dadosEmCache.estoque || await requisicao('/estoque/itens'); abrirFormulario('Adicionar material à OS', [{ name: 'itemEstoqueId', label: 'Item', type: 'select', required: true, options: lista.map((i) => [i.id, `${i.descricao} · disponível ${i.quantidadeDisponivel} ${i.unidadeMedida}`]) }, { name: 'quantidade', label: 'Quantidade', type: 'number', min: '.001', step: '.001', required: true }], async (d) => { await requisicao(`/ordens-servico/${id}/materiais`, { method: 'POST', body: JSON.stringify({ ...d, quantidade: Number(d.quantidade) }) }); aviso('Material adicionado à OS.'); abrirDetalheOS(id); }); }
        return;
      }
      try {
        let path = `/ordens-servico/${id}/${acao}`; let method = 'POST'; let body;
        if (acao === 'orcamentos/disponibilizar') path = `/ordens-servico/${id}/orcamentos/disponibilizar`;
        if (acao === 'orcamentos') path = `/ordens-servico/${id}/orcamentos`;
        if (acao === 'cancelar') { if (!confirm('Cancelar esta ordem de serviço?')) return; method = 'DELETE'; }
        const resultado = await requisicao(path, { method, ...(body ? { body: JSON.stringify(body) } : {}) });
        if (acao === 'orcamentos') aviso(`Orçamento versão ${resultado.versao} gerado.`);
        detalhe.close(); aviso('Etapa da ordem atualizada.'); await abrirDetalheOS(id); await carregarOrdens();
      } catch (e) { aviso(e.message, true); }
    }));
  } catch (e) { aviso(e.message, true); }
}

async function carregarIndicadores() {
  const hoje = new Date(); const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().slice(0, 10); const fim = hoje.toISOString().slice(0, 10);
  estruturaView('Indicadores', 'Tempo médio de serviços concluídos no período.', '<div class="indicator-dates"><label>De <input id="indicator-start" type="date" value="' + inicio + '"></label><label>Até <input id="indicator-end" type="date" value="' + fim + '"></label><button class="button button-primary" id="run-indicators">Consultar</button></div>');
  document.querySelector('#run-indicators').addEventListener('click', consultarIndicadores);
  await consultarIndicadores();
}
async function consultarIndicadores() {
  try {
    const inicio = document.querySelector('#indicator-start').value; const fim = document.querySelector('#indicator-end').value;
    const dados = await requisicao(`/indicadores/tempo-medio-servicos?inicio=${inicio}&fim=${fim}`);
    const catalogo = dadosEmCache.servicos || await requisicao('/servicos').catch(() => []);
    mostrarTabela(['Serviço', 'Ordens concluídas', 'Tempo médio'], dados.map((x) => `<tr><td><strong>${esc(catalogo.find((s) => s.id === x.servicoId)?.nome || x.servicoId)}</strong></td><td>${esc(x.ordensConcluidas)}</td><td>${Math.floor(x.tempoMedioSegundos / 3600)}h ${Math.floor((x.tempoMedioSegundos % 3600) / 60)}min</td></tr>`), `${dados.length} indicador(es) · ${inicio} a ${fim}`);
  } catch (e) { erroView(e); }
}
function carregarConfiguracoes() {
  estruturaView('Configurações', 'Sessão e conexão com o backend da Oficina Digital.', '');
  mostrarTabela(['Parâmetro', 'Valor', 'Ação'], [`<tr><td><strong>URL da API</strong></td><td>${esc(baseApi())}</td><td class="row-actions"><button id="change-api">Alterar</button></td></tr>`, `<tr><td><strong>Sessão</strong></td><td>Token JWT · Bearer</td><td class="row-actions"><button id="logout" class="danger-link">Sair da conta</button></td></tr>`], 'Conexão atual');
  document.querySelector('#change-api').addEventListener('click', () => { const u = prompt('URL base da API', baseApi()); if (u?.trim()) { localStorage.setItem(apiKey, u.trim()); aviso('URL alterada. Faça login novamente para conectar.'); sair(false); } });
  document.querySelector('#logout').addEventListener('click', () => sair());
}

function abrirFormulario(titulo, campos, aoEnviar) {
  if (dialog.open) dialog.close();
  const form = dialog.querySelector('form');
  form.innerHTML = `<button class="dialog-close" value="cancel" aria-label="Fechar">×</button><p class="eyebrow">OFICINA DIGITAL</p><h2>${esc(titulo)}</h2><div class="form-grid">${campos.map((campo) => campoFormulario(campo)).join('')}</div><p class="form-error" id="form-error" role="alert" aria-live="assertive"></p><div class="dialog-actions"><button class="button button-quiet" type="button" data-cancel>Cancelar</button><button class="button button-primary" type="submit">Salvar</button></div>`;
  form.onsubmit = async (evento) => {
    evento.preventDefault();
    if (!validarFormulario(form)) return;
    form.querySelector('#form-error').textContent = '';
    const dados = Object.fromEntries(new FormData(form));
    const botao = form.querySelector('[type=submit]'); botao.disabled = true; botao.textContent = 'Salvando…';
    try { await aoEnviar(dados); if (dialog.open) dialog.close(); }
    catch (erro) {
      const alerta = form.querySelector('#form-error');
      alerta.textContent = erro.message;
      alerta.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      botao.disabled = false; botao.textContent = 'Salvar';
    }
  };
  form.addEventListener('input', (evento) => {
    const campo = evento.target;
    if (!campo.matches('input, select, textarea')) return;
    campo.setAttribute('aria-invalid', String(!campo.validity.valid));
    const erro = campo.closest('.form-field, label')?.querySelector('.field-error');
    if (erro) erro.textContent = campo.validity.valid ? '' : campo.validationMessage;
    if (campo.validity.valid) form.querySelector('#form-error').textContent = '';
  });
  form.querySelector('[data-cancel]').addEventListener('click', () => dialog.close());
  form.querySelector('.dialog-close').addEventListener('click', (e) => { e.preventDefault(); dialog.close(); });
  dialog.showModal();
}
function campoFormulario(c) {
  const type = c.type || 'text'; const requerido = c.required ? 'required' : '';
  const attrs = [`name="${esc(c.name)}"`, `id="field-${esc(c.name)}"`, `type="${type === 'textarea' || type === 'select' ? 'text' : esc(type)}"`, `value="${esc(c.value ?? '')}"`, requerido, c.min != null ? `min="${esc(c.min)}"` : '', c.max != null ? `max="${esc(c.max)}"` : '', c.step ? `step="${esc(c.step)}"` : '', c.maxlength ? `maxlength="${esc(c.maxlength)}"` : '', c.type === 'date' && c.value ? `value="${esc(String(c.value).slice(0, 10))}"` : ''].filter(Boolean).join(' ');
  let elemento = '';
  if (c.type === 'textarea') elemento = `<textarea name="${esc(c.name)}" id="field-${esc(c.name)}" ${requerido} ${c.maxlength ? `maxlength="${c.maxlength}"` : ''}>${esc(c.value ?? '')}</textarea>`;
  else if (c.type === 'select') elemento = `<select name="${esc(c.name)}" id="field-${esc(c.name)}" ${requerido}><option value="">Selecione…</option>${(c.options || []).map(([value, label]) => `<option value="${esc(value)}" ${String(value) === String(c.value) ? 'selected' : ''}>${esc(label)}</option>`).join('')}</select>`;
  else elemento = `<input ${attrs}/>`;
  return `<label class="form-field">${esc(c.label)}${elemento}<span class="field-error" role="alert"></span></label>`;
}

document.querySelectorAll('[data-view]').forEach((botao) => botao.addEventListener('click', () => abrirView(botao.dataset.view)));
document.querySelector('#mobile-menu').addEventListener('click', (evento) => {
  const aberto = sidebar.classList.toggle('is-open'); evento.currentTarget.setAttribute('aria-expanded', String(aberto));
});
document.querySelector('#notifications').addEventListener('click', () => aviso('Atualize uma seção para consultar os dados mais recentes da API.'));
document.querySelector('.more-button').addEventListener('click', () => abrirView('configuracoes'));
document.addEventListener('keydown', (evento) => { if ((evento.metaKey || evento.ctrlKey) && evento.key.toLowerCase() === 'k') { evento.preventDefault(); searchInput.focus(); } });
searchInput.addEventListener('input', (evento) => {
  const q = evento.target.value.trim().toLocaleLowerCase('pt-BR');
  document.querySelectorAll('#table-content tbody tr, #recent-orders tbody tr').forEach((linha) => { linha.hidden = !linha.textContent.toLocaleLowerCase('pt-BR').includes(q); });
  document.querySelectorAll('#pipeline .order-item').forEach((ordem) => { ordem.hidden = !ordem.textContent.toLocaleLowerCase('pt-BR').includes(q); });
});
document.addEventListener('click', (evento) => {
  const link = evento.target.closest('[data-view]');
  if (link && !link.closest('.main-nav')) { evento.preventDefault(); abrirView(link.dataset.view); }
});
document.querySelector('#order-dialog').addEventListener('click', (evento) => { if (evento.target === dialog) dialog.close(); });

atualizarData();
if (token && Number(sessionStorage.getItem('oficina.jwtExpires') || Infinity) > Date.now()) {
  document.querySelector('.profile-card strong').textContent = sessionStorage.getItem('oficina.email') || 'Equipe da oficina';
  document.querySelector('.profile-card small').textContent = lerPerfil(token);
  carregarInicio().catch((e) => { sair(false); renderLogin(e.message); });
} else { token = null; sessionStorage.removeItem(tokenKey); renderLogin(); }
