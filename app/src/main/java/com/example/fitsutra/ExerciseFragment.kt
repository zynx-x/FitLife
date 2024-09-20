import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitsutra.databinding.FragmentExerciseBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.ExecutionException

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var videoUrls: List<String> = emptyList()
    private var currentVideoIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goal = arguments?.getString("goal") ?: "weight gain"
        Log.d("ExerciseFragment", "Goal fetched: $goal")
        fetchVideoUrls(goal)

        binding.btnPrevious.setOnClickListener { playPreviousVideo() }
        binding.btnNext.setOnClickListener { playNextVideo() }

        startCamera()
    }

    private fun fetchVideoUrls(goal: String) {
        Log.d("ExerciseFragment", "Fetching video URLs for goal: $goal")
        val exerciseVideosRef = db.collection("exercise_video").document(goal)
        exerciseVideosRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val videoUrls = mutableListOf<String>()
                val data = document.data?.values?.filterIsInstance<String>() ?: emptyList()

                data.forEach { gsUrl ->
                    val ref = storage.getReferenceFromUrl(gsUrl)
                    ref.downloadUrl.addOnSuccessListener { url ->
                        videoUrls.add(url.toString())
                        if (videoUrls.size == data.size) {
                            this.videoUrls = videoUrls
                            Log.d("ExerciseFragment", "Video URLs fetched: $videoUrls")
                            if (videoUrls.isNotEmpty()) {
                                playVideo(currentVideoIndex)
                            } else {
                                Log.d("ExerciseFragment", "No videos found for the goal.")
                            }
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("ExerciseFragment", "Error fetching download URL: ", exception)
                    }
                }
            } else {
                Log.d("ExerciseFragment", "Document does not exist for goal: $goal")
            }
        }.addOnFailureListener { exception ->
            Log.e("ExerciseFragment", "Error fetching video URLs: ", exception)
        }
    }

    private fun playVideo(index: Int) {
        if (index in videoUrls.indices) {
            val videoUrl = videoUrls[index]
            Log.d("ExerciseFragment", "Playing video: $videoUrl")
            binding.videoView.setVideoURI(Uri.parse(videoUrl))
            binding.videoView.setOnErrorListener { _, what, extra ->
                Log.e("ExerciseFragment", "Error occurred while playing video. What: $what, Extra: $extra")
                true
            }
            binding.videoView.setOnPreparedListener {
                Log.d("ExerciseFragment", "Video prepared and starting.")
                binding.videoView.start()
            }
            binding.videoView.setOnCompletionListener {
                Log.d("ExerciseFragment", "Video playback completed.")
            }
        } else {
            Log.d("ExerciseFragment", "Invalid video index: $index")
        }
    }

    private fun playPreviousVideo() {
        if (currentVideoIndex > 0) {
            currentVideoIndex--
            playVideo(currentVideoIndex)
        } else {
            Log.d("ExerciseFragment", "No previous video to play.")
        }
    }

    private fun playNextVideo() {
        if (currentVideoIndex < videoUrls.size - 1) {
            currentVideoIndex++
            playVideo(currentVideoIndex)
        } else {
            Log.d("ExerciseFragment", "No next video to play.")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: ExecutionException) {
                Log.e("ExerciseFragment", "Error starting camera", e)
            } catch (e: InterruptedException) {
                Log.e("ExerciseFragment", "Error starting camera", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
