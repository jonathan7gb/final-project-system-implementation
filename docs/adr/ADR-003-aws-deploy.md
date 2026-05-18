# ADR-003 - Deploy na AWS

**Status:** Accepted  
**Data:** 2026-05-18

## Contexto

A aplicação precisa de um ambiente de deploy acessível pelo frontend (repositório separado) e pelo time para validação. As alternativas consideradas foram:

- **AWS (ECS Fargate + RDS + SES)**: cloud gerenciada
- **Railway / Render / Fly.io**: PaaS simplificado
- **VPS manual (EC2 ou externo)**: servidor autogerenciado

## Decisão

Usar **AWS** com ECS Fargate (compute), RDS PostgreSQL (banco) e SES (email).

## Justificativa

- PaaS como Railway são mais simples, mas limitam controle de rede, secrets e configuração de SMTP — SES exige domínio verificado que plataformas PaaS não gerenciam bem.
- VPS manual exige configuração de SO, atualizações e backups — overhead sem ganho para o escopo.
- AWS oferece os serviços necessários como primitivos gerenciados: RDS cuida de backups e patches do PostgreSQL; Secrets Manager elimina `.env` em produção; ECS Fargate roda containers sem gerenciar EC2; SES é o caminho natural para email transacional na AWS.
- O stack de código não muda para rodar na AWS — apenas variáveis de ambiente apontam para os recursos gerenciados.

## Consequências

- Necessário um **Dockerfile** na raiz do projeto para build da imagem.
- **Spring Boot Actuator** (`/actuator/health`) é obrigatório para o health check do ALB.
- Credenciais (`JWT_SECRET`, `DB_PASSWORD`, `MAIL_PASSWORD`) devem ser armazenadas no Secrets Manager — nunca em variáveis de ambiente hardcoded no task definition do ECS.
- Custo estimado baixo para o volume do projeto (RDS `db.t3.micro` + Fargate Spot).
