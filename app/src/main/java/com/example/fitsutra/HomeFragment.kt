import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fitsutra.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference to the Firestore documents
        val statsDocRef = db.collection("home_page").document("stats")
        val exercisesDocRef = db.collection("muscle_gain").document("DAY 1")

        // Fetch the stats document
        statsDocRef.get().addOnSuccessListener { statsDocument ->
            if (statsDocument != null && statsDocument.exists()) {
                binding.ExercisesCompleted.text = statsDocument.getLong("Exercises_Completed")?.toString() ?: "0"
                binding.ExercisesToDo.text = statsDocument.getLong("Exercises_Todo")?.toString() ?: "0"
                binding.TimeElapsed.text = statsDocument.getLong("Time_Elapsed")?.toString() ?: "0"
            } else {
                Log.w("FirestoreWarning", "Stats document does not exist.")
                binding.ExercisesCompleted.text = "0"
                binding.ExercisesToDo.text = "0"
                binding.TimeElapsed.text = "0"
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting stats document: ", exception)
            binding.ExercisesCompleted.text = "Error"
            binding.ExercisesToDo.text = "Error"
            binding.TimeElapsed.text = "Error"
        }

        // Fetch the exercises document
        exercisesDocRef.get().addOnSuccessListener { exercisesDocument ->
            if (exercisesDocument != null && exercisesDocument.exists()) {
                val fields = exercisesDocument.data ?: emptyMap()

                // Clear previous content
                binding.WorkoutContainer.removeAllViews()

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
                        val storageRef = storage.getReferenceFromUrl(value)
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Create and add the ImageView
                            val imageView = ImageView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }
                            Picasso.get().load(uri).into(imageView, object : com.squareup.picasso.Callback {
                                override fun onSuccess() {
                                    Log.d("Picasso", "Image loaded successfully: $uri")
                                }

                                override fun onError(e: Exception?) {
                                    Log.e("PicassoError", "Error loading image: $uri", e)
                                }
                            })
                            fieldContainer.addView(imageView)
                        }.addOnFailureListener { exception ->
                            Log.e("FirebaseStorageError", "Error getting download URL: ", exception)
                        }
                    } else {
                        // Handle non-image values (if any) here
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
                    binding.WorkoutContainer.addView(fieldContainer)
                }
            } else {
                Log.w("FirestoreWarning", "No such document in Exercises collection.")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting exercises document: ", exception)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
