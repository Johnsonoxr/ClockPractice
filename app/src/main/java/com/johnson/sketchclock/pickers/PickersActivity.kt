package com.johnson.sketchclock.pickers

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.ActivityPickersBinding
import com.johnson.sketchclock.pickers.font_picker.FontPickerFragment
import com.johnson.sketchclock.pickers.sticker_picker.StickerPickerFragment
import com.johnson.sketchclock.pickers.template_picker.TemplatePickerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PickersActivity : AppCompatActivity(), ControllableFabHolder {

    private lateinit var vb: ActivityPickersBinding

    private val pageInfoList = listOf(
        PageInfo("Templates", TemplatePickerFragment::class.java),
        PageInfo("Fonts", FontPickerFragment::class.java),
        PageInfo("Stickers", StickerPickerFragment::class.java)
    )

    private data class PageInfo(val title: String, val fragmentClass: Class<out Fragment>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityPickersBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setSupportActionBar(vb.bottomAppBar)

        val adapter = SectionsPagerAdapter(supportFragmentManager)
        vb.viewPager.adapter = adapter
        vb.viewPager.setPageTransformer(true, ZoomOutPageTransformer())
        vb.tabs.setupWithViewPager(vb.viewPager)

        vb.fabAdd.setOnClickListener {
            (adapter.currentFragment as? OnFabClickListener)?.onFabClick()
        }
    }

    override fun changeFabControlMode(controlMode: ControlMode) {
        val prevControlMode = vb.fabAdd.tag as? ControlMode ?: ControlMode.NORMAL
        vb.fabAdd.tag = controlMode
        val resId = when (controlMode) {
            ControlMode.NORMAL -> R.drawable.fab_add
            ControlMode.DELETE -> R.drawable.bottom_delete
            ControlMode.BOOKMARK -> R.drawable.bottom_bookmark
        }
        if (prevControlMode != controlMode) {
            vb.fabAdd.scaleOut(100) {
                vb.fabAdd.setImageResource(resId)
                vb.fabAdd.scaleIn(100)
            }
        }
    }

    private inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        var currentFragment: Fragment? = null
            private set

        override fun getItem(position: Int): Fragment {
            return pageInfoList[position].fragmentClass.getDeclaredConstructor().newInstance().apply { tag }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return pageInfoList[position].title
        }

        override fun getCount(): Int {
            return pageInfoList.size
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
            currentFragment = `object` as? Fragment
        }
    }
}