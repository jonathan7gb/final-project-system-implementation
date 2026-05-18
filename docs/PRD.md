# PRD — Internal Parts Portal (API)

**Status:** Draft  
**Escopo:** Backend API — triáde Identity + Orders + Inventory (core), Notification (async funcional), Audit (stretch goal)  
**Time:** 5 pessoas · 3 aulas restantes

---

## 1. Objetivo

API REST para gestão de pedidos de peças industriais com fluxo request → approval → fulfillment. Quatro papéis: `EMPLOYEE`, `APPROVER`, `STOREKEEPER`, `ADMIN`.

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

Cada módulo é dono do seu próprio **PostgreSQL schema**. Isso espelha no banco os mesmos limites que o Spring Modulith impõe no código — nenhum módulo acessa tabelas de outro.

| Módulo       | Schema PostgreSQL |
|--------------|------------------|
| identity     | `identity`       |
| orders       | `orders`         |
| inventory    | `inventory`      |
| notification | `notification`   |
| audit        | `audit`          |

Os schemas são criados por um init script do Docker antes do Flyway rodar (ver Seção 11).

**Flyway** gerencia todos os schemas a partir de um único datasource. As migrations usam nomes de tabela qualificados com o schema:

```sql
-- V1__identity_create_users.sql
CREATE TABLE identity.users ( ... );
```

Formato das migrations: `V{n}__{modulo}_{descricao}.sql`  
Local: `src/main/resources/db/migration/`

**Sem FKs cross-schema.** Referências entre módulos são por UUID simples, sem constraint de FK no banco — mesma regra do código.

---

## 4. Módulo: Identity

**Responsabilidade:** autenticação, emissão de JWT, gerenciamento de usuários.

### Entidades

```
-- schema: identity
identity.users
  id          UUID PK
  name        VARCHAR(100) NOT NULL
  email       VARCHAR(150) UNIQUE NOT NULL
  password    VARCHAR(255) NOT NULL  -- bcrypt
  role        ENUM(EMPLOYEE, APPROVER, STOREKEEPER, ADMIN)
  active      BOOLEAN DEFAULT true
  created_at  TIMESTAMP
  updated_at  TIMESTAMP
```

Anotação JPA: `@Table(schema = "identity", name = "users")`

### API pública do módulo (raiz do pacote)

```java
// Contrato síncrono usado pelo Spring Security dos outros módulos
public interface UserDetailsPort { ... }  // extend Spring UserDetailsService
```

### Endpoints

| Método | Path                        | Roles    | Descrição                          |
|--------|-----------------------------|----------|------------------------------------|
| POST   | `/auth/login`               | público  | Autentica, retorna JWT             |
| GET    | `/users`                    | ADMIN    | Lista usuários                     |
| POST   | `/users`                    | ADMIN    | Cria usuário                       |
| PUT    | `/users/{id}`               | ADMIN    | Edita usuário (incluindo role)     |
| PATCH  | `/users/{id}/deactivate`    | ADMIN    | Desativa usuário                   |
| GET    | `/users/me`                 | *        | Perfil próprio                     |
| PUT    | `/users/me`                 | *        | Edita perfil próprio (nome, senha) |

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
-- schema: inventory
inventory.parts
  id           UUID PK
  code         VARCHAR(50) UNIQUE NOT NULL
  name         VARCHAR(150) NOT NULL
  unit         VARCHAR(20) NOT NULL       -- ex: "un", "kg", "m"
  qty_in_stock INT NOT NULL DEFAULT 0
  qty_reserved INT NOT NULL DEFAULT 0    -- reservado por pedidos aprovados
  qty_minimum  INT NOT NULL DEFAULT 0
  active       BOOLEAN DEFAULT true
  created_at   TIMESTAMP
  updated_at   TIMESTAMP

inventory.stock_entries
  id              UUID PK
  part_id         UUID NOT NULL REFERENCES inventory.parts(id)
  quantity        INT NOT NULL
  note            TEXT
  registered_by   UUID NOT NULL          -- ref identity.users (sem FK cross-schema)
  created_at      TIMESTAMP
