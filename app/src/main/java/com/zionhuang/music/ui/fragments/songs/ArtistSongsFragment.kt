package com.zionhuang.music.ui.fragments.songs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.zionhuang.music.extensions.addOnClickListener
import com.zionhuang.music.extensions.requireAppCompatActivity
import com.zionhuang.music.extensions.toMediaItem
import com.zionhuang.music.playback.queues.ListQueue
import com.zionhuang.music.ui.adapters.SongsAdapter
import com.zionhuang.music.ui.fragments.base.PagingRecyclerViewFragment
import com.zionhuang.music.viewmodels.PlaybackViewModel
import com.zionhuang.music.viewmodels.SongsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ArtistSongsFragment : PagingRecyclerViewFragment<SongsAdapter>() {
    private val args: ArtistSongsFragmentArgs by navArgs()

    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private val songsViewModel by activityViewModels<SongsViewModel>()
    override val adapter = SongsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.apply {
            popupMenuListener = songsViewModel.songPopupMenuListener
            sortInfo = songsViewModel.sortInfo
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addOnClickListener { pos, _ ->
                if (pos == 0) return@addOnClickListener
                playbackViewModel.playQueue(requireActivity(),
                    ListQueue(
                        title = null,
                        items = this@ArtistSongsFragment.adapter.snapshot().items.drop(1).map { it.toMediaItem() },
                        startIndex = pos - 1
                    )
                )
            }
        }

        lifecycleScope.launch {
            requireAppCompatActivity().title = songsViewModel.songRepository.getArtistById(args.artistId)!!.name
            songsViewModel.getArtistSongsAsFlow(args.artistId).collectLatest {
                adapter.submitData(it)
            }
        }

        songsViewModel.sortInfo.liveData.observe(viewLifecycleOwner) {
            adapter.refresh()
        }
    }
}