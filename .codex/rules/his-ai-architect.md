# HIS + Kangyang AI Architect Workflow

Adapted from `.cursor/rules/his-ai-architect.mdc`.

## First Principle Workflow

Do not write code immediately after receiving a requirement. Follow this sequence:

1. Requirement analysis
2. Business analysis
3. Data analysis
4. Permission analysis
5. AI analysis
6. System design
7. Development design
8. Coding implementation

If prerequisite documents are missing, stop coding and identify the missing content.

## Dual Module Architecture

| Module | Description |
| --- | --- |
| HIS | Hospital information system: outpatient, inpatient, orders, billing, and related workflows. |
| Kangyang | Kangyang service system: in-hospital services and home-visit services. |

Architecture principles:

- The two modules can be deployed independently.
- They share SSO and API gateway.
- Prefer modular monolith inside each module.
- Do not split into microservices only for technical novelty.
- A module may evolve into microservices only after user volume, team size, and module boundaries justify it.
- Shared layer: account authentication, API gateway, and AI access layer.

## HIS Domain Rules

- Default roles: patient, doctor, nurse, caregiver, kangyang therapist, service institution, family member, administrator.
- Every design must consider permission isolation, data isolation, and audit logs.
- Do not allow data access by default.

## Medical Data Rules

Medical records, orders, lab/exam results, nursing/rehabilitation records, and user profiles are sensitive data.
Consider desensitization, permission control, and audit tracing.

## AI Native Design Rules

Every business module must answer:

1. How humans complete the workflow.
2. How AI assists.
3. What data AI needs.
4. Which APIs AI calls.
5. What AI permission boundaries apply.

If these are not answered, the design is incomplete.

## Data Design Rules

For table design, output table purpose, field descriptions, primary key, unique indexes, normal indexes, foreign key relationships, and data lifecycle.
Do not output `CREATE TABLE` directly; provide design first, then DDL.

## API Design Rules

Every API must define functional description, caller, permission requirements, request/response parameters, error codes, and whether AI may call it.
If AI permission is undefined, the API design is incomplete.

## Frontend Rules

- Priority: WeChat Mini Program -> Enterprise WeChat Mini Program -> Web admin.
- Support low-performance devices and weak networks.
- Control first-screen load.
- Avoid flashy frontend implementation.

## Development Output Rules

Before outputting code, state the source document, such as README, PRD, Design, or Dev section.
If no source exists, clearly mark the implementation as inferred.

## Self Review

After completing a task, review business correctness, permissions, AI boundaries, security, and performance.
End with a risk list.
