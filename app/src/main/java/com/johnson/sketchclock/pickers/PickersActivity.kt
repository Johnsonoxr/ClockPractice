package com.johnson.sketchclock.pickers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.johnson.sketchclock.R
import com.johnson.sketchclock.databinding.ActivityPickersBinding
import com.johnson.sketchclock.font_picker.FontPickerFragment
import com.johnson.sketchclock.illustration_picker.IllustrationPickerFragment
import com.johnson.sketchclock.template_picker.TemplatePickerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PickersActivity : AppCompatActivity() {

    private lateinit var vb: ActivityPickersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityPickersBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.viewPager.adapter = SectionsPagerAdapter(supportFragmentManager)
        vb.tabs.setupWithViewPager(vb.viewPager)
    }

    class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> TemplatePickerFragment()
                1 -> FontPickerFragment()
                else -> IllustrationPickerFragment()
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> "Templates"
                1 -> "Fonts"
                else -> "Illustrations"
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }
}