package com.zionhuang.music.viewmodels

import android.app.Application
import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import androidx.paging.TerminalSeparatorType.FULLY_COMPLETE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zionhuang.music.R
import com.zionhuang.music.constants.Constants.HEADER_PLACEHOLDER_SONG
import com.zionhuang.music.constants.MediaConstants.EXTRA_MEDIA_METADATA_ITEMS
import com.zionhuang.music.constants.MediaConstants.EXTRA_SONG
import com.zionhuang.music.constants.MediaSessionConstants.COMMAND_ADD_TO_QUEUE
import com.zionhuang.music.constants.MediaSessionConstants.COMMAND_PLAY_NEXT
import com.zionhuang.music.db.entities.ArtistEntity
import com.zionhuang.music.db.entities.PlaylistEntity
import com.zionhuang.music.db.entities.Song
import com.zionhuang.music.extensions.show
import com.zionhuang.music.extensions.toMediaItem
import com.zionhuang.music.models.PreferenceSortInfo
import com.zionhuang.music.models.base.IMutableSortInfo
import com.zionhuang.music.playback.MediaSessionConnection
import com.zionhuang.music.repos.SongRepository
import com.zionhuang.music.repos.base.LocalRepository
import com.zionhuang.music.ui.fragments.dialogs.EditSongDialog
import com.zionhuang.music.ui.listeners.SongPopupMenuListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SongsViewModel(application: Application) : AndroidViewModel(application) {
    val songRepository: LocalRepository = SongRepository
    val mediaSessionConnection = MediaSessionConnection

    val sortInfo: IMutableSortInfo = PreferenceSortInfo

    var query: String? = null

    val allSongsFlow: Flow<PagingData<Song>> by lazy {
        Pager(PagingConfig(pageSize = 50, enablePlaceholders = true)) {
            if (!query.isNullOrBlank()) {
                songRepository.searchSongs(query!!).pagingSource
            } else {
                songRepository.getAllSongs(sortInfo).pagingSource
            }
        }.flow.map { pagingData ->
            if (query.isNullOrBlank()) pagingData.insertHeaderItem(FULLY_COMPLETE, HEADER_PLACEHOLDER_SONG)
            else pagingData
        }.cachedIn(viewModelScope)
    }

    val allArtistsFlow: Flow<PagingData<ArtistEntity>> by lazy {
        Pager(PagingConfig(pageSize = 50)) {
            songRepository.getAllArtists().pagingSource
        }.flow.cachedIn(viewModelScope)
    }

    val allPlaylistsFlow: Flow<PagingData<PlaylistEntity>> by lazy {
        Pager(PagingConfig(pageSize = 50)) {
            songRepository.getAllPlaylists().pagingSource
        }.flow.cachedIn(viewModelScope)
    }

    fun getArtistSongsAsFlow(artistId: String) = Pager(PagingConfig(pageSize = 50)) {
        songRepository.getArtistSongs(artistId, sortInfo).pagingSource
    }.flow.map { pagingData ->
        pagingData.insertHeaderItem(FULLY_COMPLETE, HEADER_PLACEHOLDER_SONG)
    }.cachedIn(viewModelScope)

    fun getPlaylistSongsAsFlow(playlistId: String) = Pager(PagingConfig(pageSize = 50)) {
        songRepository.getPlaylistSongs(playlistId, sortInfo).pagingSource
    }.flow.cachedIn(viewModelScope)

    val songPopupMenuListener = object : SongPopupMenuListener {
        override fun editSong(song: Song, context: Context) {
            EditSongDialog().apply {
                arguments = bundleOf(EXTRA_SONG to song)
            }.show(context)
        }

        override fun playNext(songs: List<Song>, context: Context) {
            mediaSessionConnection.mediaController?.sendCommand(
                COMMAND_PLAY_NEXT,
                bundleOf(EXTRA_MEDIA_METADATA_ITEMS to songs.map { it.toMediaItem() }.toTypedArray()),
                null
            )
        }

        override fun addToQueue(songs: List<Song>, context: Context) {
            mediaSessionConnection.mediaController?.sendCommand(
                COMMAND_ADD_TO_QUEUE,
                bundleOf(EXTRA_MEDIA_METADATA_ITEMS to songs.map { it.toMediaItem() }.toTypedArray()),
                null
            )
        }

        override fun addToPlaylist(songs: List<Song>, context: Context) {
            viewModelScope.launch {
                val playlists = songRepository.getAllPlaylists().getList()
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.dialog_title_choose_playlist)
                    .setItems(playlists.map { it.name }.toTypedArray()) { _, i ->
                        viewModelScope.launch {
                            songRepository.addSongsToPlaylist(playlists[i].id, songs)
                        }
                    }
                    .show()
            }
        }

        override fun downloadSongs(songs: List<Song>, context: Context) {
            viewModelScope.launch {
                songRepository.downloadSongs(songs)
            }
        }

        override fun removeDownloads(songs: List<Song>, context: Context) {
            viewModelScope.launch {
                songRepository.removeDownloads(songs)
            }
        }

        override fun deleteSongs(songs: List<Song>) {
            viewModelScope.launch {
                songRepository.deleteSongs(songs)
            }
        }
    }
}
