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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            fetchUserData(userId)
        } else {
            Log.w("AuthWarning", "User is not authenticated.")
        }
    }

    private fun fetchUserData(userId: String) {
        val userDocRef = db.collection("users").document(userId)
        userDocRef.get().addOnSuccessListener { userDocument ->
            if (userDocument != null && userDocument.exists()) {
                val goal = userDocument.getString("goals") ?: "default_goal"
                fetchStats()
                fetchExercises(goal)
            } else {
                Log.w("FirestoreWarning", "User document does not exist.")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting user document: ", exception)
        }
    }

    private fun fetchStats() {
        val statsDocRef = db.collection("home_page").document("stats")
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
    }

    private fun fetchExercises(goal: String) {
        val exercisesDocRef = db.collection(goal).document("DAY 1")
        exercisesDocRef.get().addOnSuccessListener { exercisesDocument ->
            if (exercisesDocument != null && exercisesDocument.exists()) {
                val fields = exercisesDocument.data ?: emptyMap()
                displayExercises(fields)
            } else {
                Log.w("FirestoreWarning", "No such document in Exercises collection.")
            }
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error getting exercises document: ", exception)
        }
    }

    private fun displayExercises(fields: Map<String, Any>) {
        // Clear previous content
        binding.WorkoutContainer.removeAllViews()

        for ((key, value) in fields) {
            val fieldContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 16, 0, 16)
            }

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
                val storageRef = storage.getReferenceFromUrl(value)
                storageRef.downloadUrl.addOnSuccessListener { uri ->
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

            binding.WorkoutContainer.addView(fieldContainer)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
