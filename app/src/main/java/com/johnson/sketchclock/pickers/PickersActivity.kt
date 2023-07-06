package com.johnson.sketchclock.pickers

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.ActivityPickersBinding
import com.johnson.sketchclock.font_picker.FontPickerFragment
import com.johnson.sketchclock.illustration_picker.IllustrationPickerFragment
import com.johnson.sketchclock.template_picker.TemplatePickerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PickersActivity : AppCompatActivity() {

    private lateinit var vb: ActivityPickersBinding

    private var currentFab: View? = null

    private val pageInfoList by lazy {
        listOf(
            PageInfo("Templates", vb.fabAddTemplate, TemplatePickerFragment::class.java),
            PageInfo("Fonts", vb.fabAddFont, FontPickerFragment::class.java),
            PageInfo("Illustrations", vb.fabAddIllustration, IllustrationPickerFragment::class.java)
        )
    }

    private data class PageInfo(val title: String, val fab: View, val fragmentClass: Class<out Fragment>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityPickersBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setSupportActionBar(vb.toolbar)

        vb.viewPager.adapter = SectionsPagerAdapter(supportFragmentManager)
        vb.viewPager.setPageTransformer(true, ZoomOutPageTransformer())
        vb.viewPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                currentFab?.scaleOut(100) {
                    currentFab = pageInfoList[position].fab
                    currentFab?.scaleIn(100)
                }
            }
        })
        vb.tabs.setupWithViewPager(vb.viewPager)

        currentFab = pageInfoList[vb.viewPager.currentItem].fab
        currentFab?.isVisible = true
    }

    private inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return pageInfoList[position].fragmentClass.getDeclaredConstructor().newInstance()
        }

        override fun getPageTitle(position: Int): CharSequence {
            return pageInfoList[position].title
        }

        override fun getCount(): Int {
            return pageInfoList.size
        }
    }
}