# Initial Context: internal-parts-portal

## Purpose

Internal system for managing industrial parts requests across four roles: `EMPLOYEE`, `APPROVER`, `STOREKEEPER`, and `ADMIN`. The flow is: request → approval → warehouse fulfillment, with async notifications and immutable audit logging throughout.

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

| Publisher | Event                    | Consumers                                      |
|-----------|--------------------------|------------------------------------------------|
| Orders    | `OrderCreatedEvent`      | Notification                                   |
| Orders    | `OrderApprovedEvent`     | Inventory, Notification, Audit                 |
| Orders    | `OrderRejectedEvent`     | Inventory, Notification, Audit                 |
| Orders    | `OrderCompletedEvent`    | Inventory, Audit                               |
| Inventory | `InsufficientStockEvent` | Notification                                   |

---

## Modules & Requirements

### 🔐 Identity
| ID   | Requirement                                                         | Priority |
|------|---------------------------------------------------------------------|----------|
| RF01 | Login with email and password                                       | High     |
| RF02 | JWT authentication with configurable expiration                     | High     |
| RF03 | Role-based access: EMPLOYEE, APPROVER, STOREKEEPER, ADMIN           | High     |
| RF04 | Admin creates, edits, and deactivates users                         | High     |
| RF05 | User views and edits own profile                                    | Medium   |

### 📋 Orders
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF06 | Employee creates request with parts, quantities, and justification       | High     |
| RF07 | System validates stock availability via sync call to Inventory           | High     |
| RF08 | Employee tracks own order status                                         | High     |
| RF09 | Approver views pending orders queue                                      | High     |
| RF10 | Approver approves or rejects; rejection requires justification           | High     |
| RF11 | Publishes `OrderApprovedEvent` on approval                              | High     |
| RF12 | Publishes `OrderRejectedEvent` on rejection                             | High     |
| RF13 | Storekeeper marks approved order as completed after physical separation  | High     |
| RF14 | Publishes `OrderCompletedEvent` on completion                           | High     |

### 📦 Inventory
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF15 | Admin/Storekeeper manage parts catalog (code, name, unit, min qty)      | High     |
| RF16 | Reserves quantity on `OrderApprovedEvent`                               | High     |
| RF17 | Permanently decrements stock on `OrderCompletedEvent`                   | High     |
| RF18 | Releases reservation on `OrderRejectedEvent`                            | High     |
| RF19 | Publishes `InsufficientStockEvent` when qty falls below minimum         | Medium   |
| RF20 | Storekeeper manually registers stock entries                             | High     |

### 🔔 Notification
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF21 | Emails approver(s) on new order (`OrderCreatedEvent`)                   | High     |
| RF22 | Emails requester on approval or rejection                                | High     |
| RF23 | Emails storekeeper(s) when order is approved (item to separate)         | High     |
| RF24 | Emails storekeeper(s) on low stock (`InsufficientStockEvent`)           | Medium   |

### 🗂️ Audit
| ID   | Requirement                                                              | Priority |
|------|--------------------------------------------------------------------------|----------|
| RF25 | Records every domain event: type, payload, responsible user, timestamp  | High     |
| RF26 | Admin views audit log filtered by event type and date range             | Medium   |

---

## Order Status Flow

```
PENDING → APPROVED → COMPLETED
        ↘ REJECTED
```

---

## Key Constraints

- Stock reservation and release must be **idempotent** (events may be redelivered).
- Audit records are **immutable** — no updates or deletes ever.
- The sync stock check (RF07) is the **only** cross-module synchronous dependency.
- JWT roles must be enforced at the API boundary before any business logic executes.
