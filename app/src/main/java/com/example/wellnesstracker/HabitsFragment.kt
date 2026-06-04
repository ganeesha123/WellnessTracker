package com.example.wellnesstracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HabitsFragment : Fragment() {
    private val prefs by lazy { Prefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        android.util.Log.d("HabitsFragment", "onCreateView called")
        try {
            val root = inflater.inflate(R.layout.fragment_habits, container, false)
            android.util.Log.d("HabitsFragment", "Layout inflated successfully")
            
            setupHabitsView(root)
            android.util.Log.d("HabitsFragment", "HabitsFragment setup completed successfully")
            return root
        } catch (e: Exception) {
            android.util.Log.e("HabitsFragment", "Error in onCreateView: ${e.message}", e)
            throw e
        }
    }

    private fun setupHabitsView(root: View) {
        android.util.Log.d("HabitsFragment", "Setting up habits view components")
        val progress = root.findViewById<ProgressBar>(R.id.progressDay)
        val label = root.findViewById<TextView>(R.id.progressLabel)
        val habitList = root.findViewById<LinearLayout>(R.id.habitList)
        val emptyState = root.findViewById<LinearLayout>(R.id.emptyState)
        val btnAddHabit = root.findViewById<Button>(R.id.btnAddHabit)
        
        // Check if all views are found
        if (progress == null) android.util.Log.e("HabitsFragment", "progressDay not found!")
        if (label == null) android.util.Log.e("HabitsFragment", "progressLabel not found!")
        if (habitList == null) android.util.Log.e("HabitsFragment", "habitList not found!")
        if (emptyState == null) android.util.Log.e("HabitsFragment", "emptyState not found!")
        if (btnAddHabit == null) android.util.Log.e("HabitsFragment", "btnAddHabit not found!")
        
        android.util.Log.d("HabitsFragment", "All views found successfully")

        fun refreshHabits() {
            try {
                android.util.Log.d("HabitsFragment", "refreshHabits() called")
                habitList.removeAllViews()
                val habits = prefs.getHabits()
                android.util.Log.d("HabitsFragment", "Retrieved ${habits.size} habits from prefs")
            
            if (habits.isEmpty()) {
                android.util.Log.d("HabitsFragment", "No habits found, showing empty state")
                emptyState.visibility = View.VISIBLE
                habitList.visibility = View.GONE
            } else {
                android.util.Log.d("HabitsFragment", "Found ${habits.size} habits, showing habit list")
                emptyState.visibility = View.GONE
                habitList.visibility = View.VISIBLE
                
                var completedToday = 0
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                android.util.Log.d("HabitsFragment", "Today's date: $today")
                
                for ((index, habit) in habits.withIndex()) {
                    android.util.Log.d("HabitsFragment", "Processing habit ${index + 1}: '${habit.name}' with ${habit.completedDates.size} completed dates")
                    val isCompletedToday = habit.completedDates.contains(today)
                    if (isCompletedToday) completedToday++
                    android.util.Log.d("HabitsFragment", "Habit '${habit.name}' completed today: $isCompletedToday")
                    
                    val habitView = createHabitView(habit, isCompletedToday) { 
                        refreshHabits()
                    }
                    habitList.addView(habitView)
                    android.util.Log.d("HabitsFragment", "Added habit view to habitList")
                }
                
                // Update progress
                val percentage = (completedToday * 100) / habits.size
                progress.progress = percentage
                label.text = "$percentage% completed today"
                android.util.Log.d("HabitsFragment", "Updated progress: $percentage%")
            }
            } catch (e: Exception) {
                android.util.Log.e("HabitsFragment", "Error in refreshHabits: ${e.message}", e)
            }
        }

        btnAddHabit.setOnClickListener {
            showAddHabitDialog { refreshHabits() }
        }

        refreshHabits()
    }

    private fun createHabitView(habit: Habit, isCompleted: Boolean, onUpdate: () -> Unit): View {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            elevation = 2f
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 32, 48, 32)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val checkbox = CheckBox(requireContext()).apply {
            isChecked = isCompleted
            buttonTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4CAF50")
            )
        }

        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(32, 0, 0, 0)
        }

        val nameText = TextView(requireContext()).apply {
            text = habit.name
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#2C3E50"))
        }

        val streakText = TextView(requireContext()).apply {
            text = "${habit.completedDates.size} times completed"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#7F8C8D"))
        }

        textLayout.addView(nameText)
        
        // Add description if it exists
        if (habit.description.isNotEmpty()) {
            val descriptionText = TextView(requireContext()).apply {
                text = habit.description
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#95A5A6"))
                setPadding(0, 8, 0, 0)
            }
            textLayout.addView(descriptionText)
        }
        
        textLayout.addView(streakText)

        val moreButton = ImageView(requireContext()).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            setColorFilter(android.graphics.Color.parseColor("#7F8C8D"))
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                showHabitOptionsMenu(habit, onUpdate)
            }
        }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            toggleHabitCompletion(habit, isChecked)
            onUpdate()
        }

        layout.addView(checkbox)
        layout.addView(textLayout)
        layout.addView(moreButton)
        cardView.addView(layout)

        return cardView
    }

    private fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val habits = prefs.getHabits()
        val habitIndex = habits.indexOfFirst { it.name == habit.name }
        
        if (habitIndex != -1) {
            if (isCompleted) {
                if (!habits[habitIndex].completedDates.contains(today)) {
                    habits[habitIndex].completedDates.add(today)
                }
            } else {
                habits[habitIndex].completedDates.remove(today)
            }
            prefs.saveHabits(habits)
        }
    }

    private fun showAddHabitDialog(onAdded: () -> Unit) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val nameEdit = EditText(requireContext()).apply {
            hint = "Enter habit name"
            setPadding(16, 16, 16, 16)
        }

        val descEdit = EditText(requireContext()).apply {
            hint = "Enter habit description (optional)"
            setPadding(16, 16, 16, 16)
            maxLines = 3
        }

        layout.addView(nameEdit)
        layout.addView(descEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Habit")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val habitName = nameEdit.text.toString().trim()
                val habitDesc = descEdit.text.toString().trim()
                android.util.Log.d("HabitsFragment", "Adding habit: name='$habitName', desc='$habitDesc'")
                if (habitName.isNotEmpty()) {
                    try {
                        val habits = prefs.getHabits()
                        android.util.Log.d("HabitsFragment", "Current habits count: ${habits.size}")
                        val newHabit = Habit(habitName, habitDesc)
                        habits.add(newHabit)
                        android.util.Log.d("HabitsFragment", "Added new habit, total count: ${habits.size}")
                        prefs.saveHabits(habits)
                        android.util.Log.d("HabitsFragment", "Saved habits to prefs")
                        onAdded()
                        android.util.Log.d("HabitsFragment", "Called onAdded() callback")
                    } catch (e: Exception) {
                        android.util.Log.e("HabitsFragment", "Error adding habit: ${e.message}", e)
                    }
                } else {
                    android.util.Log.w("HabitsFragment", "Habit name is empty, not adding")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditHabitDialog(habit: Habit, onUpdated: () -> Unit) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val nameEdit = EditText(requireContext()).apply {
            hint = "Enter habit name"
            setText(habit.name)
            setPadding(16, 16, 16, 16)
            selectAll() // Select all text for easy editing
        }

        val descEdit = EditText(requireContext()).apply {
            hint = "Enter habit description (optional)"
            setText(habit.description)
            setPadding(16, 16, 16, 16)
            maxLines = 3
        }

        layout.addView(nameEdit)
        layout.addView(descEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newHabitName = nameEdit.text.toString().trim()
                val newHabitDesc = descEdit.text.toString().trim()
                if (newHabitName.isNotEmpty()) {
                    editHabit(habit, newHabitName, newHabitDesc)
                    onUpdated()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHabitOptionsMenu(habit: Habit, onUpdate: () -> Unit) {
        val options = arrayOf("Edit", "Delete")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Habit Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditHabitDialog(habit, onUpdate) // Edit
                    1 -> showDeleteConfirmation(habit.name) { // Delete
                        deleteHabit(habit)
                        onUpdate()
                    }
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(habitName: String, onDelete: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '$habitName'?")
            .setPositiveButton("Delete") { _, _ -> onDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit(habit: Habit) {
        val habits = prefs.getHabits()
        habits.removeIf { it.name == habit.name }
        prefs.saveHabits(habits)
    }

    private fun editHabit(habit: Habit, newName: String, newDescription: String = "") {
        val habits = prefs.getHabits()
        val habitIndex = habits.indexOfFirst { it.name == habit.name }
        
        if (habitIndex != -1) {
            habits[habitIndex].name = newName
            habits[habitIndex].description = newDescription
            prefs.saveHabits(habits)
        }
    }
}
