# ADR-002 - Event Publication Registry interno vs. broker externo

**Status:** Accepted  
**Data:** 2026-05-18

## Contexto

Notification e Audit dependem de eventos publicados por Orders e Inventory. Se o processo cair entre o commit da transação e a entrega do evento, o evento é perdido silenciosamente com `@Async` puro. As alternativas para garantir entrega foram:

- **SQS/SNS (AWS)**: broker externo gerenciado, entrega garantida
- **RabbitMQ / Kafka**: broker auto-hospedado
- **Spring Modulith Event Publication Registry**: tabela `event_publication` na mesma transação do evento de domínio

## Decisão

Usar o **Event Publication Registry do Spring Modulith** (`spring-modulith-events-jpa`).

## Justificativa

- SQS e brokers externos adicionam um serviço a operar, monitorar e pagar — desnecessário para o volume e prazo deste projeto.
- O Event Publication Registry persiste o evento na **mesma transação** que persiste a mudança de estado. Se o processo cair, o registro fica com `completion_date = null` e pode ser reprocessado.
- Nenhum código de domínio muda — a durabilidade é transparente via dependência.
- Se o volume crescer e justificar SQS no futuro, a migração é de infraestrutura (trocar `spring-modulith-events-jpa` por `spring-modulith-events-sqs`), não de lógica de negócio.

## Consequências

- Uma migration adicional cria a tabela `event_publication` (sem schema prefix — gerenciada pelo próprio Modulith).
- Reprocessamento de eventos pendentes deve ser idempotente — requisito já previsto no PRD para Inventory (RF16–RF18).
- A entrega continua sendo in-process; não há garantia de ordem entre listeners diferentes do mesmo evento.