```

Anotações JPA: `@Table(schema = "inventory", name = "parts")` e `@Table(schema = "inventory", name = "stock_entries")`

### API pública do módulo

```java
// Única interface síncrona consumida por Orders
public interface StockCheckPort {
    StockCheckResult checkAvailability(UUID partId, int requestedQty);
}
```

`StockCheckResult` é um record público com `available: boolean` e `qtyInStock: int`.

### Endpoints

| Método | Path                          | Roles                    | Descrição                      |
|--------|-------------------------------|--------------------------|--------------------------------|
| GET    | `/parts`                      | *                        | Lista peças ativas             |
| GET    | `/parts/{id}`                 | *                        | Detalhe da peça                |
| POST   | `/parts`                      | ADMIN, STOREKEEPER       | Cadastra peça                  |
| PUT    | `/parts/{id}`                 | ADMIN, STOREKEEPER       | Edita peça                     |
| POST   | `/parts/{id}/stock-entries`   | ADMIN, STOREKEEPER       | Entrada manual de estoque      |
| GET    | `/parts/{id}/stock-entries`   | ADMIN, STOREKEEPER       | Histórico de entradas          |

### Regras de estoque

- `qty_available = qty_in_stock - qty_reserved`
- `StockCheckPort` verifica `qty_available >= requestedQty`.
- Reserva (`OrderApprovedEvent`): incrementa `qty_reserved`.
- Conclusão (`OrderCompletedEvent`): decrementa `qty_in_stock` e `qty_reserved`.
- Rejeição (`OrderRejectedEvent`): decrementa `qty_reserved`.
- Todas as operações de estoque são **idempotentes** — verificar estado antes de aplicar.
- Publica `InsufficientStockEvent` quando `qty_in_stock - qty_reserved < qty_minimum` após qualquer movimentação.

### Critérios de aceite

- [ ] Não é possível criar peça com `code` duplicado.
- [ ] `qty_in_stock` e `qty_reserved` nunca ficam negativos.
- [ ] Reprocessar o mesmo evento não altera os valores duas vezes (idempotência).

---

## 6. Módulo: Orders

**Responsabilidade:** ciclo de vida completo de um pedido.

### Entidades

```
-- schema: orders
orders.orders
  id              UUID PK
  requester_id    UUID NOT NULL    -- ref identity.users (sem FK cross-schema)
  status          ENUM(PENDING, APPROVED, REJECTED, COMPLETED)
  justification   TEXT NOT NULL
  rejection_note  TEXT             -- obrigatório se status = REJECTED
  reviewed_by     UUID             -- ref identity.users (sem FK cross-schema)
  reviewed_at     TIMESTAMP
  created_at      TIMESTAMP
  updated_at      TIMESTAMP

orders.order_items
  id          UUID PK
  order_id    UUID NOT NULL REFERENCES orders.orders(id)
  part_id     UUID NOT NULL        -- ref inventory.parts (sem FK cross-schema)
  part_code   VARCHAR(50) NOT NULL -- snapshot no momento do pedido
  part_name   VARCHAR(150) NOT NULL
  quantity    INT NOT NULL
```

> **Sem FKs cross-schema no banco.** Referências entre módulos são por UUID simples. Dados relevantes de outros módulos são copiados como snapshot no momento da criação (ex: `part_code`, `part_name`).

Anotações JPA: `@Table(schema = "orders", name = "orders")` e `@Table(schema = "orders", name = "order_items")`

### Eventos publicados (raiz do pacote `orders/`)

```java
public record OrderCreatedEvent(UUID orderId, UUID requesterId, List<ItemRef> items) {}
public record OrderApprovedEvent(UUID orderId, UUID reviewerId, List<ItemRef> items) {}
public record OrderRejectedEvent(UUID orderId, UUID reviewerId, String rejectionNote, List<ItemRef> items) {}
public record OrderCompletedEvent(UUID orderId, UUID storekeeperId, List<ItemRef> items) {}

public record ItemRef(UUID partId, int quantity) {}
```

### Endpoints

| Método | Path                           | Roles                                      | Descrição                             |
|--------|--------------------------------|--------------------------------------------|---------------------------------------|
| POST   | `/orders`                      | EMPLOYEE                                   | Cria pedido (dispara check de estoque)|
| GET    | `/orders`                      | EMPLOYEE                                   | Lista próprios pedidos                |
| GET    | `/orders/{id}`                 | EMPLOYEE, APPROVER, STOREKEEPER, ADMIN     | Detalhe                               |
| GET    | `/orders/pending`              | APPROVER                                   | Fila de pedidos pendentes             |
| POST   | `/orders/{id}/approve`         | APPROVER                                   | Aprova pedido                         |
| POST   | `/orders/{id}/reject`          | APPROVER                                   | Rejeita (body: `rejectionNote`)       |
| GET    | `/orders/approved`             | STOREKEEPER                                | Lista pedidos aprovados aguardando    |
| POST   | `/orders/{id}/complete`        | STOREKEEPER                                | Marca como concluído                  |

### Fluxo de criação (RF06 + RF07)

```
POST /orders
  → valida payload
  → para cada item: StockCheckPort.checkAvailability(partId, qty)
  → se algum item indisponível: retorna 422 com detalhe por item
  → persiste order com status PENDING
  → publishEvent(OrderCreatedEvent)
