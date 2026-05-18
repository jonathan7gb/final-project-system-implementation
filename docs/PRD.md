# PRD — Internal Parts Portal (API)

**Status:** Draft  
**Escopo:** Backend API — triáde Identity + Orders + Inventory (core), Notification (async funcional), Audit (stretch goal)  
**Time:** 5 pessoas · 3 aulas restantes

---

## 1. Objetivo

API REST para gestão de pedidos de peças industriais com fluxo request → approval → fulfillment. Quatro papéis: `COLABORADOR`, `APROVADOR`, `ALMOXARIFE`, `ADMIN`.

---

## 2. Fora de Escopo (neste PRD)

- Frontend (repositório separado)
- Templates HTML de email (texto puro é suficiente)
- RF26 — visualização de audit log com filtros (Audit inteiro é stretch goal)
- Paginação avançada em listagens (page/size básico é suficiente)

---

## 3. Regras de Arquitetura — Spring Modulith

Estas regras são verificadas pelo `ArchitectureTest` (`modules.verify()`). Violá-las quebra o build de testes.

### Visibilidade entre módulos

```
com.centroweg.senai.system_deployment_project_api.
├── identity/          ← public API do módulo (interfaces, DTOs, events)
│   └── internal/      ← inacessível por outros módulos
├── orders/
│   └── internal/
├── inventory/
│   └── internal/
├── notification/
│   └── internal/
└── audit/
    └── internal/
```

- Apenas classes **na raiz do pacote do módulo** são visíveis externamente.
- Toda lógica de domínio vai em `internal/` — entities, repositories, services.
- A única chamada síncrona cross-module é `Orders → Inventory`. Deve ser feita via **interface pública** declarada na raiz do pacote `inventory/`.

### Eventos assíncronos

- Publicação: `ApplicationEventPublisher.publishEvent(event)` no módulo de origem.
- Consumo: `@ApplicationEventListener` + `@Async` nos módulos `notification` e `audit`.
- **Notification e Audit nunca recebem chamada direta de nenhum módulo.**
- Os event records ficam na **raiz do módulo publicador** (Orders publica, Inventory publica).

### Banco de dados

Cada módulo é dono das suas tabelas. Prefixo obrigatório por módulo:

| Módulo      | Prefixo de tabela   |
|-------------|---------------------|
| identity    | `idt_`              |
| orders      | `ord_`              |
| inventory   | `inv_`              |
| notification| `ntf_`              |
| audit       | `aud_`              |

Migrations Flyway em `src/main/resources/db/migration/` com formato `V{n}__{modulo}_{descricao}.sql`.

---

## 4. Módulo: Identity

**Responsabilidade:** autenticação, emissão de JWT, gerenciamento de usuários.

### Entidades

```
idt_users
  id          UUID PK
  name        VARCHAR(100) NOT NULL
  email       VARCHAR(150) UNIQUE NOT NULL
  password    VARCHAR(255) NOT NULL  -- bcrypt
  role        ENUM(COLABORADOR, APROVADOR, ALMOXARIFE, ADMIN)
  active      BOOLEAN DEFAULT true
  created_at  TIMESTAMP
  updated_at  TIMESTAMP
```

### API pública do módulo (raiz do pacote)

```java
// Contrato síncrono usado pelo Spring Security dos outros módulos
public interface UserDetailsPort { ... }  // extend Spring UserDetailsService
```

### Endpoints

| Método | Path                  | Roles    | Descrição                          |
|--------|-----------------------|----------|------------------------------------|
| POST   | `/auth/login`         | público  | Autentica, retorna JWT             |
| GET    | `/users`              | ADMIN    | Lista usuários                     |
| POST   | `/users`              | ADMIN    | Cria usuário                       |
| PUT    | `/users/{id}`         | ADMIN    | Edita usuário (incluindo role)     |
| PATCH  | `/users/{id}/deactivate` | ADMIN | Desativa usuário                  |
| GET    | `/users/me`           | *        | Perfil próprio                     |
| PUT    | `/users/me`           | *        | Edita perfil próprio (nome, senha) |

