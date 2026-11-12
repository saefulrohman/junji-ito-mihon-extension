package eu.kanade.tachiyomi.extension.id.junjito

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document

@Source
abstract class JunjiItoIndonesia : KeiSource() {

    override val supportsLatest = false

    private val catalogUrl = "$baseUrl/p/kumpulan-manga-junji-ito-bahasa.html"
    private val placeholderThumbnail = "$baseUrl/favicon.ico"
    private val pageSize = 30
    private val maxContinuationPages = 40

    override suspend fun getPopularManga(page: Int): MangasPage {
        if (page < 1) return MangasPage(emptyList(), false)

        val entries = fetchCatalog()
        val start = (page - 1) * pageSize
        val batch = entries.drop(start).take(pageSize)

        return MangasPage(
            mangas = batch.map { entry ->
                SManga.create().apply {
                    url = entry.url
                    title = entry.title
                    thumbnail_url = placeholderThumbnail
                    genre = "Junji Ito, Horror"
                }
            },
            hasNextPage = entries.size > start + batch.size,
        )
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = getPopularManga(page)

    override suspend fun getSearchMangaList(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return getPopularManga(page)

        val matches = fetchCatalog().filter { it.title.lowercase().contains(normalizedQuery) }
        val start = (page - 1) * pageSize
        val batch = matches.drop(start).take(pageSize)

        return MangasPage(
            mangas = batch.map { entry ->
                SManga.create().apply {
                    url = entry.url
                    title = entry.title
                    thumbnail_url = placeholderThumbnail
                    genre = "Junji Ito, Horror"
                }
            },
            hasNextPage = matches.size > start + batch.size,
        )
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != baseUrl.toHttpUrl().host || !isBloggerPost(url.encodedPath)) return null

        val doc = client.get(url.toString(), Fetch.DOCUMENT).asJsoup()
        val title = extractPostTitle(doc) ?: return null

        return SManga.create().apply {
            this.url = url.encodedPath
            this.title = title
            thumbnail_url = extractPostImages(doc).firstOrNull() ?: placeholderThumbnail
            genre = "Junji Ito, Horror"
        }
    }

  override fun getMangaUrl(manga: SManga): String = if (manga.url.startsWith("http")) manga.url else "$baseUrl${manga.url}"

    override fun getChapterUrl(chapter: SChapter): String =
        if (chapter.url.startsWith("http")) chapter.url else "$baseUrl${chapter.url}"

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val doc = client.get(getMangaUrl(manga), Fetch.DOCUMENT).asJsoup()
        val title = extractPostTitle(doc) ?: manga.title
        val image = extractPostImages(doc).firstOrNull()

        manga.title = title
        manga.thumbnail_url = image ?: manga.thumbnail_url ?: placeholderThumbnail
        manga.description = "Source: Junji Ito Manga Indonesia Blogger. This entry is presented as a single reading chapter; the reader follows same-story links labelled HALAMAN SELANJUTNYA."
        manga.genre = "Junji Ito, Horror"

        val chapterList = listOf(
            SChapter.create().apply {
                url = manga.url
                name = "Full story"
                chapter_number = 1f
            },
        )

        return SMangaUpdate(manga, chapterList)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        var currentUrl = getChapterUrl(chapter)
        val visited = mutableSetOf<String>()
        val imageUrls = linkedSetOf<String>()

        repeat(maxContinuationPages) {
            if (!visited.add(currentUrl)) return@repeat

            val doc = client.get(currentUrl, Fetch.DOCUMENT).asJsoup()
            extractPostImages(doc).forEach(imageUrls::add)

            val nextUrl = findContinuationUrl(doc) ?: return imageUrls.mapIndexed { index, image ->
                Page(index, imageUrl = image)
            }
            currentUrl = nextUrl
        }

        return imageUrls.mapIndexed { index, image -> Page(index, imageUrl = image) }
    }

    private suspend fun fetchCatalog(): List<CatalogEntry> {
        val doc = client.get(catalogUrl, Fetch.DOCUMENT).asJsoup()
        val content = doc.selectFirst(".post-body") ?: doc.selectFirst(".entry-content") ?: doc
        val entries = linkedMapOf<String, CatalogEntry>()

        content.select("a[href]").forEach { anchor ->
            val href = anchor.absUrl("href")
            val title = anchor.text().trim()
            val path = runCatching { href.toHttpUrl().encodedPath }.getOrNull() ?: return@forEach

            if (title.isBlank() || !isBloggerPost(path)) return@forEach
            if (href.substringBefore('?') == catalogUrl) return@forEach
            entries.putIfAbsent(path, CatalogEntry(path, title))
        }

        return entries.values.toList()
    }

    private fun extractPostTitle(doc: Document): String? =
        doc.selectFirst("h1.post-title, h3.post-title, .post-title, h1.entry-title, h3.entry-title")
            ?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: doc.title().substringBefore(" - ").trim().takeIf(String::isNotBlank)

    private fun extractPostImages(doc: Document): List<String> {
        val content = doc.selectFirst(".post-body") ?: doc.selectFirst(".entry-content") ?: return emptyList()

        return content.select("img").mapNotNull { image ->
            val candidate = sequenceOf(
                image.attr("data-src"),
                image.attr("data-original"),
                image.attr("src"),
            ).firstOrNull { it.isNotBlank() } ?: return@mapNotNull null

            val absolute = image.absUrl("src").ifBlank {
                image.absUrl("data-src").ifBlank { candidate }
            }

            absolute.takeIf {
                it.startsWith("https://") &&
                    (it.contains("blogger.googleusercontent.com") ||
                        it.contains("bp.blogspot.com") ||
                        it.contains("blogspot.com"))
            }
        }.distinct()
    }

    private fun findContinuationUrl(doc: Document): String? {
        val content = doc.selectFirst(".post-body") ?: doc.selectFirst(".entry-content") ?: return null

        return content.select("a[href]").firstNotNullOfOrNull { anchor ->
            if (!anchor.text().uppercase().contains("HALAMAN SELANJUTNYA")) return@firstNotNullOfOrNull null
            anchor.absUrl("href").takeIf { it.startsWith(baseUrl) }
        }
    }

    private fun isBloggerPost(path: String): Boolean =
        Regex("/20\\d{2}/\\d{2}/[^/]+\\.html$").containsMatchIn(path)

    private data class CatalogEntry(val url: String, val title: String)
}
