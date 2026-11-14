# Thindesk — Dashboard Administrativo de Chamados

Atividade prática M2 — Projeto e Desenvolvimento de Software Corporativo (UMC)
Aluno: Derick Alves Maschio — Sistemas de Informação, 8º semestre

## 1. Visão geral

O Thindesk é um sistema de chamados técnicos (Spring Boot + Thymeleaf + MongoDB). Esta entrega adiciona:

- uma **API REST de dashboard** (`/api/dashboard/**`) que calcula indicadores, séries por período, situação, rankings e listas filtradas a partir da coleção `chamados` no MongoDB;
- melhorias na **API de chamados** (`/api/chamados/**`): validação, padronização dos dados e controle automático das datas de abertura e fechamento;
- uma **tela de Dashboard Administrativo** (`/dashboard`) que consome a API via `fetch` e exibe cards, gráficos (Chart.js), filtros e a tabela de chamados recentes.

### Arquitetura

```
Navegador (dashboard.html + dashboard.js + Chart.js)
        │  fetch JSON (sessão autenticada)
        ▼
DashboardController / ChamadoController  ──►  ApiExceptionHandler (erros padronizados)
        │
        ▼
DashboardService / ChamadoService (regras de negócio e cálculos)
        │
        ▼
MongoTemplate / ChamadoRepository  ──►  MongoDB (coleção "chamados")
```

### Principais decisões de análise e projeto

| Decisão | Justificativa |
|---|---|
| Adicionar `dataAbertura`, `dataFechamento` e `prioridade` ao `Chamado` | Sem datas, não dá para analisar por período (RF02) nem calcular o tempo médio de resolução. A prioridade permite destacar chamados críticos pendentes. |
| Status como enum (`ABERTO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`) | A base original misturava `"Aberto"` e `"ABERTO"`. O enum padroniza os valores e aceita entradas como `"em andamento"` ou `"Concluído"`. |
| Filtros aplicados no MongoDB (`MongoTemplate` + `Criteria`) | O filtro é montado dinamicamente só com os parâmetros informados, e o banco devolve apenas os registros relevantes. |
| Cálculos em métodos isolados no `DashboardService` | Recebem uma lista e devolvem o resultado, o que permite testar com JUnit sem banco. |
| DTOs com `record` | Respostas imutáveis e enxutas: a API expõe só o que o dashboard precisa. |
| Endpoints separados por recurso do dashboard | Cada gráfico tem seu endpoint, que também pode ser usado por outros sistemas. O front faz as chamadas em paralelo (`Promise.all`). |
| `@RestControllerAdvice` com `ErroDTO` | Todos os erros da API seguem o mesmo formato JSON e o status HTTP correto. |
| Tratamento da base existente na inicialização | Registros antigos têm o status padronizado e a data de abertura recuperada do `ObjectId`. |
| Dados de exemplo (150 chamados nos últimos 6 meses) | Gerados só quando a base tem menos de 20 chamados, para o dashboard ter o que mostrar. Podem ser desligados em `application.properties`. |
| Autenticação por sessão (form login) + HTTP Basic | O dashboard usa a sessão do login. Ferramentas como Postman e curl usam Basic Auth. |

## 2. Requisitos

### 2.1 Requisitos funcionais

| ID | Requisito | Como foi atendido |
|---|---|---|
| RF01 | Indicadores gerais de chamados técnicos | Cards com total, abertos, em andamento, concluídos, cancelados, taxa de resolução, tempo médio de resolução e críticos pendentes — `GET /api/dashboard/indicadores` |
| RF02 | Análise de chamados atendidos por período (gráficos) | Gráfico de linhas de abertos x concluídos por dia, semana ou mês — `GET /api/dashboard/por-periodo` |
| RF03 | Situação dos chamados | Gráfico de rosca por status — `GET /api/dashboard/por-status` |
| RF04 | Tipos de chamados mais atendidos | Ranking dos tipos (opção "só concluídos") — `GET /api/dashboard/tipos-mais-atendidos`; complemento: ranking por técnico — `GET /api/dashboard/por-tecnico` |
| RF05 | Filtros de consulta | Data início, data fim, status, prioridade, tipo, técnico e agrupamento, aceitos por todos os endpoints do dashboard. As opções dos selects vêm de `GET /api/dashboard/opcoes-filtro` |
| RF06 | Integração com a API | A tela `/dashboard` consome exclusivamente a API REST via `fetch` (JSON) |
| RF07 | Atualização e tratamento de dados | Atualização automática a cada 60 s e botão "Atualizar"; validação e padronização ao criar ou editar chamados; data de fechamento automática; tratamento da base legada (`POST /api/chamados/tratar-dados` e na inicialização); mensagens de erro na tela |

