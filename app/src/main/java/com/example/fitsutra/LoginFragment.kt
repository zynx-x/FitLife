package com.example.fitsutra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var goalsEditText: TextInputEditText
    private lateinit var heightEditText: TextInputEditText
    private lateinit var weightEditText: TextInputEditText
    private lateinit var submitButton: Button
    private lateinit var signUpText: TextView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        emailEditText = view.findViewById(R.id.email)
        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        confirmPasswordEditText = view.findViewById(R.id.confirmpassword)
        goalsEditText = view.findViewById(R.id.goals)
        heightEditText = view.findViewById(R.id.height)
        weightEditText = view.findViewById(R.id.weight)
        submitButton = view.findViewById(R.id.submitButton)
        signUpText = view.findViewById(R.id.signUpText)

        submitButton.setOnClickListener { handleSubmit() }
        signUpText.setOnClickListener { navigateToSignUp() }

        return view
    }

    private fun handleSubmit() {
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        val goals = goalsEditText.text.toString()
        val height = heightEditText.text.toString()
        val weight = weightEditText.text.toString()

        if (password != confirmPassword) {
            // Handle password mismatch
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val user = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "goals" to goals,
                        "height" to height,
                        "weight" to weight
                    )

                    if (userId != null) {
                        firestore.collection("users").document(userId).set(user)
                            .addOnSuccessListener {
                                // Navigate to SettingFragment
                                val fragmentManager = requireActivity().supportFragmentManager
                                val fragmentTransaction = fragmentManager.beginTransaction()
                                fragmentTransaction.replace(R.id.fragment_container, SettingFragment())
                                fragmentTransaction.addToBackStack(null)
                                fragmentTransaction.commit()
                            }
                            .addOnFailureListener { e ->
                                // Handle errors related to Firestore
                                e.printStackTrace()
                            }
                    }
                } else {
                    // Handle errors related to Firebase Authentication
                    task.exception?.printStackTrace()
                }
            }
    }


    private fun navigateToSignUp() {
        // Navigate to SignUpFragment or handle sign up navigation
    }
}
