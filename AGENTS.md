# Repository Rules

## Branch workflow

- Never develop directly on `main` or `master`. These branches are reserved for merges and release-ready integration only.
- Before starting development, switch to `dev` or create a dedicated feature branch from the current `dev` branch.
- Merge completed work into `dev` first. Promote tested changes from `dev` to `main` through a merge; do not create implementation commits directly on `main`.
- After completing repository maintenance or a release merge, leave the working tree clean and switch back to `dev`.
- Write all commit messages in English.
- Deviate from this workflow only when the user explicitly requests a different branch strategy for a specific operation.

<!-- diagram-agents-kit:start -->
## Diagram generation toolkit

Read `diagram-kit/templates/docs/diagrams/AGENTS.md` and the matching specialist in
`diagram-kit/templates/docs/diagrams/agents/` before diagram work.
Styles, configuration and tools stay in `diagram-kit`. Only diagram sources and rendered
artifacts go in `docs/diagrams/<type>/`; create categories on demand.
Run `diagram-kit/scripts/validate-puml.sh` and `diagram-kit/scripts/render-puml.sh` with this
project root as their argument. Do not copy kit configuration into the output tree.
<!-- diagram-agents-kit:end -->