### 2.2 Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF01 | Back-end em Java 21 com Spring Boot 3.4 e persistência em MongoDB (Atlas). |
| RNF02 | API REST com respostas em JSON e status HTTP adequados (200, 201, 400, 401, 404, 500, 503). |
| RNF03 | Segurança: todas as rotas, exceto login e arquivos estáticos, exigem autenticação (Spring Security). |
| RNF04 | Credenciais do banco fora do código-fonte, via variável de ambiente `MONGODB_URI`. |
| RNF05 | Interface responsiva (Bootstrap 5), utilizável em desktop e celular. |
| RNF06 | Desempenho: os filtros são aplicados no banco e o dashboard carrega seus dados em requisições paralelas. |
| RNF07 | Manutenibilidade: camadas separadas (controller, service, repository, dto, exception) e regras de negócio cobertas por testes automatizados. |
| RNF08 | Usabilidade: mensagens de erro claras em português e indicação do horário da última atualização. |
| RNF09 | Segurança no front-end: dados exibidos com `textContent`, sem `innerHTML` com conteúdo vindo da API, o que evita XSS. |

## 3. Modelo de dados

Coleção `chamados` (MongoDB):

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | ObjectId | Identificador |
| `descricao` | String | Obrigatório |
| `status` | String (enum) | `ABERTO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO` (padrão `ABERTO`) |
| `tipo` | String | Ex.: Hardware, Software, Rede (padrão "Não informado") |
| `tecnico` | String | Técnico responsável (padrão "Não atribuído") |
| `usuario` | String | Solicitante |
| `prioridade` | String (enum) | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` (padrão `MEDIA`) |
| `dataAbertura` | Date | Preenchida automaticamente na criação |
| `dataFechamento` | Date | Preenchida ao concluir ou cancelar; limpa ao reabrir |

## 4. Documentação dos endpoints

**Base URL:** `http://localhost:8080`
**Autenticação:** sessão do login (navegador) ou HTTP Basic (`admin` / `thindesk`).

### 4.1 Filtros comuns (todos os `GET /api/dashboard/*`, exceto `opcoes-filtro`)

