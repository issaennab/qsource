# CC-CLI submit notes for Coord

Local repo is ready at `/Users/issaennab/dev/work/workspace/coord`.

## Checklist

- [x] MIT `LICENSE`
- [x] README describing product, audience, stack, run instructions
- [x] `ROADMAP.md` with ≥20h of planned work
- [x] Real multi-commit history (scaffold → API → web)
- [x] No JWT/secrets in repo; `.env.example` only
- [x] Seed uses sample actors only
- [ ] Create public GitHub repo `issaennab/coord` and push `main`

## Publish to GitHub (run locally)

```bash
cd /Users/issaennab/dev/work/workspace/coord
# if gh is installed and authenticated:
gh repo create coord --public --source=. --remote=origin --push \
  --description "Versioned product planning for humans and AI agents"

# or manually:
# 1) Create empty public repo issaennab/coord on github.com (no README/license)
# 2) git remote add origin git@github.com:issaennab/coord.git
# 3) git push -u origin main
```

## Form fields (draft)

- **Project name:** coord
- **Repository:** https://github.com/issaennab/coord
- **License:** MIT
- **Relationship:** I created it and I am the only owner
- **Owned by a company?** No (personal project)
- **Short description:** Coord is a product-planning app where features and tasks are immutable revisions shared by humans and coding agents. Built with Spring Boot and React; agents attribute changes via headers today, with MCP planned next.
