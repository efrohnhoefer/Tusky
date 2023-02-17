package com.keylesspalace.tusky.gallery

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.fragment.ViewImageFragment

class GalleryViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val attachments: List<Attachment>
): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = attachments.size

    override fun createFragment(position: Int): Fragment {
        val attachment = attachments[position]
        val fragment = when (attachment.type) {
            Attachment.Type.IMAGE -> ImageFragment.newInstance(attachment)
            Attachment.Type.VIDEO,
            Attachment.Type.GIFV,
            Attachment.Type.AUDIO -> VideoFragment.newInstance(attachment)
            else -> ViewImageFragment()
        }
        return fragment
    }
}