| Parâmetro | Tipo | Obrigatório | Exemplo | Regra |
|---|---|---|---|---|
| `dataInicio` | data `yyyy-MM-dd` | não | `2026-06-01` | Considera `dataAbertura >= dataInicio 00:00` |
| `dataFim` | data `yyyy-MM-dd` | não | `2026-11-14` | Considera `dataAbertura < dataFim + 1 dia`; não pode ser menor que `dataInicio` |
| `status` | texto | não | `CONCLUIDO` | Aceita variações como `concluído` e `em andamento` |
| `prioridade` | texto | não | `CRITICA` | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` |
| `tipo` | texto | não | `Software` | Comparação exata, sem diferenciar maiúsculas |
| `tecnico` | texto | não | `Maria` | Comparação exata, sem diferenciar maiúsculas |

### 4.2 `GET /api/dashboard/indicadores` — RF01

Exemplo de requisição:
```
curl -u admin:thindesk "http://localhost:8080/api/dashboard/indicadores?dataInicio=2026-06-01&dataFim=2026-11-14"
```
Resposta `200 OK`:
```json
{
  "total": 150,
  "abertos": 5,
  "emAndamento": 27,
  "concluidos": 105,
  "cancelados": 13,
  "criticosPendentes": 10,
  "taxaResolucao": 70.0,
  "tempoMedioResolucaoHoras": 36.3
}
```
- `taxaResolucao` = concluídos ÷ total × 100
- `tempoMedioResolucaoHoras` = média de (`dataFechamento` − `dataAbertura`) dos concluídos
- `criticosPendentes` = prioridade `CRITICA` com status `ABERTO` ou `EM_ANDAMENTO`

### 4.3 `GET /api/dashboard/por-periodo` — RF02

Parâmetro extra: `agrupamento` = `DIA` | `SEMANA` | `MES` (padrão `MES`).
```
curl -u admin:thindesk "http://localhost:8080/api/dashboard/por-periodo?agrupamento=MES&tipo=Software"
```
Resposta `200 OK`:
```json
[
  { "periodo": "2026-09", "abertos": 21, "concluidos": 14 },
  { "periodo": "2026-10", "abertos": 34, "concluidos": 17 },
  { "periodo": "2026-11", "abertos": 8,  "concluidos": 3 }
]
```
`abertos` conta pela data de abertura e `concluidos` pela data de fechamento. Na semana, o formato é `2026-S45` (semana ISO).

### 4.4 `GET /api/dashboard/por-status` — RF03
```
curl -u admin:thindesk "http://localhost:8080/api/dashboard/por-status"
```
Resposta `200 OK` (sempre os 4 status, mesmo com total 0):
```json
[
  { "nome": "ABERTO", "total": 5, "percentual": 3.3 },
  { "nome": "EM_ANDAMENTO", "total": 27, "percentual": 18.0 },
  { "nome": "CONCLUIDO", "total": 105, "percentual": 70.0 },
  { "nome": "CANCELADO", "total": 13, "percentual": 8.7 }
]
```

### 4.5 `GET /api/dashboard/tipos-mais-atendidos` — RF04

| Parâmetro extra | Padrão | Regra |
|---|---|---|
| `limite` | 5 | entre 1 e 100 |
| `apenasConcluidos` | false | `true` considera só chamados concluídos |

```
curl -u admin:thindesk "http://localhost:8080/api/dashboard/tipos-mais-atendidos?limite=3&apenasConcluidos=true"
```
Resposta `200 OK` (ordem decrescente):
```json
[
  { "nome": "Software", "total": 31, "percentual": 29.5 },
  { "nome": "Hardware", "total": 22, "percentual": 21.0 },
  { "nome": "Acesso e Senhas", "total": 17, "percentual": 16.2 }
]
```

### 4.6 `GET /api/dashboard/por-tecnico`
Mesmo formato do ranking de tipos, agrupado por técnico. Parâmetro extra: `limite` (padrão 5).

### 4.7 `GET /api/dashboard/chamados`
Lista os chamados filtrados, do mais recente para o mais antigo. Parâmetro extra: `limite` (padrão 20, máx. 100).
```json
[
  {
    "id": "6735f0c2a1b2c3d4e5f60718",
    "descricao": "Erro ao abrir o sistema",
    "status": "EM_ANDAMENTO",
    "tipo": "Software",
    "tecnico": "Maria",
    "usuario": "Camila",
    "prioridade": "MEDIA",
    "dataAbertura": "2026-11-05T20:30:51",
    "dataFechamento": null
  }
]
```

### 4.8 `GET /api/dashboard/opcoes-filtro` — RF05
```json
{
  "status": ["ABERTO", "EM_ANDAMENTO", "CONCLUIDO", "CANCELADO"],
  "prioridades": ["BAIXA", "MEDIA", "ALTA", "CRITICA"],
  "tipos": ["Acesso e Senhas", "E-mail", "Hardware", "Impressora", "Manutenção", "Rede", "Software"],
  "tecnicos": ["Ana", "Carlos", "João", "Maria", "Pedro"]
}
```

### 4.9 API de chamados

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/api/chamados` | Cria um chamado | 201 |
| GET | `/api/chamados` | Lista todos | 200 |
| GET | `/api/chamados/{id}` | Busca por id | 200 |
| PUT | `/api/chamados/{id}` | Atualiza todos os campos | 200 |
| PATCH | `/api/chamados/{id}/status` | Altera só o status | 200 |
| POST | `/api/chamados/tratar-dados` | Padroniza a base existente (RF07) | 200 |

