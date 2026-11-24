# AssessFlow: visão inicial

O AssessFlow permite a organizações criar, organizar e aplicar avaliações. Utilizadores integram organizações com papéis OWNER, ADMIN, INSTRUCTOR ou PARTICIPANT. O banco contém questões reutilizáveis; avaliações associam-nas com pontos e ordem. Instrutores podem criar sessões ao vivo a partir de avaliações publicadas. Questões e opções são copiadas para snapshots; participantes entram por código e respondem com atualizações em tempo real.

O frontend React comunica com a API Spring Boot e PostgreSQL. REST transporta comandos e consultas; STOMP/WebSocket distribui eventos. Participantes convidados usam tokens próprios, distintos da autenticação de utilizadores.

- C4: [contexto](diagrams/c4/context.puml), [containers](diagrams/c4/containers.puml)
- Mapa C4 de Comunicação e Fluxos: [utilização normal, sessões e WebSocket](diagrams/c4/live-communication.svg) ([fonte](diagrams/c4/live-communication.puml))
- Mapa C4 de Comunicação e Fluxos — Internos do Framework: [Spring Boot por dentro](diagrams/c4/spring-boot-communication.svg) ([fonte](diagrams/c4/spring-boot-communication.puml))
- [Modelo relacional e cardinalidades](diagrams/database/ER.puml)
- [Casos de uso](diagrams/use-case/assessflow.puml)

Os `.puml` são a fonte de verdade; SVGs são artefactos derivados.

## Mapa C4 de Comunicação e Fluxos

Nome da convenção do kit baseada no C4 de componentes: uma vista geral das famílias de ações e dos seus caminhos de comunicação, agrupados por responsabilidade e cor. Não é um nível adicional do modelo C4. O ficheiro existente `live-communication.puml` mantém o nome para preservar os links.

O [C4 de comunicação](diagrams/c4/live-communication.svg) mostra o funcionamento geral da aplicação, com grupos e relações por cor:

- **Azul — utilização normal:** pedidos REST de autenticação, organizações, questões e avaliações. Não criam sessões ao vivo nem abrem WebSockets.
- **Verde — sessão via HTTP:** endpoints específicos criam a sessão, registam participantes e executam comandos como iniciar ou responder. A criação devolve `sessionId`, mas não abre uma ligação WebSocket no servidor por si só.
- **Roxo — canal de eventos:** ao navegar para a página ao vivo com `sessionId` e token, o navegador abre `/ws`, autentica STOMP e subscreve. Notificações usam esse canal; comandos continuam por HTTP. O broker é interno em `simple` ou usa RabbitMQ em `relay`.

A [vista de funcionamento HTTP e abertura opcional do canal](diagrams/activity/websocket-connection.svg) ([fonte](diagrams/activity/websocket-connection.puml)) mostra a decisão entre continuar com HTTP normal e abrir o WebSocket. Detalha TCP → TLS opcional → Upgrade HTTP → WebSocket → CONNECT/SUBSCRIBE STOMP. STOMP define o formato das mensagens; WebSocket transporta-as em frames sobre TCP, com TLS no caso de WSS. Pedidos HTTP normais podem reutilizar ligações TCP, sem um novo handshake a cada pedido.

No caminho ao vivo:

1. O participante entra por `POST /api/v1/live-sessions/join`, guarda o token e abre `/play/{sessionId}`. O instrutor já tem o token do login e abre a página de controlo da sessão.
2. `useLiveEvents` ativa o cliente quando existem `sessionId` e token. O navegador faz o handshake HTTP em `/ws` (`Upgrade: websocket`, resposta `101`). A ligação já abre na sala de espera; não espera pelo início da primeira questão.
3. Dentro do WebSocket, STOMP envia `CONNECT` com `Authorization: Bearer`. O token está no frame STOMP, não num header personalizado do handshake. Após `CONNECTED`, o cliente subscreve `/topic/host/sessions/{id}` ou `/topic/sessions/{id}`. O servidor valida o acesso à sessão.
4. Comandos do instrutor (`start`, `questions/next`, `questions/end`, `finish`) e respostas do participante (`POST /api/v1/live-sessions/{id}/answers`) continuam por HTTP. O servidor rejeita STOMP `SEND` do cliente.
5. A API valida e persiste a operação. Os eventos de negócio são publicados após o commit, através de `StompLiveSessionNotifier` e `SimpMessagingTemplate`.
6. O broker entrega notificações pela ligação existente: por exemplo, `QUESTION_STARTED` e `QUESTION_RESULTS` a ambos os públicos, e `ANSWER_RECEIVED` ao instrutor. A resposta HTTP e a notificação podem chegar em ordens diferentes.

