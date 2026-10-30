# Free My TV: guide for AI assistants

The full guide is [CLAUDE.md](CLAUDE.md): read it first and follow it (it is written for any assistant, not only
Claude Code). Most people ask in plain words, like "Install Free My TV on my TV at 192.168.1.50. I live in Lisbon.",
and don't know what a terminal is: explain each step simply, and ask before anything that changes their TV or computer.

## This fork

- Work on `main`, the only maintained branch, tracking `origin/main` in `tolyashaksta/free-my-tv`.
  Keep the owner's customizations there; do not recreate `personal` or keep a separate upstream-base branch.
- Read `LOCAL_SETUP.md` when present for this computer's build and TV notes; it is private and ignored by Git.
- Import upstream changes only after the owner announces a new published release and asks to update.
  Fetch and merge that release's exact tag into `main`, preserving the customizations. Do not merge
  `upstream/main`, fetch upstream during routine work, or schedule upstream checks or synchronization.
