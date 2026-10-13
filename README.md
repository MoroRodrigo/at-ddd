# Freela Marketplace

Projeto de referência para um marketplace de contratação de freelancers construído com arquitetura de microsserviços em Java e Spring.

A aplicação representa um cenário em que clientes contratam freelancers para a execução de trabalhos. O núcleo do sistema é o gerenciamento dos contratos firmados entre as partes. A partir desse domínio, outros serviços mantêm informações relacionadas a notificações, reputação e auditoria.

## Visão geral

O sistema é composto por seis aplicações Spring Boot:

- `eureka-server`: registro e descoberta dos serviços.
- `api-gateway`: ponto de entrada HTTP da aplicação.
- `contrato-service`: gerenciamento dos contratos entre clientes e freelancers.
- `notificacao-service`: armazenamento das notificações relacionadas aos contratos.
- `reputacao-service`: manutenção de informações agregadas sobre os freelancers.
- `auditoria-service`: registro de eventos relevantes do sistema.

A infraestrutura local utiliza PostgreSQL e Apache Kafka.

```text
                         +-------------------+
                         |      Cliente      |
                         +---------+---------+
                                   |
                                   | HTTP
                                   v
                         +-------------------+
                         |    API Gateway    |
                         |       :8080       |
                         +---------+---------+
                                   |
                     Service Discovery / Eureka
                                   |
                +------------------+------------------+
                |                  |                  |
                v                  v                  v
       +----------------+  +----------------+  +----------------+
       | contrato       |  | notificacao    |  | reputacao      |
       | service :8081  |  | service :8082  |  | service :8083  |
       +----------------+  +----------------+  +----------------+
                |
                |                       +----------------+
                +---------------------->| auditoria      |
                                        | service :8084  |
                                        +----------------+

                          +-------------------+
                          |       Kafka       |
                          |       :9092       |
                          +-------------------+

                          +-------------------+
                          |    PostgreSQL     |
                          |       :5432       |
                          +-------------------+
```

## Domínio

O domínio principal está no `contrato-service`.

Um contrato representa o vínculo entre um cliente e um freelancer para a execução de um trabalho. Cada contrato possui:

- identificador;
- cliente;
- freelancer;
- título do trabalho;
- valor;
- status;
- data de criação.

Os estados disponíveis são:

```text
ATIVO
ENTREGA_REGISTRADA
CONCLUIDO
CANCELADO
```

O fluxo de negócio previsto pelo modelo é:

```text
ATIVO
  |
  v
ENTREGA_REGISTRADA
  |
  v
CONCLUIDO
```

Um contrato ativo também pode ser cancelado.

O `contrato-service` utiliza uma organização inspirada em Domain-Driven Design, separando domínio, aplicação e infraestrutura.

```text
contrato-service
└── src/main/java/br/com/freela/contrato
    ├── application
    ├── domain
    │   ├── event
    │   ├── model
    │   ├── repository
    │   └── shared
    └── infrastructure
        ├── persistence
        └── web
```

O Aggregate `Contrato` concentra as regras relacionadas às mudanças de estado e produz eventos de domínio. Atualmente existe o evento `ContratoCriado`, que contém as principais informações do contrato no momento da criação.

## Serviços

### contrato-service

Responsável pelo ciclo de vida dos contratos.

Porta:

```text
8081
```

Banco:

```text
contrato_db
```

Principais recursos HTTP:

```text
POST /api/contratos
GET  /api/contratos
GET  /api/contratos/{id}
```

Exemplo de criação de contrato:

```json
{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "freelancerId": "22222222-2222-2222-2222-222222222222",
  "titulo": "Construção de API de pagamentos",
  "valor": 3500.00
}
```

### notificacao-service

Mantém notificações relacionadas aos acontecimentos do marketplace.

Porta:

```text
8082
```

Banco:

```text
notificacao_db
```

As notificações armazenam informações como contrato, destinatário, tipo, mensagem e momento de criação.

### reputacao-service

Mantém informações agregadas sobre a atividade dos freelancers.

Porta:

```text
8083
```

Banco:

```text
reputacao_db
```

Para cada freelancer são mantidos dados como quantidade de contratos concluídos e valor total dos contratos registrados.

