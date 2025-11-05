package com.example.firebaseauth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebaseauth.databinding.ActivityLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.launch

/**
 * LoginActivity - Handles OIDC and SAML authentication
 *
 * This activity demonstrates how to implement OIDC and SAML authentication with Firebase.
 * Before using, you must:
 * 1. Configure your provider (Auth0, Azure AD, Okta, etc.) as either OIDC or SAML
 * 2. Add the provider to Firebase Console -> Authentication -> Sign-in method
 *    - For OIDC: Add custom provider with "oidc." prefix
 *    - For SAML: Add SAML provider with "saml." prefix
 * 3. Update the auth.provider.id in app/src/main/res/raw/config_properties
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var authConfig: AuthConfig
    private val authManager = AuthManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Load configuration
        try {
            authConfig = ConfigManager.getAuthConfig(this)
            android.util.Log.d("LoginActivity", "Configuration loaded - OIDC: ${authConfig.oidcProviderId}, SAML: ${authConfig.samlProviderId}")

            // Enable/disable buttons based on configuration
            binding.btnSignIn.isEnabled = authConfig.hasOIDC()
            binding.btnSamlSignIn.isEnabled = authConfig.hasSAML()

            if (!authConfig.hasOIDC() && !authConfig.hasSAML()) {
                showError("No authentication providers configured")
            }
        } catch (e: ConfigurationException) {
            android.util.Log.e("LoginActivity", "Failed to load configuration", e)
            showError("Configuration Error: ${e.message}")
            binding.btnSignIn.isEnabled = false
            binding.btnSamlSignIn.isEnabled = false
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // Setup OIDC button
        binding.btnSignIn.setOnClickListener {
            // If development mode is enabled, show dialog to allow changing provider ID
            if (authConfig.developmentMode) {
                showProviderIdDialog("oidc")
            } else {
                // Use configured OIDC provider
                authConfig.oidcProviderId?.let { providerId ->
                    signInWithProvider(providerId)
                } ?: run {
                    showError("OIDC provider not configured")
                }
            }
        }

        // Setup SAML button
        binding.btnSamlSignIn.setOnClickListener {
            // If development mode is enabled, show dialog to allow changing provider ID
            if (authConfig.developmentMode) {
                showProviderIdDialog("saml")
            } else {
                // Use configured SAML provider
                authConfig.samlProviderId?.let { providerId ->
                    signInWithProvider(providerId)
                } ?: run {
                    showError("SAML provider not configured")
                }
            }
        }

        // Setup Google Sign-In button
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * Shows a dialog to enter or confirm the Provider ID
     * This is helpful for testing different providers in development mode
     *
     * @param providerType The type of provider ("oidc" or "saml")
     */
    private fun showProviderIdDialog(providerType: String) {
        val input = android.widget.EditText(this)

        // Pre-fill with appropriate provider ID from config
        val defaultProviderId = if (providerType == "oidc") {
            authConfig.oidcProviderId ?: "oidc.auth0"
        } else {
            authConfig.samlProviderId ?: "saml.auth0"
        }

        input.setText(defaultProviderId)
        input.hint = "Enter your ${providerType.uppercase()} Provider ID"

        val title = if (providerType == "oidc") "OIDC Provider" else "SAML Provider"
        val format = if (providerType == "oidc") "oidc.provider_name" else "saml.provider_name"

        MaterialAlertDialogBuilder(this)
            .setTitle("$title (Development Mode)")
            .setMessage("Enter your provider ID from Firebase Console\nFormat: $format")
            .setView(input)
            .setPositiveButton("Sign In") { _, _ ->
                val providerId = input.text.toString().trim()
                if (providerId.isNotEmpty()) {
                    signInWithProvider(providerId)
                } else {
                    showError("Provider ID cannot be empty")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Initiates Google Sign-In flow using Credential Manager API
     */
    private fun signInWithGoogle() {
        android.util.Log.d("LoginActivity", "Starting Google Sign-In")
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Get the web client ID from resources
                val webClientId = getString(R.string.default_web_client_id)

                // Validate web client ID
                if (webClientId == "YOUR_WEB_CLIENT_ID_HERE") {
                    showError("Please configure your Web Client ID in strings.xml")
                    showLoading(false)
                    return@launch
                }

                android.util.Log.d("LoginActivity", "Web Client ID: $webClientId")

                // Sign in with Google
                val result = authManager.signInWithGoogle(this@LoginActivity, webClientId)

                result.fold(
                    onSuccess = { user ->
                        android.util.Log.d("LoginActivity", "Google Sign-In successful: ${user.uid}")
                        handleAuthSuccess(user)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("LoginActivity", "Google Sign-In failed", exception)
                        handleAuthFailure(exception)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Google Sign-In exception", e)
                handleAuthFailure(e)
            }
        }
    }

    /**
     * Initiates OAuth provider sign-in flow (OIDC or SAML)
     *
     * IMPORTANT: This implementation follows Firebase best practices:
     * 1. Checks for pending auth results first (handles Custom Chrome Tabs lifecycle)
     * 2. Avoids Activity references in listeners to prevent detachment issues
     * 3. Supports both OIDC (oidc.*) and SAML (saml.*) providers
     *
     * @param providerId The provider ID configured in Firebase Console (oidc.* or saml.*)
     */
    private fun signInWithProvider(providerId: String) {
        val providerType = when {
            providerId.startsWith("oidc.") -> "OIDC"
            providerId.startsWith("saml.") -> "SAML"
            else -> "OAuth"
        }

        android.util.Log.d("LoginActivity", "========================================")
        android.util.Log.d("LoginActivity", "Starting $providerType sign-in")
        android.util.Log.d("LoginActivity", "Provider ID: $providerId")
        android.util.Log.d("LoginActivity", "Config - OIDC: ${authConfig.oidcProviderId}, SAML: ${authConfig.samlProviderId}")
        android.util.Log.d("LoginActivity", "========================================")
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
        // Note: OAuthProvider works for both OIDC and SAML based on the provider ID prefix
        val provider = OAuthProvider.newBuilder(providerId)

        // Add custom parameters from config if any (primarily for OIDC)
        if (authConfig.customParameters.isNotEmpty()) {
            authConfig.customParameters.forEach { (key, value) ->
                android.util.Log.d("LoginActivity", "Adding custom parameter: $key")
                provider.addCustomParameter(key, value)
            }
        }

        // Add scopes from config if any (primarily for OIDC)
        if (authConfig.scopes.isNotEmpty()) {
            android.util.Log.d("LoginActivity", "Adding scopes: ${authConfig.scopes}")
            provider.scopes = authConfig.scopes
        }

        // STEP 3: Start the OAuth sign-in flow
        // Note: We avoid referencing 'this' Activity in the listeners to prevent
        // detachment issues when Custom Chrome Tabs temporarily removes the Activity
        android.util.Log.d("LoginActivity", "Calling startActivityForSignInWithProvider...")
        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { authResult ->
                android.util.Log.d("LoginActivity", "Sign-in success! Provider Type: $providerType, User: ${authResult.user?.uid}")
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
     * Legacy method for backward compatibility
     * @deprecated Use signInWithProvider() instead
     */
    @Deprecated("Use signInWithProvider() instead", ReplaceWith("signInWithProvider(providerId)"))
    private fun signInWithOIDC(providerId: String) {
        signInWithProvider(providerId)
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
    private fun handleAuthFailure(exception: Throwable) {
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

        // Extract provider type from exception if available
        val providerType = when {
            exception.message?.contains("oidc.", ignoreCase = true) == true -> "OIDC"
            exception.message?.contains("saml.", ignoreCase = true) == true -> "SAML"
            exception.message?.contains("google", ignoreCase = true) == true -> "Google"
            else -> "Unknown"
        }

        android.util.Log.e("LoginActivity", "[$providerType] Authentication failed")
        android.util.Log.e("LoginActivity", "Full error message: ${exception.message}")

        val errorMessage = when {
            exception.message?.contains("provider", ignoreCase = true) == true ->
                "[$providerType] Provider ID not configured. Please check:\n" +
                "1. Firebase Console -> Authentication -> Sign-in method\n" +
                "2. Verify the provider ID in config_properties matches Firebase\n" +
                "3. Ensure the provider is enabled in Firebase"
            exception.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection."
            exception.message?.contains("CANCELED", ignoreCase = true) == true ||
            exception.message?.contains("cancelled", ignoreCase = true) == true ->
                "Authentication cancelled by user."
            exception.message?.contains("10") == true ->
                "[$providerType] Developer error: Check your Firebase configuration and provider ID"
            else ->
                "[$providerType] Authentication failed: ${exception.message}"
        }

        android.util.Log.e("LoginActivity", "Displaying error to user: $errorMessage")
        showError(errorMessage)

        // Log the error for debugging
        android.util.Log.e("LoginActivity", "Authentication failed - Full stack trace:", exception)
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        // Disable ALL authentication buttons to prevent interference
        binding.btnSignIn.isEnabled = !show && authConfig.hasOIDC()
        binding.btnSamlSignIn.isEnabled = !show && authConfig.hasSAML()
        binding.btnGoogleSignIn.isEnabled = !show
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
