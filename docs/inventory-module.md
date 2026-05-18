# Módulo Inventory — Documentação

Responsável: catálogo de peças (`inv_parts`) e movimentações de estoque (`inv_stock_entries`).  
Schema PostgreSQL: `inventory`.

## Issues implementadas

| GitHub | ID interno | RF      | Escopo                                                  |
|--------|------------|---------|---------------------------------------------------------|
| #12    | ISSUE-012  | RF15    | CRUD de peças + `StockCheckPort`                        |
| #13    | ISSUE-013  | RF20    | Entradas manuais de estoque                             |
| #14    | ISSUE-014  | RF16–18 | Handlers de eventos de pedido (reserva/baixa/liberação) |
| #15    | ISSUE-015  | RF19    | Publicar `EstoqueInsuficienteEvent`                     |

---

## Arquitetura (Clean + Hexagonal dentro do Modulith)

O Spring Modulith define **fronteiras entre módulos**. Dentro do Inventory usamos camadas hexagonais em `internal/`:

```
inventory/
├── InventoryModule.java          # Marcador @ApplicationModule
├── StockCheckPort.java           # Porta pública (driving port para Orders)
├── StockCheckResult.java         # DTO público de resposta
├── EstoqueInsuficienteEvent.java # Evento publicado para Notification (RF19)
└── internal/
    ├── domain/                   # Núcleo: modelos, exceções, portas de saída
    │   ├── model/                # Part, StockEntry (sem JPA)
    │   ├── repository/           # Interfaces PartRepository, StockEntryRepository
    │   └── exception/
    ├── application/              # Casos de uso / serviços de aplicação
    │   ├── PartService.java
    │   ├── StockEntryService.java
    │   ├── StockCheckService.java       # Implementa StockCheckPort
    │   ├── OrderStockService.java       # RF16–18: reserva/baixa/liberação
    │   ├── OrderStockEventListener.java # Listeners dos eventos de Orders
    │   ├── PartStockMovementService.java
    │   └── LowStockEventPublisher.java  # RF19: publica EstoqueInsuficienteEvent
    ├── infrastructure/           # Adaptadores de saída (persistência)
    │   └── persistence/          # JPA entities, adapters, mappers
    └── api/                      # Adaptadores de entrada (REST)
        ├── PartController.java
        ├── StockEntryController.java
        ├── CurrentUserProvider.java
        └── dto/
```

### Regras Modulith

- Apenas `inventory.*` (raiz) é visível para **Orders** e outros módulos.
- `internal.*` é encapsulado — não importar de outros módulos.
- **Única dependência síncrona recebida:** Orders chama `StockCheckPort`.
- `EstoqueInsuficienteEvent` fica na **raiz** do pacote `inventory/` e é publicado via `ApplicationEventPublisher`.
- Inventory consome eventos da **raiz** do pacote `orders/` (`PedidoAprovadoEvent`, `PedidoConcluidoEvent`, `PedidoRejeitadoEvent`).

### Estoque disponível

```
qty_available = qty_in_stock - qty_reserved
```

`StockCheckPort.checkAvailability(partId, qty)` compara `qty_available >= qty`.

---

## API REST

| Método | Path                          | Roles              | Descrição                    |
|--------|-------------------------------|--------------------|------------------------------|
| GET    | `/parts`                      | autenticado        | Lista peças **ativas**       |
| GET    | `/parts/{id}`                 | autenticado        | Detalhe (inclui inativas)    |
| POST   | `/parts`                      | ADMIN, ALMOXARIFE  | Cadastra peça                |
| PUT    | `/parts/{id}`                 | ADMIN, ALMOXARIFE  | Edita nome, unit, mínimo, active |
| POST   | `/parts/{id}/stock-entries`   | ADMIN, ALMOXARIFE  | Entrada manual (+ estoque)   |
| GET    | `/parts/{id}/stock-entries`   | ADMIN, ALMOXARIFE  | Histórico de entradas        |

### Exemplos (dev — HTTP Basic)

Usuários de desenvolvimento (`config.SecurityConfig`):

