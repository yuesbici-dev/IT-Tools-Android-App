package com.example.network

import android.util.Xml
import com.example.data.model.RssArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class RssParser {

    suspend fun fetchFeed(feedUrl: String, feedTitle: String = "Feed"): Result<List<RssArticle>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(feedUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NetPulse-Android/1.0 (IT Network Suite)")
            }

            conn.inputStream.use { stream ->
                parseFeed(stream, feedTitle)
            }
        }
    }

    private fun parseFeed(inputStream: InputStream, defaultSource: String): List<RssArticle> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val articles = mutableListOf<RssArticle>()
        var eventType = parser.eventType

        var currentTitle = ""
        var currentLink = ""
        var currentPubDate = ""
        var currentDescription = ""
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase() ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "item" || tagName == "entry") {
                        insideItem = true
                        currentTitle = ""
                        currentLink = ""
                        currentPubDate = ""
                        currentDescription = ""
                    } else if (insideItem) {
                        when (tagName) {
                            "title" -> currentTitle = parser.nextText()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                currentLink = if (!href.isNullOrBlank()) {
                                    href
                                } else {
                                    parser.nextText()
                                }
                            }
                            "pubdate", "published", "updated" -> currentPubDate = parser.nextText()
                            "description", "summary", "content" -> currentDescription = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "item" || tagName == "entry") {
                        insideItem = false
                        if (currentTitle.isNotBlank()) {
                            articles.add(
                                RssArticle(
                                    title = currentTitle.trim(),
                                    link = currentLink.trim(),
                                    pubDate = currentPubDate.trim().substringBefore(" +").substringBefore(" GMT"),
                                    description = cleanHtml(currentDescription.trim()),
                                    sourceTitle = defaultSource
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return articles
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
            .take(300)
    }
}