Endpoint disponível para consulta:

```text
GET /api/reputacoes
```

### auditoria-service

Responsável pelo armazenamento de registros associados aos eventos do sistema.

Porta:

```text
8084
```

Banco:

```text
auditoria_db
```

Cada registro de auditoria pode armazenar:

- `eventId`;
- `aggregateId`;
- tipo do evento;
- `correlationId`;
- payload original;
- horário de recebimento.

Endpoint disponível para consulta:

```text
GET /api/auditoria
```

## API Gateway

O `api-gateway` é o ponto de entrada HTTP para os microsserviços.

Porta:

```text
8080
```

As rotas configuradas são:

| Caminho | Serviço |
|---|---|
| `/api/contratos/**` | `contrato-service` |
| `/api/notificacoes/**` | `notificacao-service` |
| `/api/reputacoes/**` | `reputacao-service` |
| `/api/auditoria/**` | `auditoria-service` |

O Gateway utiliza Eureka para localizar as instâncias dos serviços.

Também existe suporte ao header:

```text
X-Correlation-Id
```

Quando o header não é enviado pelo cliente, o Gateway gera automaticamente um UUID e o encaminha para o serviço de destino.

## Eureka Server

O Eureka Server mantém o registro das aplicações disponíveis no ambiente.

Porta:

```text
8761
```

Interface web:

```text
http://localhost:8761
```

Os microsserviços utilizam, por padrão:

```text
http://localhost:8761/eureka/
```

como endereço do service registry.

## PostgreSQL

O ambiente utiliza uma única instância PostgreSQL com bancos separados para cada serviço.

```text
Host:     localhost
Porta:    5432
Usuário:  freela
Senha:    freela
```

Bancos criados durante a inicialização:

```text
contrato_db
notificacao_db
reputacao_db
auditoria_db
```

O script de criação dos bancos está em:

```text
infra/postgres/init-databases.sql
```

Os serviços utilizam Hibernate com `ddl-auto: update` para criação e atualização das tabelas locais.

## Apache Kafka

O Apache Kafka é executado em modo KRaft, sem ZooKeeper.

Para aplicações executadas diretamente na máquina:

```text
localhost:9092
```

Para aplicações executadas dentro da rede Docker:

```text
kafka:19092
```

O broker possui listeners separados para comunicação interna e externa.

O ambiente também inclui o Kafka UI.

```text
http://localhost:8090
```

## Logs

Todos os serviços utilizam logs em nível `INFO` com um formato comum contendo data, nível, nome da aplicação, thread, logger e mensagem.

Exemplo:

```text
2026-09-14 14:42:18.431 INFO service=contrato-service thread=http-nio-8081-exec-1 logger=b.c.f.c.a.ContratoApplicationService - contrato.criacao.inicio clienteId=... freelancerId=...
```

O código registra pontos importantes do fluxo, incluindo:

```text
gateway.request.inicio
gateway.request.fim
http.contrato.criar
contrato.criacao.inicio
contrato.dominio.criado
contrato.persistence.save.inicio
contrato.persistence.save.sucesso
contrato.evento.pendente
contrato.criacao.sucesso
reputacao.atualizacao.inicio
reputacao.atualizacao.sucesso
auditoria.registro.inicio
auditoria.registro.sucesso
```

A presença do `correlationId` nas chamadas HTTP permite relacionar logs produzidos durante uma mesma requisição.

## Infraestrutura local

Os serviços de infraestrutura estão definidos em:

```text
infra/docker-compose.yml
```

Para iniciar o ambiente:

```bash
cd infra
docker compose up -d
```

Para verificar os containers:

```bash
docker compose ps
```

Para encerrar:

```bash
docker compose down
```

Os dados do PostgreSQL são mantidos em volume Docker.

Para remover também os dados persistidos:

```bash
docker compose down -v
```

## Execução das aplicações

A partir da raiz do projeto, cada módulo pode ser iniciado separadamente com Maven.

Eureka Server:

```bash
mvn -pl eureka-server spring-boot:run
```

API Gateway:

```bash
mvn -pl api-gateway spring-boot:run
```

Contrato Service:

