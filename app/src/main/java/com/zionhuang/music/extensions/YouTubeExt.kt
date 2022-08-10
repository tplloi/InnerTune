package com.zionhuang.music.extensions

import androidx.paging.PagingSource.LoadResult
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.BrowseResult
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.music.db.entities.AlbumEntity
import com.zionhuang.music.db.entities.PlaylistEntity
import com.zionhuang.music.db.entities.SongEntity
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.Page

// the SongItem should be produced by get_queue endpoint to have detailed information
fun SongItem.toSongEntity() = SongEntity(
    id = id,
    title = title,
    duration = duration!!,
    thumbnailUrl = thumbnails.last().url
)

fun AlbumItem.toAlbumEntity() = AlbumEntity(
    id = id,
    title = title,
    year = year,
    thumbnailUrl = thumbnails.last().url
)

fun PlaylistItem.toPlaylistEntity() = PlaylistEntity(
    id = id,
    name = title,
    thumbnailUrl = thumbnails.last().url
)

fun <T : InfoItem> ListInfo<T>.toPage() = LoadResult.Page<Page, InfoItem>(
    data = relatedItems,
    nextKey = nextPage,
    prevKey = null
)

fun <T : InfoItem> InfoItemsPage<T>.toPage() = LoadResult.Page<Page, InfoItem>(
    data = items,
    nextKey = nextPage,
    prevKey = null
)

fun BrowseResult.toPage() = LoadResult.Page(
    data = items,
    nextKey = continuations,
    prevKey = null
)