Criar chamado:
```
curl -u admin:thindesk -X POST http://localhost:8080/api/chamados \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Impressora do RH sem toner","tipo":"Impressora","tecnico":"Ana","usuario":"Beatriz","prioridade":"alta"}'
```
Resposta `201 Created`:
```json
{
  "id": "6736a1f0c2b4a51e2f0a9b11",
  "descricao": "Impressora do RH sem toner",
  "status": "ABERTO",
  "tipo": "Impressora",
  "tecnico": "Ana",
  "usuario": "Beatriz",
  "prioridade": "ALTA",
  "dataAbertura": "2026-11-14T14:02:10.512",
  "dataFechamento": null
}
```
Concluir chamado:
```
curl -u admin:thindesk -X PATCH http://localhost:8080/api/chamados/6736a1f0c2b4a51e2f0a9b11/status \
  -H "Content-Type: application/json" -d '{"status":"Concluído"}'
```
A resposta traz `"status": "CONCLUIDO"` e `dataFechamento` preenchida.

### 4.10 Erros possíveis

Formato padrão (`ErroDTO`):
```json
{
  "dataHora": "2026-11-14T14:05:00.123",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "A dataInicio não pode ser posterior à dataFim",
  "caminho": "/api/dashboard/indicadores"
}
```

| Status | Quando acontece | Exemplo de mensagem |
|---|---|---|
| 400 | Data fora do formato `yyyy-MM-dd` | `Valor inválido para o parâmetro dataInicio: 14/11/2026 (use o formato yyyy-MM-dd para datas)` |
| 400 | `dataInicio` maior que `dataFim` | `A dataInicio não pode ser posterior à dataFim` |
| 400 | Status, prioridade ou agrupamento inexistente | `Status inválido: PERDIDO. Valores aceitos: [ABERTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO]` |
| 400 | `limite` fora de 1–100 ou não numérico | `O parâmetro limite deve estar entre 1 e 100` |
| 400 | Chamado sem descrição ou JSON mal formatado | `A descrição do chamado é obrigatória` |
| 401 | Requisição sem login ou credenciais Basic (no navegador, redireciona para `/login`) | — |
| 404 | Id de chamado inexistente | `Chamado não encontrado com id: 999` |
| 503 | MongoDB indisponível | `Banco de dados indisponível no momento` |
| 500 | Erro inesperado | `Erro interno ao processar a requisição` |

## 5. Evidências de testes

Testes executados em 14/11/2026 com a aplicação rodando localmente (Windows 11, Java 21, MongoDB Atlas — cluster AWS São Paulo) e 150 chamados de exemplo gerados na inicialização.

### 5.1 Testes automatizados (JUnit 5 + Mockito + MockMvc)

Comando: `mvnw test`

| Classe | Teste | Verifica | Resultado |
|---|---|---|---|
| DashboardServiceTest | deveCalcularIndicadoresGerais | Totais por status, críticos pendentes, taxa de 40% e tempo médio de 6 h | ✔ |
| DashboardServiceTest | deveRetornarIndicadoresZeradosSemChamados | Sem divisão por zero quando não há dados | ✔ |
| DashboardServiceTest | deveAgruparChamadosPorMes | Abertos e concluídos por mês | ✔ |
| DashboardServiceTest | deveContarTodosOsStatus | Os 4 status com percentual | ✔ |
| DashboardServiceTest | deveMontarRankingDeTiposDoMaiorParaOMenor | Ordenação e limite do ranking | ✔ |
| ChamadoServiceTest | deveNormalizarDadosAoCriarChamado | Espaços, status "Concluído" → `CONCLUIDO`, valores padrão e datas | ✔ |
| ChamadoServiceTest | deveManterDataFechamentoVaziaParaChamadoAberto | Chamado em andamento sem data de fechamento | ✔ |
| ChamadoServiceTest | deveRecusarChamadoSemDescricao | Validação de campo obrigatório | ✔ |
| ChamadoServiceTest | deveRecusarStatusInvalido | Validação do status | ✔ |
| ChamadoServiceTest | deveLancarErroQuandoChamadoNaoExiste | Exceção 404 | ✔ |
| DashboardControllerTest | deveRetornarIndicadores | 200 e JSON de indicadores | ✔ |
| DashboardControllerTest | deveRetornar400ParaDataInvalida | Data em formato errado | ✔ |
| DashboardControllerTest | deveRetornar400QuandoDataInicioMaiorQueDataFim | Intervalo inválido | ✔ |
| DashboardControllerTest | deveRetornar400ParaStatusInvalido | Filtro inválido | ✔ |
| DashboardControllerTest | deveRetornar400ParaAgrupamentoInvalido | Agrupamento inválido | ✔ |
| DashboardControllerTest | deveRetornar400ParaLimiteForaDoIntervalo | Limite inválido | ✔ |

