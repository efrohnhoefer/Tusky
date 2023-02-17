package com.keylesspalace.tusky.gallery

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.keylesspalace.tusky.databinding.FragmentImageBinding
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.di.ViewModelFactory
import com.keylesspalace.tusky.entity.Attachment
import javax.inject.Inject
import kotlin.math.abs

private const val ARG_ATTACHMENT = "attachment"

/**
 * A simple [Fragment] subclass.
 * Use the [ImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageFragment : Fragment(), Injectable {
    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private var attachment: Attachment? = null
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: GalleryViewModel by activityViewModels { viewModelFactory }

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
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                Glide.with(this@ImageFragment).clear(binding.imageView)
                _binding = null
            }
        })

        // Configure image view
        binding.imageView.apply{
            transitionName = attachment?.url
        }

        val attacher = PhotoViewAttacher(binding.imageView).apply {
            // This prevents conflicts with ViewPager
            setAllowParentInterceptOnEdge(true)

            // Clicking outside the photo closes the viewer.
            setOnOutsidePhotoTapListener { viewModel.dismiss() }
            setOnPhotoTapListener { _, _, _ -> viewModel.toggleChrome() }
        }

        binding.imageView.apply {
            var lastY = 0f
            val touchSlop: Int = ViewConfiguration.get(requireContext()).scaledTouchSlop
            setOnTouchListener { v, event ->
                // This part is for scaling/translating on vertical move.
                // We use raw coordinates to get the correct ones during scaling

                if (event.action == MotionEvent.ACTION_DOWN) {
                    lastY = event.rawY
                } else if (event.pointerCount == 1 &&
                    attacher.scale == 1f &&
                    event.action == MotionEvent.ACTION_MOVE
                ) {
                    val diff = event.rawY - lastY
                    // This code is to prevent transformations during page scrolling
                    // If we are already translating or we reached the threshold, then transform.
                    if (translationY != 0f || abs(diff) > touchSlop) {
                        viewModel.drag(diff)
                        translationY += diff
                        lastY = event.rawY
                        return@setOnTouchListener true
                    }
                } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    lastY = 0f
                    if (abs(translationY) > 250) {
                        viewModel.dismiss()
                    } else {
                        viewModel.reset()
                        animate().translationY(0f).start()
                    }
                }
                attacher.onTouch(v, event)
            }
        }

        loadImageFromNetwork(attachment!!.url, attachment!!.previewUrl, binding.imageView)
    }

    private fun loadImageFromNetwork(url: String, previewUrl: String?, photoView: ImageView) {
        val glide = Glide.with(this)
        // Request image from the any cache
        glide
            .load(url)
            .dontAnimate()
            .onlyRetrieveFromCache(true)
            .let {
                if (previewUrl != null)
                    it.thumbnail(
                        glide
                            .load(previewUrl)
                            .dontAnimate()
                            .onlyRetrieveFromCache(true)
                            .centerInside()
                    )
                else it
            }
            // Request image from the network on fail load image from cache
            .error(glide.load(url).centerInside())
            .centerInside()
            .addListener(ImageRequestListener())
            .into(photoView)
    }

    private inner class ImageRequestListener : RequestListener<Drawable> {

        override fun onLoadFailed(
            e: GlideException?,
            model: Any,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {

            activity?.supportStartPostponedEnterTransition()

            return false
        }

        override fun onResourceReady(
            resource: Drawable,
            model: Any,
            target: Target<Drawable>,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {

            activity?.supportStartPostponedEnterTransition()

            return false
        }
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
            ImageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ATTACHMENT, attachment)
                }
            }
    }
}