### JWT

- Payload mínimo: `sub` (userId), `role`, `exp`.
- Filtro HTTP valida o token e popula o `SecurityContext` antes de qualquer controller.
- Roles aplicadas via `@PreAuthorize` nos controllers — nunca dentro de services.

### Critérios de aceite

- [ ] Login com credenciais inválidas retorna `401`.
- [ ] Token expirado retorna `401`.
- [ ] Usuário inativo não consegue autenticar.
- [ ] ADMIN não pode se auto-desativar.
- [ ] Senha armazenada como hash bcrypt (nunca em plain text).

---

## 5. Módulo: Inventory

**Responsabilidade:** catálogo de peças e controle de estoque.

### Entidades

```
inv_parts
  id          UUID PK
  code        VARCHAR(50) UNIQUE NOT NULL
  name        VARCHAR(150) NOT NULL
  unit        VARCHAR(20) NOT NULL      -- ex: "un", "kg", "m"
  qty_in_stock INT NOT NULL DEFAULT 0
  qty_reserved INT NOT NULL DEFAULT 0   -- reservado por pedidos aprovados
  qty_minimum  INT NOT NULL DEFAULT 0
  active      BOOLEAN DEFAULT true
  created_at  TIMESTAMP
  updated_at  TIMESTAMP

inv_stock_entries
  id          UUID PK
  part_id     UUID FK inv_parts
  quantity    INT NOT NULL
  note        TEXT
  registered_by UUID FK idt_users
  created_at  TIMESTAMP
```

### API pública do módulo

```java
// Única interface síncrona consumida por Orders
public interface StockCheckPort {
    StockCheckResult checkAvailability(UUID partId, int requestedQty);
}
```

`StockCheckResult` é um record público com `available: boolean` e `qtyInStock: int`.

### Endpoints

| Método | Path                        | Roles                    | Descrição                      |
|--------|-----------------------------|--------------------------|--------------------------------|
| GET    | `/parts`                    | *                        | Lista peças ativas             |
| GET    | `/parts/{id}`               | *                        | Detalhe da peça                |
| POST   | `/parts`                    | ADMIN, ALMOXARIFE        | Cadastra peça                  |
| PUT    | `/parts/{id}`               | ADMIN, ALMOXARIFE        | Edita peça                     |
| POST   | `/parts/{id}/stock-entries` | ADMIN, ALMOXARIFE        | Entrada manual de estoque      |
| GET    | `/parts/{id}/stock-entries` | ADMIN, ALMOXARIFE        | Histórico de entradas          |

### Regras de estoque

- `qty_available = qty_in_stock - qty_reserved`
- `StockCheckPort` verifica `qty_available >= requestedQty`.
- Reserva (`PedidoAprovadoEvent`): incrementa `qty_reserved`.
- Conclusão (`PedidoConcluidoEvent`): decrementa `qty_in_stock` e `qty_reserved`.
- Rejeição (`PedidoRejeitadoEvent`): decrementa `qty_reserved`.
- Todas as operações de estoque são **idempotentes** — verificar estado antes de aplicar.
- Publica `EstoqueInsuficienteEvent` quando `qty_in_stock - qty_reserved < qty_minimum` após qualquer movimentação.

### Critérios de aceite

- [ ] Não é possível criar peça com `code` duplicado.
- [ ] `qty_in_stock` e `qty_reserved` nunca ficam negativos.
- [ ] Reprocessar o mesmo evento não altera os valores duas vezes (idempotência).

---

## 6. Módulo: Orders

**Responsabilidade:** ciclo de vida completo de um pedido.

### Entidades

