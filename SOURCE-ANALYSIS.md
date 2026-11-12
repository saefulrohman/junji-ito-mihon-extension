# Blogger source analysis

Target catalog:
https://junjiitomangaindonesia.blogspot.com/p/kumpulan-manga-junji-ito-bahasa.html

Observed structure and conventions:
- Catalog post links follow Blogger paths like `/YYYY/MM/post-slug.html`.
- Main catalog content is expected in `.post-body`, with `.entry-content` fallback.
- Manga images are expected inside the same post-body container.
- Same-story continuation text observed: `HALAMAN SELANJUTNYA`.
- Next-story text observed: `BERSAMBUNG KE BAB SELANJUTNYA`; intentionally not followed.

Example posts observed:
- https://junjiitomangaindonesia.blogspot.com/2023/04/no-longer-human-chain-no-1-yozo-oba.html
- https://junjiitomangaindonesia.blogspot.com/2024/08/no-longer-human-chain-no-1-yozo-oba.html

Caveat: this is a structural implementation hypothesis based on observed rendered pages and link labels, not a full HTML regression fixture. Verify selectors and navigation against the live site before treating the source as production-ready.
