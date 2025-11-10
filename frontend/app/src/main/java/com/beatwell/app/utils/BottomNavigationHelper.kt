package com.beatwell.app.utils

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import com.beatwell.app.CalendarActivity
import com.beatwell.app.ChatActivity
import com.beatwell.app.MainActivity
import com.beatwell.app.ProfileActivity
import com.beatwell.app.EditProfileActivity
import com.beatwell.app.R

class BottomNavigationHelper(private val activity: Activity) {
    
    private var currentTab: Tab = Tab.HOME
    
    enum class Tab {
        HOME, CALENDAR, CHAT, PROFILE
    }
    
    fun setupBottomNavigation() {
        // Home tab
        activity.findViewById<android.widget.LinearLayout>(R.id.tabHome)?.setOnClickListener {
            navigateToTab(Tab.HOME)
        }
        
        // Calendar tab
        activity.findViewById<android.widget.LinearLayout>(R.id.tabCalendar)?.setOnClickListener {
            navigateToTab(Tab.CALENDAR)
        }
        
        // Chat tab
        activity.findViewById<android.widget.LinearLayout>(R.id.tabChat)?.setOnClickListener {
            navigateToTab(Tab.CHAT)
        }
        
        // Profile tab
        activity.findViewById<android.widget.LinearLayout>(R.id.tabProfile)?.setOnClickListener {
            navigateToTab(Tab.PROFILE)
        }
        
        // Set initial state based on current activity
        setCurrentTabBasedOnActivity()
    }
    
    private fun setCurrentTabBasedOnActivity() {
        when (activity) {
            is MainActivity -> setCurrentTab(Tab.HOME)
            is CalendarActivity -> setCurrentTab(Tab.CALENDAR)
            is ChatActivity -> setCurrentTab(Tab.CHAT)
            is ProfileActivity -> setCurrentTab(Tab.PROFILE)
            is EditProfileActivity -> setCurrentTab(Tab.PROFILE)
            else -> setCurrentTab(Tab.HOME)
        }
    }
    
    private fun navigateToTab(tab: Tab) {
        if (currentTab == tab) return
        
        when (tab) {
            Tab.HOME -> {
                if (activity !is MainActivity) {
                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
            Tab.CALENDAR -> {
                if (activity !is CalendarActivity) {
                    val intent = Intent(activity, CalendarActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
            Tab.CHAT -> {
                if (activity !is ChatActivity) {
                    val intent = Intent(activity, ChatActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
            Tab.PROFILE -> {
                if (activity !is ProfileActivity) {
                    val intent = Intent(activity, ProfileActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            }
        }
    }
    
    fun setCurrentTab(tab: Tab) {
        currentTab = tab
        updateTabAppearance()
    }
    
    private fun updateTabAppearance() {
        // Reset all tabs to unselected state
        resetTabAppearance(R.id.ivHome, R.id.tvHome)
        resetTabAppearance(R.id.ivCalendar, R.id.tvCalendar)
        resetTabAppearance(R.id.ivChat, R.id.tvChat)
        resetTabAppearance(R.id.ivProfile, R.id.tvProfile)
        
        // Highlight current tab
        when (currentTab) {
            Tab.HOME -> highlightTab(R.id.ivHome, R.id.tvHome)
            Tab.CALENDAR -> highlightTab(R.id.ivCalendar, R.id.tvCalendar)
            Tab.CHAT -> highlightTab(R.id.ivChat, R.id.tvChat)
            Tab.PROFILE -> highlightTab(R.id.ivProfile, R.id.tvProfile)
        }
    }
    
    private fun highlightTab(imageViewId: Int, textViewId: Int) {
        activity.findViewById<ImageView>(imageViewId)?.setColorFilter(
            activity.resources.getColor(R.color.primary_color, null)
        )
        activity.findViewById<TextView>(textViewId)?.setTextColor(
            activity.resources.getColor(R.color.primary_color, null)
        )
    }
    
    private fun resetTabAppearance(imageViewId: Int, textViewId: Int) {
        activity.findViewById<ImageView>(imageViewId)?.clearColorFilter()
        activity.findViewById<TextView>(textViewId)?.setTextColor(
            activity.resources.getColor(R.color.text_secondary, null)
        )
    }
}
