from pathlib import Path

SOURCE = Path(__file__).parents[1] / "src/id/junjito/src/eu/kanade/tachiyomi/extension/id/junjito/JunjiItoIndonesia.kt"


def test_source_uses_keiyoushi_api_markers():
    text = SOURCE.read_text(encoding="utf-8")
    assert "@Source" in text
    assert "KeiSource()" in text
    assert "SMangaUpdate" in text


def test_catalog_parser_is_scoped_to_blogger_posts():
    text = SOURCE.read_text(encoding="utf-8")
    assert 'select("a[href]")' in text
    assert 'Regex("/20\\\\d{2}/\\\\d{2}/[^/]+\\\\.html$")' in text


def test_reader_follows_only_same_story_continuations():
    text = SOURCE.read_text(encoding="utf-8")
    assert "HALAMAN SELANJUTNYA" in text
    assert "maxContinuationPages" in text
    assert "visited" in text
    assert "BERSAMBUNG KE BAB SELANJUTNYA" not in text


def test_reader_extracts_images_from_post_body():
    text = SOURCE.read_text(encoding="utf-8")
    assert 'selectFirst(".post-body")' in text
    assert 'select("img")' in text
