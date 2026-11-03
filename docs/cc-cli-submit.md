# CC-CLI submit notes for QSource

Local repo is ready at `/Users/issaennab/dev/work/workspace/qsource`.

## Checklist

- [x] MIT `LICENSE`
- [x] README describing product, audience, stack, run instructions
- [x] `ROADMAP.md` with ≥20h of planned work
- [x] Real multi-commit history (scaffold → API → web)
- [x] No JWT/secrets in repo; `.env.example` only
- [x] Seed uses sample actors only
- [ ] Create public GitHub repo `issaennab/qsource` and push `main`

## Publish to GitHub (run locally)

```bash
cd /Users/issaennab/dev/work/workspace/qsource
# if gh is installed and authenticated:
gh repo create qsource --public --source=. --remote=origin --push \
  --description "Versioned product planning for humans and AI agents"

# or manually:
# 1) Create empty public repo issaennab/qsource on github.com (no README/license)
# 2) git remote add origin git@github.com:issaennab/qsource.git
# 3) git push -u origin main
```

## Form fields (draft)

- **Project name:** qsource
- **Repository:** https://github.com/issaennab/qsource
- **License:** MIT
- **Relationship:** I created it and I am the only owner
- **Owned by a company?** No (personal project)
- **Short description:** QSource is a product-planning app where features and tasks are immutable revisions shared by humans and coding agents. Built with Spring Boot and React; agents attribute changes via headers today, with MCP planned next.
