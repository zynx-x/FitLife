import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fitsutra.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*

class ReportFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var messageTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var dynamicContentContainer: LinearLayout

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        // Initialize views
        calendarView = view.findViewById(R.id.calendarView)
        messageTextView = view.findViewById(R.id.messageTextView)
        imageView = view.findViewById(R.id.imageView)
        dynamicContentContainer = view.findViewById(R.id.dynamicContentContainer)

        // Set up listener for date changes
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = CalendarDay.from(year, month + 1, dayOfMonth) // month is 0-based
            handleDateSelection(selectedDate)
        }

        // Load data from Firestore
        loadDataFromFirestore()

        return view
    }

    private fun handleDateSelection(selectedDate: CalendarDay) {
        messageTextView.text = "Selected Date: ${selectedDate.day}/${selectedDate.month}/${selectedDate.year}"
    }

    private fun loadDataFromFirestore() {
        val docRef = firestore.collection("reports").document("experimental")

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val imageUrl = document.getString("associatedWithImage")

                // Load image with Picasso
                imageUrl?.let {
                    Picasso.get()
                        .load(it)
                        .into(imageView, object : com.squareup.picasso.Callback {
                            override fun onSuccess() {
                                Log.d("Picasso", "Image loaded successfully: $it")
                            }

                            override fun onError(e: Exception?) {
                                Log.e("PicassoError", "Error loading image: $it", e)
                            }
                        })
                }

                // Populate dynamic content container
                populateDynamicContent(document)
            } else {
                Log.w("FirestoreWarning", "Document does not exist.")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting document: ", exception)
        }
    }

    private fun populateDynamicContent(document: DocumentSnapshot) {
        // Clear previous content
        dynamicContentContainer.removeAllViews()

        val fields = document.data ?: emptyMap()

        for ((key, value) in fields) {
            // Create a container for each field and its value
            val fieldContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 16, 0, 16)
            }

            // Create and add the TextView for field name
            val fieldNameTextView = TextView(requireContext()).apply {
                text = key
                textSize = 16f
                setPadding(0, 16, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            fieldContainer.addView(fieldNameTextView)

            if (value is String && value.startsWith("gs://")) { // Check if the value is a GCS URL
                // Convert gs:// URL to HTTPS URL
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(value)
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Create and add the ImageView
                    val fieldImageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    Picasso.get()
                        .load(uri)
                        .into(fieldImageView, object : com.squareup.picasso.Callback {
                            override fun onSuccess() {
                                Log.d("Picasso", "Image loaded successfully: $uri")
                            }

                            override fun onError(e: Exception?) {
                                Log.e("PicassoError", "Error loading image: $uri", e)
                            }
                        })
                    fieldContainer.addView(fieldImageView)
                }.addOnFailureListener { exception ->
                    Log.e("FirebaseStorageError", "Error getting download URL: ", exception)
                }
            } else {
                // Handle non-image values (if any)
                val valueTextView = TextView(requireContext()).apply {
                    text = value.toString()
                    textSize = 16f
                    setPadding(0, 16, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                fieldContainer.addView(valueTextView)
            }

            // Add the field container to the main container
            dynamicContentContainer.addView(fieldContainer)
        }
    }

    private data class CalendarDay(val year: Int, val month: Int, val day: Int) {
        companion object {
            fun from(year: Int, month: Int, day: Int) = CalendarDay(year, month, day)
        }
    }
}