| Username (UUID)                          | Senha        | Role        |
|------------------------------------------|--------------|-------------|
| `00000000-0000-0000-0000-000000000001`   | `admin`      | ADMIN       |
| `00000000-0000-0000-0000-000000000002`   | `almoxarife` | ALMOXARIFE |
| `00000000-0000-0000-0000-000000000003`   | `colaborador`| COLABORADOR |

```bash
# Criar peça
curl -u 00000000-0000-0000-0000-000000000001:admin \
  -H "Content-Type: application/json" \
  -d '{"code":"PAR-001","name":"Parafuso M8","unit":"un","qtyMinimum":10}' \
  http://localhost:8080/parts

# Registrar entrada de estoque
curl -u 00000000-0000-0000-0000-000000000002:almoxarife \
  -H "Content-Type: application/json" \
  -d '{"quantity":100,"note":"NF 12345"}' \
  http://localhost:8080/parts/{partId}/stock-entries
```

Substituir HTTP Basic por JWT quando o módulo Identity estiver pronto.

---

## Banco de dados

Migrations Flyway em `src/main/resources/db/migration/`:

| Arquivo | Conteúdo |
|---------|----------|
| `V1__inventory_create_schema_and_tables.sql` | `inv_parts`, `inv_stock_entries` |
| `V2__inventory_processed_order_events.sql` | Idempotência dos handlers de pedido |

Tabelas:

- `inventory.inv_parts`
- `inventory.inv_stock_entries` (FK interna para `inv_parts`)
- `inventory.inv_processed_order_events` — chave `(order_id, event_type)` evita reprocessamento

Constraints garantem `qty_in_stock`, `qty_reserved`, `qty_minimum >= 0` e `quantity > 0` nas entradas.

### Handlers de pedido (RF16–18)

| Evento (Orders) | Efeito no estoque | Idempotência |
|-----------------|-------------------|--------------|
| `PedidoAprovadoEvent` | `qty_reserved += quantity` | PK em `inv_processed_order_events` |
| `PedidoConcluidoEvent` | `qty_in_stock -= qty`, `qty_reserved -= qty` | idem |
| `PedidoRejeitadoEvent` | `qty_reserved -= quantity` | idem |

### Estoque baixo (RF19)

Após qualquer movimentação (entrada manual ou evento de pedido), se `qty_available` **cruzar** de `>= qty_minimum` para `< qty_minimum`, publica:

```java
EstoqueInsuficienteEvent(UUID partId, String partName, int qtyInStock, int qtyMinimum)
```

Não republica enquanto a peça já estiver abaixo do mínimo.

---

## Integração com Orders

**Verificação síncrona (RF07):**

```java
@Autowired StockCheckPort stockCheck;

StockCheckResult result = stockCheck.checkAvailability(partId, requestedQty);
if (!result.available()) {
    // retornar 422 com detalhe do item
}
```

**Publicação de eventos (RF11–14):** o módulo Orders deve publicar na mesma transação da mudança de status:

```java
applicationEventPublisher.publishEvent(
    new PedidoAprovadoEvent(orderId, reviewerId, items));
```

Records públicos em `orders/`: `ItemRef`, `PedidoAprovadoEvent`, `PedidoRejeitadoEvent`, `PedidoConcluidoEvent`.

---

## Testes

| Classe                     | Escopo                                      |
|----------------------------|---------------------------------------------|
| `PartControllerTest`       | CRUD HTTP, código duplicado → 409           |
| `StockEntryControllerTest` | Entrada manual + incremento de estoque      |
| `StockCheckServiceTest`    | Porta pública `StockCheckPort`              |
| `OrderStockServiceTest`    | Reserva, baixa, liberação e idempotência     |
| `LowStockEventPublisherTest` | Publicação de `EstoqueInsuficienteEvent`  |

Executar: `./mvnw test` (Testcontainers sobe PostgreSQL automaticamente).

---

## Referências

- [PRD.md](./PRD.md) — Seção 5 (Inventory)
- [initial-context.md](./initial-context.md) — RF15–RF20
