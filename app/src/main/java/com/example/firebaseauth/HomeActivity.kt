package com.example.firebaseauth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebaseauth.databinding.ActivityHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * HomeActivity - Protected screen shown after successful authentication
 * Displays user information and provides sign-out functionality
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not authenticated, redirect to login
            navigateToLogin()
            return
        }

        setupUI(currentUser)
        setupBackPressHandler()
    }

    private fun setupUI(user: com.google.firebase.auth.FirebaseUser) {
        // Display user information
        binding.tvUserName.text = user.displayName ?: "No display name"
        binding.tvUserEmail.text = user.email ?: "No email"
        binding.tvUserId.text = user.uid

        // Get additional information from OIDC token
        user.getIdToken(false).addOnSuccessListener { result ->
            val claims = result.claims

            // Log token claims for debugging
            android.util.Log.d("HomeActivity", "Token claims: $claims")

            // You can extract additional claims from the OIDC token here
            // For example: name, picture, custom claims, etc.
        }

        // Setup sign-out button
        binding.btnSignOut.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    /**
     * Shows a confirmation dialog before signing out
     */
    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                signOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Signs out the current user from Firebase and clears credential state
     * This ensures Google Sign-In credentials are also cleared
     */
    private fun signOut() {
        lifecycleScope.launch {
            try {
                // Sign out and clear credential state (for Google Sign-In)
                authManager.signOut(this@HomeActivity)

                Toast.makeText(
                    this@HomeActivity,
                    "Signed out successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate back to login
                navigateToLogin()
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Error during sign out", e)
                Toast.makeText(
                    this@HomeActivity,
                    "Error signing out: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent going back to previous screen
                // Show exit confirmation instead
                MaterialAlertDialogBuilder(this@HomeActivity)
                    .setTitle("Exit App")
                    .setMessage("Do you want to exit the app?")
                    .setPositiveButton("Exit") { _, _ ->
                        finishAffinity()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        })
    }
}