```

### Critérios de aceite

- [ ] Pedido com item sem estoque retorna `422` com lista dos itens problemáticos.
- [ ] EMPLOYEE não vê pedidos de outros usuários em `GET /orders`.
- [ ] Apenas pedidos `PENDING` podem ser aprovados ou rejeitados.
- [ ] Apenas pedidos `APPROVED` podem ser concluídos.
- [ ] Rejeição sem `rejectionNote` retorna `400`.
- [ ] Todos os eventos são publicados dentro da mesma transação que persiste a mudança de status.

---

## 7. Módulo: Notification

**Responsabilidade:** envio de emails em resposta a eventos de domínio. Nunca chamado diretamente.

### Consumo de eventos

| Evento                    | Destinatário         | Assunto (sugerido)                           |
|---------------------------|----------------------|----------------------------------------------|
| `OrderCreatedEvent`       | APPROVER(s)          | "New order awaiting approval #..."           |
| `OrderApprovedEvent`      | solicitante          | "Your order has been approved"               |
| `OrderApprovedEvent`      | STOREKEEPER(s)       | "Approved order ready for separation #..."   |
| `OrderRejectedEvent`      | solicitante          | "Your order has been rejected"               |
| `InsufficientStockEvent`  | STOREKEEPER(s)       | "Low stock alert: {part_name}"               |

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
- Persiste em `audit.logs`: `id`, `event_type`, `payload` (JSONB), `user_id`, `occurred_at`.
- Registro imutável — sem UPDATE ou DELETE na tabela.
- RF26 (listagem com filtros) fica para depois do MVP.

Anotação JPA: `@Table(schema = "audit", name = "logs")`

---

## 9. Contrato de Eventos (resumo)

| Evento                    | Publicador  | Consumidores                         |
|---------------------------|-------------|--------------------------------------|
| `OrderCreatedEvent`       | Orders      | Notification                         |
| `OrderApprovedEvent`      | Orders      | Inventory, Notification, Audit       |
| `OrderRejectedEvent`      | Orders      | Inventory, Notification, Audit       |
| `OrderCompletedEvent`     | Orders      | Inventory, Audit                     |
| `InsufficientStockEvent`  | Inventory   | Notification                         |

---

## 10. Sequência de Implementação

```
Aula 1
├── [todos]    Setup: Flyway, estrutura de pacotes, tabelas iniciais
├── [1 pessoa] Identity: entidade, repositório, serviço, endpoints de usuário
├── [1 pessoa] Identity: JWT filter + Spring Security config
├── [1 pessoa] Inventory: entidade, repositório, StockCheckPort, endpoints de catálogo
└── [1 pessoa] Inventory: lógica de estoque + handler de eventos

Aula 2
├── [1 pessoa] Orders: entidade, criação com stock check síncrono
├── [1 pessoa] Orders: aprovação, rejeição, conclusão + publicação de eventos
├── [1 pessoa] Inventory: handlers de OrderApprovedEvent / Rejected / Completed
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

### 1. Adicionar Flyway ao `pom.xml`

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

### 2. Criar `docker/init.sql` — schemas PostgreSQL

```sql
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;
```

Montar no `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    volumes:
      - ./docker/init.sql:/docker-entrypoint-initdb.d/init.sql
      - postgres_data:/var/lib/postgresql/data
```

> O init script só roda na **primeira inicialização** do container (volume vazio). Para recriar do zero: `docker-compose down -v && docker-compose up -d`.

### 3. Configurar Flyway no `application.properties`

```properties
spring.flyway.default-schema=identity
spring.flyway.schemas=identity,orders,inventory,notification,audit
spring.flyway.locations=classpath:db/migration
```

### 4. Estrutura de pacotes `internal/` em cada módulo

Criar subpacote `internal` dentro de cada módulo antes de qualquer classe de domínio.

### 5. `@EnableAsync` na classe principal

```java
@SpringBootApplication
@EnableAsync
public class SystemDeploymentProjectApiApplication { ... }
```

### 6. Spring Mail + Mailhog

Adicionar ao `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Adicionar ao `docker-compose.yml`:

```yaml
  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # UI web
```

Adicionar ao `.env.example`:

```
MAIL_HOST=localhost
MAIL_PORT=1025
```

---

## 12. Decisões em Aberto

| Decisão                                     | Impacto           | Prazo      |
|---------------------------------------------|-------------------|------------|
| Como Notification busca emails por role?    | Notification      | Aula 1     |
| Paginação de listagens (tamanho padrão?)    | Orders, Inventory | Aula 1     |
| Token de refresh ou somente access token?  | Identity          | Aula 1     |