```
ord_orders
  id              UUID PK
  requester_id    UUID NOT NULL    -- ref a idt_users (sem FK cross-module)
  status          ENUM(PENDENTE, APROVADO, REJEITADO, CONCLUIDO)
  justification   TEXT NOT NULL
  rejection_note  TEXT             -- obrigatório se status = REJEITADO
  reviewed_by     UUID             -- ref a idt_users
  reviewed_at     TIMESTAMP
  created_at      TIMESTAMP
  updated_at      TIMESTAMP

ord_order_items
  id          UUID PK
  order_id    UUID FK ord_orders
  part_id     UUID NOT NULL        -- ref a inv_parts (sem FK cross-module)
  part_code   VARCHAR(50) NOT NULL -- snapshot no momento do pedido
  part_name   VARCHAR(150) NOT NULL
  quantity    INT NOT NULL
```

> **Sem FKs cross-module no banco.** Referências entre módulos são por UUID com snapshot de dados relevantes (ex: `part_code`, `part_name` no item do pedido).

### Eventos publicados (raiz do pacote `orders/`)

```java
public record PedidoCriadoEvent(UUID orderId, UUID requesterId, List<ItemRef> items) {}
public record PedidoAprovadoEvent(UUID orderId, UUID reviewerId, List<ItemRef> items) {}
public record PedidoRejeitadoEvent(UUID orderId, UUID reviewerId, String rejectionNote, List<ItemRef> items) {}
public record PedidoConcluidoEvent(UUID orderId, UUID almoxarifeId, List<ItemRef> items) {}

public record ItemRef(UUID partId, int quantity) {}
```

### Endpoints

| Método | Path                         | Roles                  | Descrição                            |
|--------|------------------------------|------------------------|--------------------------------------|
| POST   | `/orders`                    | COLABORADOR            | Cria pedido (dispara check de estoque)|
| GET    | `/orders`                    | COLABORADOR            | Lista próprios pedidos               |
| GET    | `/orders/{id}`               | COLABORADOR, APROVADOR, ALMOXARIFE, ADMIN | Detalhe       |
| GET    | `/orders/pending`            | APROVADOR              | Fila de pedidos pendentes            |
| POST   | `/orders/{id}/approve`       | APROVADOR              | Aprova pedido                        |
| POST   | `/orders/{id}/reject`        | APROVADOR              | Rejeita (body: `rejectionNote`)      |
| GET    | `/orders/approved`           | ALMOXARIFE             | Lista pedidos aprovados aguardando   |
| POST   | `/orders/{id}/complete`      | ALMOXARIFE             | Marca como concluído                 |

### Fluxo de criação (RF06 + RF07)

```
POST /orders
  → valida payload
  → para cada item: StockCheckPort.checkAvailability(partId, qty)
  → se algum item indisponível: retorna 422 com detalhe por item
  → persiste order com status PENDENTE
  → publishEvent(PedidoCriadoEvent)
```

### Critérios de aceite

- [ ] Pedido com item sem estoque retorna `422` com lista dos itens problemáticos.
- [ ] COLABORADOR não vê pedidos de outros usuários em `GET /orders`.
- [ ] Apenas pedidos `PENDENTE` podem ser aprovados ou rejeitados.
- [ ] Apenas pedidos `APROVADO` podem ser concluídos.
- [ ] Rejeição sem `rejectionNote` retorna `400`.
- [ ] Todos os eventos são publicados dentro da mesma transação que persiste a mudança de status.

---

## 7. Módulo: Notification

**Responsabilidade:** envio de emails em resposta a eventos de domínio. Nunca chamado diretamente.

### Consumo de eventos

| Evento                   | Destinatário         | Assunto (sugerido)                        |
|--------------------------|----------------------|-------------------------------------------|
| `PedidoCriadoEvent`      | APROVADOR(es)        | "Novo pedido aguardando aprovação #..."   |
| `PedidoAprovadoEvent`    | solicitante          | "Seu pedido foi aprovado"                 |
| `PedidoAprovadoEvent`    | ALMOXARIFE(s)        | "Pedido aprovado para separação #..."     |
| `PedidoRejeitadoEvent`   | solicitante          | "Seu pedido foi rejeitado"                |
| `EstoqueInsuficienteEvent`| ALMOXARIFE(s)       | "Estoque baixo: {part_name}"              |