Cada navegador tem a sua ligação. O frontend configura heartbeats de 10 segundos e tentativas de reconexão após 2 segundos. Ao reconectar, volta a subscrever; ao desmontar a página, desativa o cliente. Atualmente, o hook não dispara uma consulta de estado ao reconectar e não reproduz eventos perdidos. O participante consulta `GET /api/v1/live-sessions/{id}/state` ao carregar a página; alguns eventos também provocam consultas REST. O instrutor consulta os endpoints próprios. Os GETs iniciais e a abertura do socket são independentes.

Fontes verificadas: `web/src/features/live/useLiveEvents.ts`, `api/live.ts`, `pages/JoinPages.tsx`, `pages/HostLivePage.tsx`, `pages/ParticipantPlayPage.tsx` (sob a mesma área de live); e `WebSocketConfig`, `LiveSessionService`, `AfterCommitLiveSessionNotifier` e `StompLiveSessionNotifier` no backend.

## Versão 2 — Spring Boot por dentro

A [segunda versão](diagrams/c4/spring-boot-communication.svg) mantém a primeira e amplia os componentes internos da API. Azul identifica Tomcat, filtros e Spring MVC; verde identifica negócio, transações e dados; roxo identifica WebSocket/STOMP. É uma vista estrutural do percurso principal, não uma listagem de todas as chamadas ou filtros do framework.

| Recurso | Responsabilidade | Configuração observada |
|---|---|---|
| Tomcat NIO e executor | Aceitar sockets, detetar I/O e executar pedidos | Tomcat 11.0.26 declarado no POM; sem tamanho de pool personalizado no projeto |
| `clientInboundChannelExecutor` | Executar handlers de mensagens de entrada | Executor padrão do Spring; `ChannelInterceptor.preSend` valida antes do despacho |
| `clientOutboundChannelExecutor` | Entregar mensagens aos clientes | Executor padrão do Spring, distinto do executor de entrada |
| HikariCP | Reutilizar ligações JDBC ao PostgreSQL | `DB_POOL_MAX=10`, `DB_POOL_MIN=2` por defeito; substituíveis pelo ambiente |
| `brokerChannel` | Encaminhar publicações da aplicação ao broker | Síncrono nesta configuração; não representar como outro pool ativo |
| Listener `AFTER_COMMIT` | Publicar eventos após confirmar a transação | Sem `@Async`; não existe um pool dedicado no código |

O percurso HTTP selecionado passa pelo `PublicRateLimitFilter`, cadeia Spring Security e `DispatcherServlet`. A cadeia configurada integra CORS, `BearerTokenFilter`, `ParticipantTokenFilter` e autorização; a política é `STATELESS`. A criação de uma sessão ao vivo não é a criação de uma `HttpSession`. O filtro de limitação só limita os endpoints públicos configurados; o `LocalPackageSizeFilter` é específico da importação de pacotes Local Live e não foi expandido no desenho. As consultas auxiliares de autenticação/autorização ao banco também não estão desenhadas.

Controllers recebem HTTP e usam os serviços; os proxies transacionais e repositories JPA usam Hibernate e HikariCP. O proxy transacional envolve a execução do método do serviço, apesar de estar agrupado com a infraestrutura de persistência no desenho. `spring.jpa.open-in-view=false`. O pool de ligações ao banco não equivale ao pool de threads de pedidos: uma ligação é obtida e devolvida, e uma sessão WebSocket ociosa não reserva uma thread HTTP ou uma ligação JDBC durante toda a sua vida.

O handshake `/ws` passa pelo percurso HTTP. Depois do Upgrade, os frames seguem pela infraestrutura WebSocket do Tomcat e pelos handlers STOMP, sem executar a cadeia HTTP a cada mensagem. O canal de entrada valida `CONNECT` e `SUBSCRIBE`; `SEND` do cliente é rejeitado. Após commit, o publicador usa `SimpMessagingTemplate` e o broker entrega pela saída STOMP. O modo `simple` usa o broker interno; `relay` comunica com RabbitMQ por STOMP/TCP usando Reactor Netty. A existência desta dependência não transforma a API MVC em servidor WebFlux.

Evidência: `api/pom.xml`, `application.yml`, `SecurityConfig`, `PublicRateLimitFilter`, `LocalPackageSizeFilter`, `WebSocketConfig`, `StompLiveSessionNotifier` e os sources locais do Spring Messaging/WebSocket 7.0.9, versão gerida pelo Spring Boot 4.1.1. Foram conferidos `AbstractMessageBrokerConfiguration`, `ExecutorSubscribableChannel` e `WebSocketMessageBrokerConfigurationSupport`. A topologia de proxy/TLS e valores substituídos em ambiente não são fixados pelo diagrama.
