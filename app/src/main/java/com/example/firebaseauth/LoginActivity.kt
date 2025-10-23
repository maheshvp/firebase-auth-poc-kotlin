package com.example.firebaseauth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseauth.databinding.ActivityLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider

/**
 * LoginActivity - Handles OpenID Connect authentication
 *
 * This activity demonstrates how to implement OIDC authentication with Firebase.
 * Before using, you must:
 * 1. Configure your OIDC provider (Azure AD, Okta, Auth0, etc.)
 * 2. Add the provider to Firebase Console -> Authentication -> Sign-in method -> Custom providers
 * 3. Update the PROVIDER_ID below with your provider ID from Firebase
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    // TODO: Replace with your OIDC provider ID from Firebase Console
    // Format: "oidc.{your-provider-name}" (e.g., "oidc.azure_test", "oidc.okta")
    private val PROVIDER_ID = "oidc.auth0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setupUI()
    }

    private fun setupUI() {
        binding.btnSignIn.setOnClickListener {
            showProviderIdDialog()
        }
    }

    /**
     * Shows a dialog to enter or confirm the OIDC Provider ID
     * This is helpful for testing different providers
     */
    private fun showProviderIdDialog() {
        val input = android.widget.EditText(this)
        input.setText(PROVIDER_ID)
        input.hint = "Enter your OIDC Provider ID"

        MaterialAlertDialogBuilder(this)
            .setTitle("OIDC Provider ID")
            .setMessage("Enter your provider ID from Firebase Console\n(Format: oidc.provider_name)")
            .setView(input)
            .setPositiveButton("Sign In") { _, _ ->
                val providerId = input.text.toString().trim()
                if (providerId.isNotEmpty()) {
                    signInWithOIDC(providerId)
                } else {
                    showError("Provider ID cannot be empty")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Initiates OpenID Connect sign-in flow
     *
     * IMPORTANT: This implementation follows Firebase best practices:
     * 1. Checks for pending auth results first (handles Custom Chrome Tabs lifecycle)
     * 2. Avoids Activity references in listeners to prevent detachment issues
     *
     * @param providerId The OIDC provider ID configured in Firebase Console
     */
    private fun signInWithOIDC(providerId: String) {
        android.util.Log.d("LoginActivity", "Starting OIDC sign-in with provider: $providerId")
        showLoading(true)

        // STEP 1: Check for pending authentication result first
        // This is critical when using Custom Chrome Tabs, as the Activity may be
        // temporarily removed from foreground during authentication
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            // There's already a pending sign-in flow
            android.util.Log.d("LoginActivity", "Found pending auth result, processing...")
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    android.util.Log.d("LoginActivity", "Pending auth result success")
                    handleAuthSuccess(authResult.user)
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("LoginActivity", "Pending auth result failed", exception)
                    handleAuthFailure(exception)
                }
            return
        }

        android.util.Log.d("LoginActivity", "No pending auth result, starting new sign-in flow")

        // STEP 2: Build the OAuth provider with your provider ID
        val provider = OAuthProvider.newBuilder(providerId)

        // Optional: Add custom parameters if required by your OIDC provider
        // provider.addCustomParameter("tenant", "your-tenant-id")

        // Optional: Add scopes if required
        // provider.scopes = listOf("openid", "email", "profile")

        // STEP 3: Start the OAuth sign-in flow
        // Note: We avoid referencing 'this' Activity in the listeners to prevent
        // detachment issues when Custom Chrome Tabs temporarily removes the Activity
        android.util.Log.d("LoginActivity", "Calling startActivityForSignInWithProvider...")
        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { authResult ->
                android.util.Log.d("LoginActivity", "Sign-in success! User: ${authResult.user?.uid}")
                android.util.Log.d("LoginActivity", "User email: ${authResult.user?.email}")
                android.util.Log.d("LoginActivity", "User display name: ${authResult.user?.displayName}")
                handleAuthSuccess(authResult.user)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("LoginActivity", "Sign-in failed with exception type: ${exception.javaClass.name}")
                android.util.Log.e("LoginActivity", "Error message: ${exception.message}")
                android.util.Log.e("LoginActivity", "Stack trace:", exception)
                handleAuthFailure(exception)
            }
    }

    /**
     * Handles successful authentication
     * Separated from listener to avoid Activity reference issues
     */
    private fun handleAuthSuccess(user: com.google.firebase.auth.FirebaseUser?) {
        android.util.Log.d("LoginActivity", "handleAuthSuccess called")
        android.util.Log.d("LoginActivity", "Activity state - isDestroyed: $isDestroyed, isFinishing: $isFinishing")

        // Check if Activity is still valid before UI operations
        if (isDestroyed || isFinishing) {
            android.util.Log.w("LoginActivity", "Activity is destroyed or finishing, skipping UI updates")
            return
        }

        showLoading(false)

        android.util.Log.d("LoginActivity", "User successfully authenticated: ${user?.uid}")

        Toast.makeText(
            this,
            "Authentication successful!\nWelcome ${user?.displayName ?: user?.email}",
            Toast.LENGTH_SHORT
        ).show()

        // Navigate to Home
        navigateToHome()
    }

    /**
     * Handles authentication failure
     * Separated from listener to avoid Activity reference issues
     */
    private fun handleAuthFailure(exception: Exception) {
        android.util.Log.e("LoginActivity", "handleAuthFailure called")
        android.util.Log.e("LoginActivity", "Exception type: ${exception.javaClass.name}")
        android.util.Log.e("LoginActivity", "Exception message: ${exception.message}")
        android.util.Log.e("LoginActivity", "Activity state - isDestroyed: $isDestroyed, isFinishing: $isFinishing")

        // Check if Activity is still valid before UI operations
        if (isDestroyed || isFinishing) {
            android.util.Log.w("LoginActivity", "Activity is destroyed or finishing, skipping UI updates")
            return
        }

        showLoading(false)

        val errorMessage = when {
            exception.message?.contains("provider", ignoreCase = true) == true ->
                "Provider not configured. Please check Firebase Console."
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection."
            exception.message?.contains("CANCELED", ignoreCase = true) == true ||
            exception.message?.contains("cancelled", ignoreCase = true) == true ->
                "Authentication cancelled by user."
            else ->
                "Authentication failed: ${exception.message}"
        }

        android.util.Log.e("LoginActivity", "Displaying error to user: $errorMessage")
        showError(errorMessage)

        // Log the error for debugging
        android.util.Log.e("LoginActivity", "OIDC Sign-in failed - Full stack trace:", exception)
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSignIn.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.errorText.apply {
            text = message
            visibility = View.VISIBLE
        }

        // Auto-hide error after 5 seconds
        binding.errorText.postDelayed({
            binding.errorText.visibility = View.GONE
        }, 5000)
    }
}
