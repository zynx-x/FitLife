package com.example.fitsutra

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class TrackerFragment : Fragment(), SensorEventListener {

    private lateinit var hydrationStatusTextView: TextView
    private lateinit var add250mlButton: Button
    private lateinit var add500mlButton: Button
    private lateinit var add1000mlButton: Button
    private lateinit var stepCounterStatusTextView: TextView
    private lateinit var sleepStatusTextView: TextView
    private lateinit var addSleepButton: Button
    private lateinit var addCaloriesButton: Button
    private lateinit var nutritionStatusTextView: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tracker, container, false)

        hydrationStatusTextView = view.findViewById(R.id.hydrationStatus)
        add250mlButton = view.findViewById(R.id.add250mlButton)
        add500mlButton = view.findViewById(R.id.add500mlButton)
        add1000mlButton = view.findViewById(R.id.add1000mlButton)
        stepCounterStatusTextView = view.findViewById(R.id.stepCounterStatus)
        sleepStatusTextView = view.findViewById(R.id.sleepStatus)
        addSleepButton = view.findViewById(R.id.addSleepButton)
        addCaloriesButton = view.findViewById(R.id.addCaloriesButton)
        nutritionStatusTextView = view.findViewById(R.id.nutritionStatus)

        add250mlButton.setOnClickListener { updateHydrationData(250) }
        add500mlButton.setOnClickListener { updateHydrationData(500) }
        add1000mlButton.setOnClickListener { updateHydrationData(1000) }
        addSleepButton.setOnClickListener { showSleepDialog() }
        addCaloriesButton.setOnClickListener { addCalories() } // Correct method call

        loadHydrationData()
        loadSleepData()
        loadNutritionData()

        // Initialize sensor manager and register sensor listener
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            Log.e("StepCounter", "No step sensor found")
        }

        return view
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            stepCount = event.values[0].toInt()
            stepCounterStatusTextView.text = "Steps Taken: $stepCount"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
    }

    private fun loadHydrationData() {
        val docRef = firestore.collection("hydration").document("dailyIntake")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val totalIntake = document.getLong("totalIntake") ?: 0
                hydrationStatusTextView.text = "Total Water Consumed: $totalIntake ml"
            } else {
                // Document does not exist; initialize with default values
                docRef.set(mapOf("totalIntake" to 0)).addOnSuccessListener {
                    hydrationStatusTextView.text = "Total Water Consumed: 0 ml"
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error creating document", exception)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error loading hydration data", exception)
        }
    }

    private fun updateHydrationData(waterIntake: Int) {
        val docRef = firestore.collection("hydration").document("dailyIntake")

        firestore.runTransaction { transaction ->
            val document = transaction.get(docRef)
            val currentIntake = document.getLong("totalIntake") ?: 0
            val newIntake = currentIntake + waterIntake
            transaction.update(docRef, "totalIntake", newIntake)
            newIntake
        }.addOnSuccessListener { newIntake ->
            hydrationStatusTextView.text = "Total Water Consumed: $newIntake ml"
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error updating hydration data", exception)
        }
    }

    private fun loadSleepData() {
        val docRef = firestore.collection("sleep").document("dailySleep")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val sleepDuration = document.getLong("sleepDuration") ?: 0
                sleepStatusTextView.text = "Sleep Duration: $sleepDuration hours"
            } else {
                // Document does not exist; initialize with default values
                docRef.set(mapOf("sleepDuration" to 0)).addOnSuccessListener {
                    sleepStatusTextView.text = "Sleep Duration: 0 hours"
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error creating document", exception)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error loading sleep data", exception)
        }
    }

    private fun addSleepDuration(startTime: String, endTime: String) {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            val start = dateFormat.parse(startTime)
            val end = dateFormat.parse(endTime)

            if (start != null && end != null) {
                calendar.time = start
                val startMillis = calendar.timeInMillis
                calendar.time = end
                val endMillis = calendar.timeInMillis

                val sleepDurationMillis = endMillis - startMillis
                val sleepDurationHours = sleepDurationMillis / (1000 * 60 * 60) // convert to hours

                val docRef = firestore.collection("sleep").document("dailySleep")

                firestore.runTransaction { transaction ->
                    val document = transaction.get(docRef)
                    val currentSleep = document.getLong("sleepDuration") ?: 0
                    val newSleep = currentSleep + sleepDurationHours
                    transaction.update(docRef, "sleepDuration", newSleep)
                    newSleep
                }.addOnSuccessListener { newSleep ->
                    sleepStatusTextView.text = "Sleep Duration: $newSleep hours"
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error updating sleep data", exception)
                }
            }
        } catch (e: ParseException) {
            Log.e("SleepTracker", "Error parsing time", e)
        }
    }

    private fun showSleepDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sleep_input, null)
        val startTimeEditText: EditText = dialogView.findViewById(R.id.sleepStartTime)
        val endTimeEditText: EditText = dialogView.findViewById(R.id.sleepEndTime)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Sleep Duration")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val startTime = startTimeEditText.text.toString()
                val endTime = endTimeEditText.text.toString()

                if (validateTimeInputs(startTime, endTime)) {
                    addSleepDuration(startTime, endTime)
                } else {
                    Log.e("SleepTracker", "Invalid time inputs")
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun validateTimeInputs(startTime: String, endTime: String): Boolean {
        return !TextUtils.isEmpty(startTime) && !TextUtils.isEmpty(endTime)
    }

    private fun loadNutritionData() {
        val docRef = firestore.collection("nutrition").document("dailyCalories")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val totalCalories = document.getLong("totalCalories") ?: 0
                nutritionStatusTextView.text = "Total Calories Consumed: $totalCalories kcal"
            } else {
                // Document does not exist; initialize with default values
                docRef.set(mapOf("totalCalories" to 0)).addOnSuccessListener {
                    nutritionStatusTextView.text = "Total Calories Consumed: 0 kcal"
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error creating document", exception)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error loading nutrition data", exception)
        }
    }

    private fun addCalories() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calories_input, null)
        val caloriesInput: EditText = dialogView.findViewById(R.id.calorieInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Calories")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val calories = caloriesInput.text.toString()

                if (validateCaloriesInput(calories)) {
                    updateCaloriesData(calories.toInt())
                } else {
                    Log.e("CaloriesTracker", "Invalid calories input")
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun validateCaloriesInput(calories: String): Boolean {
        return !TextUtils.isEmpty(calories) && calories.toIntOrNull() != null
    }

    private fun updateCaloriesData(calories: Int) {
        val docRef = firestore.collection("nutrition").document("dailyCalories")

        firestore.runTransaction { transaction ->
            val document = transaction.get(docRef)
            val currentCalories = document.getLong("totalCalories") ?: 0
            val newCalories = currentCalories + calories
            transaction.update(docRef, "totalCalories", newCalories)
            newCalories
        }.addOnSuccessListener { newCalories ->
            nutritionStatusTextView.text = "Total Calories Consumed: $newCalories kcal"
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error updating calories data", exception)
        }
    }
}
