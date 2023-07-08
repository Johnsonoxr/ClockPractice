package com.johnson.sketchclock.pickers

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.johnson.sketchclock.databinding.ActivityPickersBinding
import com.johnson.sketchclock.illustration_picker.IllustrationPickerFragment
import com.johnson.sketchclock.pickers.font_picker.FontPickerFragment
import com.johnson.sketchclock.template_picker.TemplatePickerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PickersActivity : AppCompatActivity(), ControllableFabHolder {

    private lateinit var vb: ActivityPickersBinding

    private val pageInfoList by lazy {
        listOf(
            PageInfo("Templates", TemplatePickerFragment::class.java),
            PageInfo("Fonts", FontPickerFragment::class.java),
            PageInfo("Illustrations", IllustrationPickerFragment::class.java)
        )
    }

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

    override fun editFab(action: (FloatingActionButton) -> Unit) {
        action(vb.fabAdd)
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