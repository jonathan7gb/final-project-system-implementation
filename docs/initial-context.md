# Initial Context: internal-parts-portal

## Purpose

Internal system for managing industrial parts requests across four roles: `COLABORADOR`, `APROVADOR`, `ALMOXARIFE`, and `ADMIN`. The flow is: request → approval → warehouse fulfillment, with async notifications and immutable audit logging throughout.

---

## Architecture

Five modules. Four communicate via **domain events** (async). `Orders` makes one **synchronous call** to `Inventory` for stock validation at order creation.

```
Identity   →  (JWT / RBAC)  →  all modules
Orders     →  sync call     →  Inventory (stock check on create)
Orders     →  events        →  Inventory, Notification, Audit
Inventory  →  events        →  Notification
```

**Rule:** `Notification` and `Audit` are **never called directly**. They only consume events.

---

## Domain Events

| Publisher | Event                   | Consumers                                      |
|-----------|-------------------------|------------------------------------------------|
| Orders    | `PedidoCriadoEvent`     | Notification                                   |
| Orders    | `PedidoAprovadoEvent`   | Inventory, Notification, Audit                 |
| Orders    | `PedidoRejeitadoEvent`  | Inventory, Notification, Audit                 |
| Orders    | `PedidoConcluidoEvent`  | Inventory, Audit                               |
| Inventory | `EstoqueInsuficienteEvent` | Notification                                |

---

## Modules & Requirements

### 🔐 Identity
| ID   | Requirement                                                  | Priority |
|------|--------------------------------------------------------------|----------|
| RF01 | Login with email and password                                | High     |
| RF02 | JWT authentication with configurable expiration              | High     |
| RF03 | Role-based access: COLABORADOR, APROVADOR, ALMOXARIFE, ADMIN | High     |
| RF04 | Admin creates, edits, and deactivates users                  | High     |
| RF05 | User views and edits own profile                             | Medium   |

### 📋 Orders
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF06 | Colaborador creates request with parts, quantities, and justification    | High     |
| RF07 | System validates stock availability via sync call to Inventory           | High     |
| RF08 | Colaborador tracks own order status                                      | High     |
| RF09 | Aprovador views pending orders queue                                     | High     |
| RF10 | Aprovador approves or rejects; rejection requires justification          | High     |
| RF11 | Publishes `PedidoAprovadoEvent` on approval                             | High     |
| RF12 | Publishes `PedidoRejeitadoEvent` on rejection                           | High     |
| RF13 | Almoxarife marks approved order as completed after physical separation   | High     |
| RF14 | Publishes `PedidoConcluidoEvent` on completion                          | High     |

### 📦 Inventory
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF15 | Admin/Almoxarife manage parts catalog (code, name, unit, min qty)       | High     |
| RF16 | Reserves quantity on `PedidoAprovadoEvent`                              | High     |
| RF17 | Permanently decrements stock on `PedidoConcluidoEvent`                  | High     |
| RF18 | Releases reservation on `PedidoRejeitadoEvent`                          | High     |
| RF19 | Publishes `EstoqueInsuficienteEvent` when qty falls below minimum        | Medium   |
| RF20 | Almoxarife manually registers stock entries                              | High     |

### 🔔 Notification
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF21 | Emails aprovador on new order (`PedidoCriadoEvent`)                     | High     |
| RF22 | Emails solicitante on approval or rejection                              | High     |
| RF23 | Emails almoxarife when order is approved (item to separate)              | High     |
| RF24 | Emails almoxarife on low stock (`EstoqueInsuficienteEvent`)              | Medium   |

### 🗂️ Audit
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF25 | Records every domain event: type, payload, responsible user, timestamp   | High     |
| RF26 | Admin views audit log filtered by event type and date range              | Medium   |

---

## Order Status Flow

```
PENDENTE → APROVADO → CONCLUIDO
         ↘ REJEITADO
```

---

## Key Constraints

- Stock reservation and release must be **idempotent** (events may be redelivered).
- Audit records are **immutable** — no updates or deletes ever.
- The sync stock check (RF07) is the **only** cross-module synchronous dependency.
- JWT roles must be enforced at the API boundary before any business logic executes.