Saída do Maven:
```
[INFO] Running com.pfc.thindesk.controller.DashboardControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.147 s -- in com.pfc.thindesk.controller.DashboardControllerTest
[INFO] Running com.pfc.thindesk.service.ChamadoServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.265 s -- in com.pfc.thindesk.service.ChamadoServiceTest
[INFO] Running com.pfc.thindesk.service.DashboardServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in com.pfc.thindesk.service.DashboardServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

### 5.2 Testes manuais da API

| # | Requisição | Esperado | Obtido |
|---|---|---|---|
| 1 | `GET /api/dashboard/indicadores` | 200 com indicadores | ✔ 200 — total 150, taxa de resolução 73,3%, tempo médio 39,5 h |
| 2 | `GET /api/dashboard/por-periodo?agrupamento=SEMANA` | 200 com semanas | ✔ 200 — semanas 2026-S21 a 2026-S46 |
| 3 | `GET /api/dashboard/por-status?tipo=Software` | 200 com 4 status | ✔ 200 — 2 abertos, 1 em andamento, 20 concluídos, 4 cancelados |
| 4 | `GET /api/dashboard/tipos-mais-atendidos?limite=3` | 200 com 3 tipos | ✔ 200 — Hardware (36), Rede (27), Software (27) |
| 5 | `GET /api/dashboard/indicadores?dataInicio=14/11/2026` | 400 | ✔ 400 — data em formato inválido |
| 6 | `GET /api/dashboard/indicadores?dataInicio=2026-12-01&dataFim=2026-01-01` | 400 | ✔ 400 — intervalo inválido |
| 7 | `POST /api/chamados` sem descrição | 400 | ✔ 400 — "A descrição do chamado é obrigatória" |
| 8 | `GET /api/chamados/999` | 404 | ✔ 404 — chamado não encontrado |
| 9 | `PATCH /api/chamados/{id}/status` com `concluido` | 200 com `dataFechamento` | ✔ 200 — status `CONCLUIDO` e `dataFechamento` preenchida |
| 10 | `GET /api/dashboard/indicadores` sem login | 401 | ✔ 401 |
| 11 | `POST /api/chamados` válido | Chamado criado com valores padrão | ✔ status `ABERTO`, prioridade `MEDIA`, `dataAbertura` automática |

**Teste 1 — Indicadores gerais (RF01)**

![Indicadores](evidencias/01-indicadores.png)

**Teste 2 — Chamados por período, agrupados por semana (RF02)**

![Por período](evidencias/02-periodo.png)

**Teste 3 — Situação dos chamados do tipo Software (RF03 + RF05)**

![Por status](evidencias/03-status.png)

**Teste 4 — Tipos mais atendidos (RF04)**

![Tipos](evidencias/04-tipos.png)

**Teste 5 — Data em formato inválido (400)**

![Erro de data](evidencias/05-erro-data.png)

**Teste 6 — Data de início posterior à data de fim (400)**

![Erro de intervalo](evidencias/06-erro-intervalo.png)

**Teste 7 — Criação de chamado sem descrição (400)**
```
> curl.exe -i -u admin:thindesk -X POST http://localhost:8080/api/chamados -H "Content-Type: application/json" -d '{"tipo":"Rede"}'
HTTP/1.1 400
Content-Type: application/json

