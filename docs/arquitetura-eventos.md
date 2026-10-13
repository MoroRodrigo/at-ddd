# Arquitetura de eventos e observabilidade

## Visão geral

A plataforma usa um padrão de comunicação assíncrona por eventos com Kafka para desacoplar o microsserviço de contratos dos consumidores que reagem a mudanças relevantes no ciclo de vida do contrato.

O fluxo principal é:

1. O `contrato-service` persiste o aggregate e registra o evento no outbox em uma mesma transação.
2. O publisher agendado lê registros pendentes e publica no tópico `freela.contratos.eventos.v1`.
3. Os serviços `notificacao-service`, `reputacao-service` e `auditoria-service` consomem os eventos por meio de `@KafkaListener`.
4. Cada serviço valida o `eventId` e aplica a regra de idempotência persistente antes de registrar efeitos colaterais.
5. Logs e spans são exportados para Loki/Zipkin com `correlationId` e headers do Kafka.

## Tópicos e chaves

- Tópico principal: `freela.contratos.eventos.v1`
- Chave de particionamento: `contratoId`
- Estratégia: preservar ordem de eventos do mesmo contrato dentro da mesma partição
- Concorrência: `spring.kafka.listener.concurrency` configurada em 3 para cada consumer

## Eventos

| Evento | Descrição | Payload relevante |
|---|---|---|
| `ContratoCriado` | criação do contrato | `clienteId`, `freelancerId`, `titulo`, `valor`, `status` |
| `EntregaRegistrada` | entrega marcada para o trabalho | `status`, `titulo`, `valor` |
| `ContratoConcluido` | contrato finalizado | `status`, `valor`, `freelancerId` |
| `ContratoCancelado` | contrato cancelado | `status`, `titulo`, `valor` |

## Envelope de payload

```json
{
  "eventId": "uuid",
  "eventType": "ContratoConcluido",
  "contratoId": "uuid",
  "occurredAt": "2026-10-12T00:00:00Z",
  "correlationId": "uuid",
  "aggregateVersion": 2,
  "schemaVersion": "v1",
  "payload": {
    "freelancerId": "uuid",
    "valor": 3500.00,
    "status": "CONCLUIDO"
  }
}
```

## Transactional Outbox

A implementação usa a tabela `outbox_events` para garantir que o evento seja gravado junto com a mudança de estado do aggregate.

Critérios da estratégia:

- mesma transação para aggregate + outbox;
- evento serializado em JSON;
- publisher agendado envia itens pendentes;
- evento só é marcado como publicado no banco após confirmação Kafka;
- falha de envio mantém `attempts` e `failureDetails` para diagnóstico.

## Idempotência

Cada consumer persiste o `eventId` em tabela específica:

- `notificacao-service`: `notificacoes` com `eventId` único
- `reputacao-service`: `reputacao_eventos_processados`
- `auditoria-service`: `auditoria_eventos` com `eventId` único

Assim, reprocessamentos do mesmo evento não criam registros duplicados.

## Retry e DLT

Cada consumer define um `DefaultErrorHandler` com `FixedBackOff(2000L, 3L)`, e a falha após as tentativas é enviada para um DLT específico.

DLTs implementados:

- `freela.contratos.notificacao.dlt.v1`
- `freela.contratos.reputacao.dlt.v1`
- `freela.contratos.auditoria.dlt.v1`

## Observabilidade

- Logs: formatados com `service`, `traceId`, `spanId`, `correlationId`
- Gateway: gera `X-Correlation-Id` quando ausente
- Producers: incluem o header `X-Correlation-Id` no Kafka
- Consumers: leem o header para manter a correlação e registro em log
- Tracing: Micrometer + Brave exportando para Zipkin em `http://localhost:9411`
- Centralização: Loki + Grafana + Alloy da infraestrutura local

## Verificação

A implementação foi validada pelos seguintes procedimentos:

- `mvn clean verify` na raiz do projeto
- `mvn -pl contrato-service -Dtest=ContratoDomainTest test -DfailIfNoTests=false`
- `docker compose config` na pasta `infra`

## Referências de arquivos

- `contrato-service/.../application/ContratoApplicationService.java`
- `contrato-service/.../infrastructure/messaging/ContratoEventPublisher.java`
- `contrato-service/.../infrastructure/outbox/OutboxEventEntity.java`
- `notificacao-service/.../NotificacaoEventConsumer.java`
- `reputacao-service/.../ReputacaoEventConsumer.java`
- `auditoria-service/.../AuditoriaEventConsumer.java`
- `infra/docker-compose.yml`
