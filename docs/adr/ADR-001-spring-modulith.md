# ADR-001 - Spring Modulith como arquitetura de módulos

**Status:** Accepted  
**Data:** 2026-05-18

## Contexto

O sistema precisa separar claramente as responsabilidades entre Identity, Orders, Inventory, Notification e Audit, evitando acoplamento direto entre eles. As alternativas consideradas foram:

- **Microserviços**: cada módulo como serviço independente
- **Monolito tradicional**: pacotes sem fronteiras enforçadas
- **Spring Modulith**: monolito modular com limites verificados em tempo de teste

O time tem 5 pessoas, 3 aulas para entregar um fullstack funcional e nenhuma infraestrutura de orquestração disponível.

## Decisão

Usar **Spring Modulith** sobre uma única instância Spring Boot.

## Justificativa

- Microserviços exigem service discovery, deploy independente e comunicação via rede — overhead inviável para o prazo.
- Monolito tradicional não impede acoplamento acidental; qualquer classe pode chamar qualquer outra.
- Spring Modulith verifica os limites de módulo em `ArchitectureTest` (`modules.verify()`), tornando violações um erro de build. Oferece a separação de microserviços com a simplicidade operacional de um monolito.

## Consequências

- Pacotes `internal/` são inacessíveis entre módulos — disciplina enforçada pelo framework.
- A única dependência síncrona cross-module permitida é `Orders → Inventory` via interface pública (`StockCheckPort`).
- Se no futuro um módulo precisar escalar independentemente, a separação já existe; extrair para microserviço é uma refatoração de infraestrutura, não de domínio.
