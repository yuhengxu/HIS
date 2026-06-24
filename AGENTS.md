# HIS + Kangyang Codex Instructions

These project instructions are adapted from `.cursor/rules/project-constitution.mdc` and `.cursor/rules/his-ai-architect.mdc`.
They apply to every Codex turn in this repository.

## Non-Negotiable Project Constitution

Source of truth: `README.MD`.

### System Boundary

- The product is split into two independently deployable modules:
  - HIS hospital information system: outpatient, inpatient, orders, billing, and related hospital workflows.
  - Kangyang service system: in-hospital services and home-visit services.
- Both modules share one account system with SSO.
- Shared layers include account/authentication, API gateway, and AI access layer.

### Deployment And Environment

- Support Windows and Linux deployment.
- Support both local LAN deployment and cloud deployment.
- Frontend, backend, and database must support one-command deployment, preferably with Docker.

### AI Native Safety Constraints

- AI/LLM database access is read-only at the database layer.
- AI/LLM may only run `SELECT` queries.
- AI/LLM must never run `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, or `CREATE`.
- AI may only call the authorized subset of interfaces defined in `MCP-API.md`.
- All interfaces must have documentation, permission annotations, and index lists where relevant.

### Engineering Constraints

- This is a document-driven project: requirements document -> design document -> development document -> code.
- Do not skip documents and write code directly when required prerequisite documents are missing.
- Whenever code is produced, state the source document section it is based on, for example `based on PRD.md section 3.2`.
- When a code change affects tables, APIs, or permissions, update the corresponding documents under `design/`.
- All tables, fields, primary keys, indexes, foreign keys, and lifecycle rules must be described and traceable.
- Every turn must generate or reference an execution plan under `plans/` before changing docs, code, or tests.
- Follow the active plan in order: documents first, then code, then tests. If implementation diverges, update the plan or explicitly record the reason.

## Required Workflow

### Plan Driven Execution

Before implementation work in every turn:

1. Read or create the active plan in `plans/`.
2. Confirm the plan covers documents, code, tests, acceptance criteria, and risks.
3. Execute in plan order.
4. If a structural change is discovered, update the plan before continuing.

Do not immediately code after receiving a requirement. Work through this sequence first:

1. Requirement analysis
2. Business analysis
3. Data analysis
4. Permission analysis
5. AI analysis
6. System design
7. Development design
8. Coding implementation

If prerequisite documents are missing, stop before coding and identify the missing documents.

## Domain Rules

- Default roles include patient, doctor, nurse, caregiver, kangyang therapist, service institution, family member, and administrator.
- Every design must account for permission isolation, data isolation, and audit logs.
- Never assume open data access by default.
- Medical records, orders, lab/exam results, nursing/rehabilitation records, and user profiles are sensitive data.
- Designs must consider desensitization, permission control, and audit tracing.

## AI Native Design Rules

For every business module, answer:

1. How humans complete the workflow.
2. How AI assists.
3. What data AI needs.
4. Which APIs AI calls.
5. What AI permission boundaries apply.

If these are not answered, the design is incomplete.

## Data Design Rules

When designing tables, first provide:

- Table purpose
- Field descriptions
- Primary key
- Unique indexes
- Normal indexes
- Foreign key relationships
- Data lifecycle

Do not output `CREATE TABLE` directly. Provide the design first, then DDL.

## API Design Rules

Every API must define:

- Functional description
- Caller
- Permission requirements
- Request parameters
- Response parameters
- Error codes
- Whether AI may call it

If AI permission is undefined, the API design is incomplete.

## Frontend Rules

- Priority order: WeChat Mini Program -> Enterprise WeChat Mini Program -> Web admin.
- Support low-performance devices and weak networks.
- Control first-screen load.
- Avoid flashy frontend techniques that do not serve the product.

## Completion Self Review

After completing a task, review:

1. Business correctness
2. Permission model
3. AI access and boundaries
4. Security
5. Performance

End with a risk list when the task changes behavior, schema, APIs, permissions, or AI access.