{"dataHora":"2026-11-14T13:10:20.9315588","status":400,"erro":"Bad Request","mensagem":"A descrição do chamado é obrigatória","caminho":"/api/chamados"}
```

**Teste 8 — Chamado inexistente (404)**

![Não encontrado](evidencias/08-nao-encontrado.png)

**Teste 9 — Conclusão de chamado com padronização do status (RF07)**

O status foi enviado em minúsculo e sem acento (`concluido`). A API padronizou para `CONCLUIDO` e preencheu a data de fechamento sozinha.
```
> Invoke-RestMethod -Method Patch -Uri http://localhost:8080/api/chamados/6af88508098dfb54cc016e5c/status -Headers $auth -ContentType "application/json" -Body '{"status":"concluido"}'

id             : 6af88508098dfb54cc016e5c
descricao      : Erro ao abrir o sistema
status         : CONCLUIDO
tipo           : Software
tecnico        : Pedro
usuario        : Diego
prioridade     : BAIXA
dataAbertura   : 2026-11-10T23:58:00.48
dataFechamento : 2026-11-14T13:12:06.7687568
```

**Teste 10 — Acesso sem autenticação (401)**
```
> curl.exe -i http://localhost:8080/api/dashboard/indicadores -H "Accept: application/json"
HTTP/1.1 401
WWW-Authenticate: Basic realm="Realm"
Content-Length: 0
```

**Teste 11 — Criação de chamado válido com valores padrão (RF07)**

Sem `status`, `prioridade` e `usuario` informados, a API aplicou os valores padrão e registrou a data de abertura.
```
> Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/chamados -Headers $auth -ContentType "application/json" -Body '{"descricao":"Teste do video","tipo":"Rede","tecnico":"Ana"}'

id             : 6af8885974c6a032b80c1a5a
descricao      : Teste do video
status         : ABERTO
tipo           : Rede
tecnico        : Ana
usuario        : Não informado
prioridade     : MEDIA
dataAbertura   : 2026-11-14T13:12:09.8075386
dataFechamento :
```

### 5.3 Testes da interface

| # | Ação | Esperado | Obtido |
|---|---|---|---|
| 12 | Login com admin / thindesk | Abre `/dashboard` com os dados | ✔ 150 chamados, cards e gráficos carregados |
| 13 | Filtrar tipo = Software, agrupar por semana | Cards, gráficos e tabela atualizados | ✔ 27 chamados, gráfico semanal |
| 14 | Data início maior que data fim | Mensagem de erro na tela, sem chamar a API | ✔ alerta exibido |
| 15 | Criar chamado pela API e aguardar a atualização automática | Total aumenta sozinho | ✔ total aumentou após o teste 11 |

**Teste 12 — Dashboard administrativo (RF01, RF02, RF03, RF06)**

![Dashboard](evidencias/11-dashboard.png)

**Teste 13 — Filtros aplicados (RF05)**

![Filtro](evidencias/12-filtro.png)

**Teste 14 — Validação do intervalo de datas na tela (RF07)**

![Validação](evidencias/13-validacao.png)

**Teste 15 — Atualização automática (RF07)**

![Atualização automática](evidencias/14-atualizacao.png)

## 6. Como executar

1. Java 21 (ou superior) e acesso a um MongoDB (Atlas ou local).
2. Defina a variável de ambiente com a URI do banco:
   - Windows (PowerShell): `$env:MONGODB_URI="mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/?retryWrites=true&w=majority"`
   - IntelliJ: Run → Edit Configurations → Environment variables → `MONGODB_URI=...`
3. Rode `ThindeskApplication` ou `mvnw spring-boot:run`.
4. Acesse `http://localhost:8080/login` (usuário `admin`, senha `thindesk`).
5. Na primeira execução, com a base vazia, são criados 150 chamados de exemplo. Para desligar: `thindesk.dados-exemplo.habilitado=false`.

## 7. Gitflow

| Branch | Uso |
|---|---|
| `main` | Versão entregue |
| `develop` | Integração |
| `feature/api-dashboard` | Entidade, services, endpoints, tratamento de erros e testes |
| `feature/tela-dashboard` | Template, JS, CSS e menu |
| `feature/documentacao` | Este documento e as evidências |
| `release/1.0.0` | Ajustes finais antes do merge na `main` (tag `v1.0.0`) |

## 8. Vídeo de apresentação

Link do vídeo (até 3 minutos): **[inserir link]**
