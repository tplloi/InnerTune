package com.zionhuang.music.youtube.newpipe

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@Suppress("BlockingMethodInNonBlockingContext")
object ExtractorHelper {
    private val service = NewPipe.getService(ServiceList.YouTube.serviceId) as YoutubeService

    /**
     * Stream
     */
    fun extractVideoId(url: String): String = service.streamLHFactory.getId(url)

    /**
     * Search
     */
    fun getSearchQueryHandler(query: String, contentFilter: List<String>): SearchQueryHandler =
        service.searchQHFactory.fromQuery(query, contentFilter, "")

    suspend fun search(query: String, contentFilter: List<String>): SearchInfo =
        search(getSearchQueryHandler(query, contentFilter))

    suspend fun search(queryHandler: SearchQueryHandler): SearchInfo = checkCache("${queryHandler.searchString}$${queryHandler.contentFilters[0]}") {
        SearchInfo.getInfo(service, queryHandler)
    }

    suspend fun search(query: String, contentFilter: List<String>, page: Page): InfoItemsPage<InfoItem> =
        search(service.searchQHFactory.fromQuery(query, contentFilter, ""), page)

    suspend fun search(queryHandler: SearchQueryHandler, page: Page): InfoItemsPage<InfoItem> = checkCache("${queryHandler.searchString}$${queryHandler.contentFilters[0]}$${page.hashCode()}") {
        SearchInfo.getMoreItems(service, queryHandler, page)
    }

    /**
     * Playlist
     */
    fun getPlaylistLinkHandler(url: String): ListLinkHandler =
        service.playlistLHFactory.fromUrl(url)

    suspend fun getPlaylist(url: String): PlaylistInfo = checkCache(url) {
        PlaylistInfo.getInfo(service, url)
    }

    suspend fun getPlaylist(url: String, page: Page): InfoItemsPage<StreamInfoItem> =
        checkCache("$url$${page.hashCode()}") {
            PlaylistInfo.getMoreItems(service, url, page)
        }

    /**
     * Channel
     */
    fun getChannelLinkHandler(url: String): ListLinkHandler =
        service.channelLHFactory.fromUrl(url)

    suspend fun getChannel(url: String): ChannelInfo = checkCache(url) {
        ChannelInfo.getInfo(service, url)
    }

    suspend fun getChannel(url: String, page: Page): InfoItemsPage<StreamInfoItem> =
        checkCache("$url$${page.hashCode()}") {
            ChannelInfo.getMoreItems(service, url, page)
        }

    suspend fun getStreamInfo(id: String): StreamInfo = checkCache("stream$$id") {
        StreamInfo.getInfo(service, service.streamLHFactory.getUrl(id))
    }

    private suspend fun <T : Any> checkCache(id: String, loadFromNetwork: suspend () -> T): T =
        loadFromCache(id) ?: withContext(IO) {
            loadFromNetwork().also {
                InfoCache.putInfo(id, it)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> loadFromCache(id: String): T? =
        InfoCache.getFromKey(id) as T?
}