```bash
mvn -pl contrato-service spring-boot:run
```

Notificação Service:

```bash
mvn -pl notificacao-service spring-boot:run
```

Reputação Service:

```bash
mvn -pl reputacao-service spring-boot:run
```

Auditoria Service:

```bash
mvn -pl auditoria-service spring-boot:run
```

## Portas

| Componente | Porta |
|---|---:|
| API Gateway | `8080` |
| contrato-service | `8081` |
| notificacao-service | `8082` |
| reputacao-service | `8083` |
| auditoria-service | `8084` |
| Eureka Server | `8761` |
| Kafka | `9092` |
| Kafka UI | `8090` |
| PostgreSQL | `5432` |

## Teste básico

Com a infraestrutura e as aplicações em execução, um contrato pode ser criado pelo Gateway:

```bash
curl -i -X POST http://localhost:8080/api/contratos \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: teste-contrato-001' \
  -d '{
    "clienteId": "11111111-1111-1111-1111-111111111111",
    "freelancerId": "22222222-2222-2222-2222-222222222222",
    "titulo": "Construção de API de pagamentos",
    "valor": 3500.00
  }'
```

Consulta dos contratos:

```bash
curl http://localhost:8080/api/contratos
```

Consulta de um contrato específico:

```bash
curl http://localhost:8080/api/contratos/{id}
```

## Tecnologias

```text
Java 21
Spring Boot 4.1
Spring Cloud
Spring Cloud Gateway
Netflix Eureka
Spring Data JPA
PostgreSQL 16
Apache Kafka 4
Docker Compose
Maven
``` 

## Contratos de eventos e tópicos

O `contrato-service` publica eventos em um único tópico:

```text
freela.contratos.eventos.v1
```

Com chave de particionamento baseada em `contratoId` para garantir que eventos do mesmo contrato permaneçam na mesma partição e sejam consumidos em ordem.

### Eventos publicados

| Evento | Tópico | Produtor | Consumidores | Chave |
|---|---|---|---|---|
| `ContratoCriado` | `freela.contratos.eventos.v1` | `contrato-service` | `notificacao-service`, `reputacao-service`, `auditoria-service` | `contratoId` |
| `EntregaRegistrada` | `freela.contratos.eventos.v1` | `contrato-service` | `notificacao-service`, `auditoria-service` | `contratoId` |
| `ContratoConcluido` | `freela.contratos.eventos.v1` | `contrato-service` | `notificacao-service`, `reputacao-service`, `auditoria-service` | `contratoId` |
| `ContratoCancelado` | `freela.contratos.eventos.v1` | `contrato-service` | `notificacao-service`, `auditoria-service` | `contratoId` |

### Estrutura do envelope

```json
{
  "eventId": "1f5f4c77-9b7b-4d48-8df5-aec3d9f02a48",
  "eventType": "ContratoConcluido",
  "contratoId": "a7d5f5f2-31d7-4978-b7f2-07b7d7aa4e8f",
  "occurredAt": "2026-10-12T00:00:00Z",
  "correlationId": "c5af75de-5b31-456f-86d5-69771b24cd56",
  "aggregateVersion": 2,
  "schemaVersion": "v1",
  "payload": {
    "clienteId": "11111111-1111-1111-1111-111111111111",
    "freelancerId": "22222222-2222-2222-2222-222222222222",
    "titulo": "Construção de API de pagamentos",
    "valor": 3500.00,
    "status": "CONCLUIDO"
  }
}
```

## Estratégia de Transactional Outbox

A publicação assíncrona é feita por meio do padrão Transactional Outbox no `contrato-service`.

- Cada alteração de domínio gera eventos em `Contrato`.
- O `ContratoApplicationService` persiste o aggregate e registra o evento no `OutboxEventEntity` na mesma transação.
- O `ContratoEventPublisher` consulta itens pendentes e envia para Kafka em um scheduler.
- O status do evento é atualizado para `PUBLISHED` após sucesso e mantém `attempts`/`failureDetails` em caso de erro.

Arquivos principais:

