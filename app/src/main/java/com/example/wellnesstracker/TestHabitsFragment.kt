package com.example.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TestHabitsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        android.util.Log.d("TestHabitsFragment", "onCreateView called")
        
        // Create a simple test view
        val textView = TextView(requireContext()).apply {
            text = "Test Habits Fragment - Navigation Working!"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        
        android.util.Log.d("TestHabitsFragment", "Test fragment created successfully")
        return textView
    }
}