### Implementação

- `@ApplicationEventListener` + `@Async` em cada handler.
- Spring Mail (`spring-boot-starter-mail`) com Mailhog para dev.
- Configuração via `application.properties`:
  ```
  spring.mail.host=${MAIL_HOST:localhost}
  spring.mail.port=${MAIL_PORT:1025}
  ```
- Corpo dos emails: **texto puro** neste ciclo — sem templates HTML.
- Para saber os destinatários por role, Notification busca os emails via `UserDetailsPort` (interface pública de Identity).

### Critérios de aceite

- [ ] Falha no envio de email não propaga exceção para o módulo publicador.
- [ ] Emails chegam no Mailhog em ambiente local para cada evento.

---

## 8. Módulo: Audit *(stretch goal)*

Implementar apenas se Identity + Orders + Inventory + Notification estiverem funcionais.

- `@ApplicationEventListener` + `@Async` consome todos os eventos de domínio.
- Persiste em `aud_logs`: `id`, `event_type`, `payload` (JSON), `user_id`, `occurred_at`.
- Registro imutável — sem UPDATE ou DELETE na tabela.
- RF26 (listagem com filtros) fica para depois do MVP.

---

## 9. Contrato de Eventos (resumo)

| Evento                     | Publicador  | Consumidores                         |
|----------------------------|-------------|--------------------------------------|
| `PedidoCriadoEvent`        | Orders      | Notification                         |
| `PedidoAprovadoEvent`      | Orders      | Inventory, Notification, Audit       |
| `PedidoRejeitadoEvent`     | Orders      | Inventory, Notification, Audit       |
| `PedidoConcluidoEvent`     | Orders      | Inventory, Audit                     |
| `EstoqueInsuficienteEvent` | Inventory   | Notification                         |

---

## 10. Sequência de Implementação

```
Aula 1
├── [todos]   Setup: Flyway, estrutura de pacotes, tabelas iniciais
├── [1 pessoa] Identity: entidade, repositório, serviço, endpoints de usuário
├── [1 pessoa] Identity: JWT filter + Spring Security config
├── [1 pessoa] Inventory: entidade, repositório, StockCheckPort, endpoints de catálogo
└── [1 pessoa] Inventory: lógica de estoque + handler de eventos

Aula 2
├── [1 pessoa] Orders: entidade, criação com stock check síncrono
├── [1 pessoa] Orders: aprovação, rejeição, conclusão + publicação de eventos
├── [1 pessoa] Inventory: handlers de PedidoAprovadoEvent / Rejeitado / Concluido
├── [1 pessoa] Notification: setup Spring Mail + handlers dos eventos
└── [integração] Smoke tests do fluxo completo

Aula 3
├── [todos]   Correção de bugs de integração
├── [stretch] Audit: handler genérico de todos os eventos
└── [stretch] Testes de módulo com Spring Modulith + Testcontainers
```

---

## 11. Setup Inicial (antes da Aula 1)

Tarefas que devem ser feitas **uma vez** antes do time se dividir:

1. Adicionar Flyway ao `pom.xml`:
   ```xml
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-core</artifactId>
   </dependency>
   <dependency>
     <groupId>org.flywaydb</groupId>
     <artifactId>flyway-database-postgresql</artifactId>
   </dependency>
   ```

2. Criar estrutura de pacotes `internal/` em cada módulo.

3. Criar a primeira migration: `V1__identity_create_users.sql`.

4. Configurar `@EnableAsync` na classe principal para os listeners assíncronos funcionarem.

5. Adicionar Spring Mail ao `pom.xml` e Mailhog ao `docker-compose.yml`.

---

## 12. Decisões em Aberto

| Decisão                                     | Impacto        | Prazo      |
|---------------------------------------------|----------------|------------|
| Como Notification busca emails por role?    | Notification   | Aula 1     |
| Paginação de listagens (tamanho padrão?)    | Orders, Inventory | Aula 1  |
| Token de refresh ou somente access token?  | Identity       | Aula 1     |