- [contrato-service/src/main/java/br/com/freela/contrato/application/ContratoApplicationService.java](contrato-service/src/main/java/br/com/freela/contrato/application/ContratoApplicationService.java)
- [contrato-service/src/main/java/br/com/freela/contrato/infrastructure/outbox/OutboxEventEntity.java](contrato-service/src/main/java/br/com/freela/contrato/infrastructure/outbox/OutboxEventEntity.java)
- [contrato-service/src/main/java/br/com/freela/contrato/infrastructure/messaging/ContratoEventPublisher.java](contrato-service/src/main/java/br/com/freela/contrato/infrastructure/messaging/ContratoEventPublisher.java)

## Idempotência persistente e retry/DLT

Cada serviço consumidor usa o `eventId` como identificador único para bloquear reprocessamento duplicado:

- `notificacao-service`: `NotificacaoRepository.existsByEventId()`
- `reputacao-service`: `ReputacaoEventoProcessadoRepository.existsByEventId()`
- `auditoria-service`: `EventoAuditoriaRepository.existsByEventId()`

A falha no consumo usa `DefaultErrorHandler` com backoff fixo e publicação em DLT, com tópicos como:

```text
freela.contratos.notificacao.dlt.v1
freela.contratos.reputacao.dlt.v1
freela.contratos.auditoria.dlt.v1
```

## Logs centralizados e tracing

Os serviços registram logs com o `service`, `traceId`, `spanId`, `correlationId` e identificadores de contrato/evento.

A propagação de correlação é feita em duas camadas:

- HTTP: `X-Correlation-Id` no Gateway e nos controllers.
- Kafka: header `X-Correlation-Id` no `ContratoEventPublisher` e leitura em cada `@KafkaListener`.

O projeto envia spans para Zipkin por meio do Micrometer + Brave e da configuração em cada arquivo `application.yml`.

## Inicialização do ambiente

```bash
cd infra
docker compose up -d
```

Verificação:

```bash
cd infra
docker compose ps
docker compose config
```

Encerramento:

```bash
cd infra
docker compose down
```

Remoção de volumes:

```bash
cd infra
docker compose down -v
```

## Checklist de verificação da AT

| Rubrica da AT | Implementação | Evidência |
|---|---|---|
| Comunicação baseada em eventos | `ContratoEventPublisher`, `ContratoKafkaConfiguration`, `DomainEventEnvelope` | Publica em `freela.contratos.eventos.v1` e mantém chave por contrato |
| Integração dos serviços | `NotificacaoEventConsumer`, `ReputacaoEventConsumer`, `AuditoriaEventConsumer` | Cada serviço consome o tópico e persiste o evento |
| Especificação das mensagens | `docs/arquitetura-eventos.md` e seção acima | Descreve payload, campos e exemplos |
| Concorrência e ordenação | chave `contratoId` e `@KafkaListener` com `concurrency` | Mantém ordem por contrato em partição |
| Duplicidade | `existsByEventId` nos serviços | Reprocessamento idempotente sem duplicação |
| Logs e centralização | `logging.pattern` + Alloy + Loki + Grafana | Logs do serviço e observabilidade centralizada |
| Rastreamento distribuído | Micrometer + Zipkin + `X-Correlation-Id` | Spans exportados para Zipkin |
| Tratamento de falhas | DLT e `DefaultErrorHandler` | Mensagens em DLT para diagnóstico e retry |
| Infraestrutura | `infra/docker-compose.yml` | PostgreSQL + Kafka + Kafka UI + Zipkin + Loki + Grafana |
| Documentação | `README.md` + `docs/arquitetura-eventos.md` | Instruções, tópicos e matrizes de evidência |
| Validação | `mvn clean verify` + testes unitários | Resultado executado e documentado |

## Verificação executada

Comandos executados no projeto:

```bash
cd "C:\Users\rodri\Downloads\at-ddd"
mvn -pl contrato-service -Dtest=ContratoDomainTest test -DfailIfNoTests=false
mvn clean verify
cd infra
docker compose config
```

Resultados verificados:

- `ContratoDomainTest`: 3 testes, 0 falhas, 0 erros.
- Build do reator: `BUILD SUCCESS` para todos os módulos.
- Compose: configuração do Docker validada sem erros de sintaxe do arquivo YAML.
