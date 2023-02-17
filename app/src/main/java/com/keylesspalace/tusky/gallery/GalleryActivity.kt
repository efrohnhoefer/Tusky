package com.keylesspalace.tusky.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.keylesspalace.tusky.BaseActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.ViewMediaActivity
import com.keylesspalace.tusky.components.viewthread.ViewThreadActivity
import com.keylesspalace.tusky.databinding.ActivityViewMediaBinding
import com.keylesspalace.tusky.di.ViewModelFactory
import com.keylesspalace.tusky.util.viewBinding
import com.keylesspalace.tusky.viewdata.AttachmentViewData
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs


class GalleryActivity : BaseActivity(), HasAndroidInjector {
    private val binding by viewBinding(ActivityViewMediaBinding::inflate)
    private var attachments: List<AttachmentViewData> = emptyList()
    private var imageUrl: String? = null

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: GalleryViewModel by viewModels { viewModelFactory }
    private var toolbarVisibility = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            ) {
                toolbarVisibility = true
                binding.toolbar.animate().setStartDelay(0).alpha(1f).start()
            } else {
                toolbarVisibility = false
                binding.toolbar.animate().setStartDelay(0).alpha(0f).start()
            }
            ViewCompat.onApplyWindowInsets(view, windowInsets)
        }

        supportPostponeEnterTransition()

        setContentView(binding.root)

        attachments = intent.getParcelableArrayListExtra(EXTRA_ATTACHMENTS) ?: emptyList()
        val initialPosition = intent.getIntExtra(EXTRA_ATTACHMENT_INDEX, 0)

        // Configure the view pager
        binding.viewPager.apply {
            adapter = GalleryViewPagerAdapter(
                supportFragmentManager,
                lifecycle,
                attachments.map(AttachmentViewData::attachment)
            )
            setCurrentItem(initialPosition, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.toolbar.title = getPageTitle(position)
                }
            })
        }

        // Configure the toolbar
        binding.toolbar.apply {
            setNavigationOnClickListener { supportFinishAfterTransition() }
            title = getPageTitle(initialPosition)
            setSupportActionBar(this)
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    //R.id.action_download -> requestDownloadMedia()
                    R.id.action_open_status -> onOpenStatus()
                    //R.id.action_share_media -> shareMedia()
                    R.id.action_copy_media_link -> copyLink()
                }
                true
            }
        }

        // Configure the action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        lifecycleScope.launch {
            var diffY = 0f
            val insetsController = WindowInsetsControllerCompat(window, binding.root)
            viewModel.events.collect { event ->
                when (event) {
                    GalleryViewModel.Event.ToggleChrome -> {
                        if (toolbarVisibility) {
                            insetsController.hide(
                                WindowInsetsCompat.Type.statusBars() or
                                    WindowInsetsCompat.Type.navigationBars()
                            )
                        } else {
                            insetsController.show(
                                WindowInsetsCompat.Type.statusBars() or
                                    WindowInsetsCompat.Type.navigationBars()
                            )
                        }
                    }
                    GalleryViewModel.Event.Dismiss -> {
                        supportFinishAfterTransition()
                    }
                    GalleryViewModel.Event.Reset -> {
                        diffY = 0f
                        binding.toolbar.animate().translationY(0f).start()
                    }
                    is GalleryViewModel.Event.Drag -> {
                        diffY += event.dy
                        binding.toolbar.translationY = -abs(diffY)
                    }
                }
            }
        }
    }

    private fun getPageTitle(position: Int): CharSequence {
        return String.format(Locale.getDefault(), "%d/%d", position + 1, attachments.size)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.view_media_toolbar, menu)
        // We don't support 'open status' from single image views
        menu.findItem(R.id.action_open_status)?.isVisible = attachments.isNotEmpty()
        return true
    }

    private fun onOpenStatus() {
        val attach = attachments[binding.viewPager.currentItem]
        startActivityWithSlideInAnimation(ViewThreadActivity.startIntent(this, attach.statusId, attach.statusUrl))
    }

    private fun copyLink() {
        val url = imageUrl ?: attachments[binding.viewPager.currentItem].statusUrl
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(null, url))
    }

    override fun androidInjector() = androidInjector

    companion object {
        private const val EXTRA_ATTACHMENTS = "attachments"
        private const val EXTRA_ATTACHMENT_INDEX = "index"
        private const val EXTRA_SINGLE_IMAGE_URL = "single_image"

        @JvmStatic
        fun newIntent(
            context: Context,
            attachments: List<AttachmentViewData>,
            index: Int
        ): Intent {
            val intent = Intent(context, GalleryActivity::class.java)
            intent.putParcelableArrayListExtra(EXTRA_ATTACHMENTS, ArrayList(attachments))
            intent.putExtra(EXTRA_ATTACHMENT_INDEX, index)
            return intent
        }

        @JvmStatic
        fun newSingleImageIntent(
            context: Context,
            url: String
        ): Intent {
            val intent = Intent(context, ViewMediaActivity::class.java)
            intent.putExtra(EXTRA_SINGLE_IMAGE_URL, url)
            return intent
        }
    }
}
