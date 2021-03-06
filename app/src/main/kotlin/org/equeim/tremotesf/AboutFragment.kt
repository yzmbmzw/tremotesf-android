/*
 * Copyright (C) 2017-2019 Alexey Rochev <equeim@gmail.com>
 *
 * This file is part of Tremotesf.
 *
 * Tremotesf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tremotesf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.equeim.tremotesf

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.google.android.material.tabs.TabLayoutMediator

import org.equeim.tremotesf.donationsfragment.DonationsFragment

import kotlinx.android.synthetic.main.about_fragment.*
import kotlinx.android.synthetic.main.about_fragment_license_tab_fragment.*
import kotlinx.android.synthetic.main.about_fragment_base_tab_fragment.*


class AboutFragment : NavigationFragment(R.layout.about_fragment) {
    companion object {
        const val DONATE = "donate"
    }

    private var pagerAdapter: PagerAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.title = "%s %s".format(getString(R.string.app_name), BuildConfig.VERSION_NAME)

        pagerAdapter = PagerAdapter(this)
        pager.adapter = pagerAdapter
        TabLayoutMediator(tab_layout, pager) { tab, position ->
            tab.setText(PagerAdapter.getTitle(position))
        }.attach()

        if (requireArguments().getBoolean(DONATE)) {
            pager.currentItem = 1
        }
    }

    private class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        companion object {
            private val tabs = Tab.values()

            @StringRes
            fun getTitle(position: Int): Int {
                return when (tabs[position]) {
                    Tab.Main -> R.string.about
                    Tab.Donate -> R.string.donate_tab
                    Tab.Authors -> R.string.authors
                    Tab.Translators -> R.string.translators
                    Tab.License -> R.string.license
                }
            }
        }

        private enum class Tab {
            Main,
            Donate,
            Authors,
            Translators,
            License
        }

        override fun getItemCount() = tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (tabs[position]) {
                Tab.Main -> MainTabFragment()
                Tab.Donate -> DonationsFragment()
                Tab.Authors -> AuthorsTabFragment()
                Tab.Translators -> TranslatorsTabFragment()
                Tab.License -> LicenseTabFragment()
            }
        }
    }

    class MainTabFragment : Fragment(R.layout.about_fragment_base_tab_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val inputStream = resources.openRawResource(R.raw.about)
            text_view.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(inputStream.reader().readText(), 0)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(inputStream.reader().readText())
            }
            inputStream.close()
        }
    }

    class AuthorsTabFragment : Fragment(R.layout.about_fragment_base_tab_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val inputStream = resources.openRawResource(R.raw.authors)
            text_view.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(inputStream.reader().readText(), 0)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(inputStream.reader().readText())
            }
            inputStream.close()
        }
    }

    class TranslatorsTabFragment : Fragment(R.layout.about_fragment_base_tab_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val inputStream = resources.openRawResource(R.raw.translators)
            text_view.text = inputStream.reader().readText()
            inputStream.close()
        }
    }

    class LicenseTabFragment : Fragment(R.layout.about_fragment_license_tab_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val inputStream = resources.openRawResource(R.raw.license)
            web_view.loadData(inputStream.reader().readText(), "text/html", null)
            inputStream.close()
        }
    }
}
