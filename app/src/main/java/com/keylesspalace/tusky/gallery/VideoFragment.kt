package com.keylesspalace.tusky.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.util.EventLogger
import com.keylesspalace.tusky.databinding.FragmentVideoBinding
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.entity.Attachment

private const val ARG_ATTACHMENT = "attachment"

class VideoFragment : Fragment(), Injectable {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    private var attachment: Attachment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            attachment = it.getParcelable(ARG_ATTACHMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                _binding = null
            }
        })
    }

    override fun onResume() {
        super.onResume()
        initializePlayer()
        binding.playerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.playerView.onPause()
        releasePlayer()
    }

    private fun initializePlayer(): Boolean {
        if (player == null) {
            val intent: Intent = getIntent()
            mediaItems = createMediaItems(intent)
            if (mediaItems.isEmpty()) {
                return false
            }
            val playerBuilder = ExoPlayer.Builder( /* context= */this)
                .setMediaSourceFactory(createMediaSourceFactory())
            setRenderersFactory(
                playerBuilder,
                intent.getBooleanExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, false)
            )
            player = playerBuilder.build()
            player.setTrackSelectionParameters(trackSelectionParameters)
            player.addListener(com.google.android.exoplayer2.demo.PlayerActivity.PlayerEventListener())
            player.addAnalyticsListener(EventLogger())
            player.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            player.setPlayWhenReady(true)
            playerView.setPlayer(player)
            configurePlayerWithServerSideAdsLoader()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player.seekTo(startItemIndex, startPosition)
        }
        player.setMediaItems(mediaItems,  /* resetPosition= */!haveStartPosition)
        player.prepare()

        return true
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        binding.playerView.player = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param attachment Media Attachment
         * @return A new instance of fragment ImageFragment.
         */
        @JvmStatic
        fun newInstance(attachment: Attachment) =
            VideoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ATTACHMENT, attachment)
                }
            }
    }
}
