# Repository Rules

## Branch workflow

- Never develop directly on `main` or `master`. These branches are reserved for merges and release-ready integration only.
- Before starting development, switch to `dev` or create a dedicated feature branch from the current `dev` branch.
- Merge completed work into `dev` first. Promote tested changes from `dev` to `main` through a merge; do not create implementation commits directly on `main`.
- After completing repository maintenance or a release merge, leave the working tree clean and switch back to `dev`.
- Write all commit messages in English.
- Deviate from this workflow only when the user explicitly requests a different branch strategy for a specific operation.
