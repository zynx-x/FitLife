package com.example.fitsutra

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class SettingFragment : Fragment() {

    private lateinit var profileUsername: EditText
    private lateinit var profileEmail: TextView
    private lateinit var profileGoals: EditText
    private lateinit var profileHeight: EditText
    private lateinit var profileWeight: EditText
    private lateinit var profileImage: ImageView
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        profileUsername = view.findViewById(R.id.profileUsername)
        profileEmail = view.findViewById(R.id.profileEmail)
        profileGoals = view.findViewById(R.id.profileGoals)
        profileHeight = view.findViewById(R.id.profileHeight)
        profileWeight = view.findViewById(R.id.profileWeight)
        profileImage = view.findViewById(R.id.profileImage)
        saveButton = view.findViewById(R.id.saveButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        loadUserProfile()

        saveButton.setOnClickListener { saveUserProfile() }
        logoutButton.setOnClickListener { showLogoutConfirmationDialog() }

        return view
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username") ?: "N/A"
                        val email = document.getString("email") ?: "N/A"
                        val goals = document.getString("goals") ?: "N/A"
                        val height = document.getString("height") ?: "N/A"
                        val weight = document.getString("weight") ?: "N/A"
                        val profileImageUrl = document.getString("profileImage")

                        profileUsername.setText(username)
                        profileEmail.text = "Email: $email"
                        profileGoals.setText(goals)
                        profileHeight.setText(height)
                        profileWeight.setText(weight)

                        // Load profile image
                        loadProfileImage(profileImageUrl)
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    private fun loadProfileImage(imageUrl: String?) {
        if (imageUrl != null) {
            val file = File.createTempFile("profile", "jpg")

            val profileImageRef = storage.child("profile_images/$imageUrl")

            profileImageRef.getFile(file).addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                profileImage.setImageBitmap(bitmap)
            }.addOnFailureListener {
                it.printStackTrace()
            }
        }
    }

    private fun saveUserProfile() {
        val username = profileUsername.text.toString()
        val goals = profileGoals.text.toString()
        val height = profileHeight.text.toString()
        val weight = profileWeight.text.toString()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val updatedUser = hashMapOf(
                "username" to username,
                "goals" to goals,
                "height" to height,
                "weight" to weight
            )

            firestore.collection("users").document(userId).update(updatedUser as Map<String, Any>)
                .addOnSuccessListener {
                    // Handle success (e.g., show a toast or dialog)
                }
                .addOnFailureListener {
                    // Handle failure (e.g., show an error message)
                    it.printStackTrace()
                }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun logout() {
        auth.signOut()
        val fragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, LoginFragment())
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }
}
