package com.example.firebaseauth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * AuthManager - Utility class for Firebase Authentication operations
 *
 * This class provides a simplified interface for OIDC authentication
 * and can be extended for other authentication methods.
 */
class AuthManager private constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager().also { instance = it }
            }
        }
    }

    /**
     * Get the currently authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Sign in with OpenID Connect provider
     *
     * @param activity The activity context for the sign-in flow
     * @param providerId The OIDC provider ID from Firebase Console
     * @param customParameters Optional custom parameters for the OIDC provider
     * @param scopes Optional scopes to request
     * @return Result with FirebaseUser or exception
     */
    suspend fun signInWithOIDC(
        activity: Activity,
        providerId: String,
        customParameters: Map<String, String>? = null,
        scopes: List<String>? = null
    ): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "signInWithOIDC called with providerId: $providerId")
            android.util.Log.d("AuthManager", "Custom parameters: $customParameters")
            android.util.Log.d("AuthManager", "Scopes: $scopes")

            // Check for pending auth result first
            val pendingResult = auth.pendingAuthResult
            if (pendingResult != null) {
                android.util.Log.d("AuthManager", "Found pending auth result, awaiting...")
                val authResult = pendingResult.await()
                android.util.Log.d("AuthManager", "Pending auth completed successfully, user: ${authResult.user?.uid}")
                return Result.success(authResult.user!!)
            }

            android.util.Log.d("AuthManager", "No pending result, building OAuth provider")

            // Build the OAuth provider
            val providerBuilder = OAuthProvider.newBuilder(providerId)

            // Add custom parameters if provided
            customParameters?.forEach { (key, value) ->
                android.util.Log.d("AuthManager", "Adding custom parameter: $key = $value")
                providerBuilder.addCustomParameter(key, value)
            }

            // Add scopes if provided (use property assignment, not setScopes)
            scopes?.let {
                android.util.Log.d("AuthManager", "Adding scopes: $it")
                providerBuilder.scopes = it
            }

            // Start sign-in flow
            android.util.Log.d("AuthManager", "Starting sign-in activity...")
            val authResult = auth.startActivityForSignInWithProvider(
                activity,
                providerBuilder.build()
            ).await()

            android.util.Log.d("AuthManager", "Sign-in activity completed")
            val user = authResult.user
            if (user != null) {
                android.util.Log.d("AuthManager", "Sign-in successful! User ID: ${user.uid}, Email: ${user.email}")
                Result.success(user)
            } else {
                android.util.Log.e("AuthManager", "Sign-in failed: User is null")
                Result.failure(Exception("Sign-in failed: User is null"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Sign-in exception occurred", e)
            android.util.Log.e("AuthManager", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("AuthManager", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Reauthenticate the current user with OIDC provider
     *
     * Use this method before performing sensitive operations like:
     * - Deleting the user account
     * - Changing the user's password
     * - Updating security settings
     *
     * @param activity The activity context for the reauthentication flow
     * @param providerId The OIDC provider ID from Firebase Console
     * @param customParameters Optional custom parameters for the OIDC provider
     * @param scopes Optional scopes to request
     * @return Result with FirebaseUser or exception
     */
    suspend fun reauthenticateWithOIDC(
        activity: Activity,
        providerId: String,
        customParameters: Map<String, String>? = null,
        scopes: List<String>? = null
    ): Result<FirebaseUser> {
        return try {
            val currentUser = getCurrentUser()
                ?: return Result.failure(Exception("No user is currently signed in"))

            // Check for pending auth result first
            val pendingResult = auth.pendingAuthResult
            if (pendingResult != null) {
                val authResult = pendingResult.await()
                return Result.success(authResult.user!!)
            }

            // Build the OAuth provider
            val providerBuilder = OAuthProvider.newBuilder(providerId)

            // Add custom parameters if provided
            customParameters?.forEach { (key, value) ->
                providerBuilder.addCustomParameter(key, value)
            }

            // Add scopes if provided
            scopes?.let {
                providerBuilder.scopes = it
            }

            // Start reauthentication flow
            val authResult = currentUser.startActivityForReauthenticateWithProvider(
                activity,
                providerBuilder.build()
            ).await()

            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Reauthentication failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Link an OIDC provider to the current user account
     *
     * This allows a user to sign in with multiple providers.
     * For example, a user who initially signed in with Auth0 can link
     * their Azure AD account to the same Firebase user.
     *
     * @param activity The activity context for the linking flow
     * @param providerId The OIDC provider ID from Firebase Console
     * @param customParameters Optional custom parameters for the OIDC provider
     * @param scopes Optional scopes to request
     * @return Result with FirebaseUser or exception
     */
    suspend fun linkWithOIDC(
        activity: Activity,
        providerId: String,
        customParameters: Map<String, String>? = null,
        scopes: List<String>? = null
    ): Result<FirebaseUser> {
        return try {
            val currentUser = getCurrentUser()
                ?: return Result.failure(Exception("No user is currently signed in"))

            // Check for pending auth result first
            val pendingResult = auth.pendingAuthResult
            if (pendingResult != null) {
                val authResult = pendingResult.await()
                return Result.success(authResult.user!!)
            }

            // Build the OAuth provider
            val providerBuilder = OAuthProvider.newBuilder(providerId)

            // Add custom parameters if provided
            customParameters?.forEach { (key, value) ->
                providerBuilder.addCustomParameter(key, value)
            }

            // Add scopes if provided
            scopes?.let {
                providerBuilder.scopes = it
            }

            // Start account linking flow
            val authResult = currentUser.startActivityForLinkWithProvider(
                activity,
                providerBuilder.build()
            ).await()

            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Account linking failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with OIDC using an existing ID token
     *
     * Use this method if you already have an ID token from your OIDC provider
     * (e.g., from a native SDK or manual flow).
     *
     * @param providerId The OIDC provider ID from Firebase Console
     * @param idToken The ID token from your OIDC provider
     * @param accessToken Optional access token from your OIDC provider
     * @return Result with FirebaseUser or exception
     */
    suspend fun signInWithOIDCToken(
        providerId: String,
        idToken: String,
        accessToken: String? = null
    ): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "signInWithOIDCToken called with providerId: $providerId")
            android.util.Log.d("AuthManager", "Has access token: ${accessToken != null}")

            // Create credential with ID token
            val credential = OAuthProvider.newCredentialBuilder(providerId)
                .setIdToken(idToken)
                .apply {
                    accessToken?.let { setAccessToken(it) }
                }
                .build()

            android.util.Log.d("AuthManager", "Credential created, signing in...")

            // Sign in with credential
            val authResult = auth.signInWithCredential(credential).await()

            android.util.Log.d("AuthManager", "Credential sign-in completed")

            val user = authResult.user
            if (user != null) {
                android.util.Log.d("AuthManager", "Token sign-in successful! User ID: ${user.uid}")
                Result.success(user)
            } else {
                android.util.Log.e("AuthManager", "Token sign-in failed: User is null")
                Result.failure(Exception("Sign-in with token failed: User is null"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Token sign-in exception occurred", e)
            android.util.Log.e("AuthManager", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("AuthManager", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google using Credential Manager API
     *
     * This method uses the modern Credential Manager API which provides
     * a unified sign-in experience across Google accounts.
     *
     * @param context The context (activity or application context)
     * @param webClientId The OAuth 2.0 web client ID from Firebase Console
     * @return Result with FirebaseUser or exception
     */
    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String
    ): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "signInWithGoogle called")

            val credentialManager = CredentialManager.create(context)

            // Build Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            // Build credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            android.util.Log.d("AuthManager", "Requesting credentials...")

            // Get credential from Credential Manager
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            android.util.Log.d("AuthManager", "Credential received, processing...")

            // Handle the credential response
            handleGoogleSignInResult(result)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Google Sign-In failed", e)
            android.util.Log.e("AuthManager", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("AuthManager", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Handle the credential response from Credential Manager
     */
    private suspend fun handleGoogleSignInResult(
        result: GetCredentialResponse
    ): Result<FirebaseUser> {
        return try {
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                android.util.Log.d("AuthManager", "Processing Google ID token credential")

                // Extract the ID token from the credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                android.util.Log.d("AuthManager", "ID token extracted, authenticating with Firebase...")

                // Authenticate with Firebase using the Google ID token
                firebaseAuthWithGoogle(idToken)
            } else {
                android.util.Log.e("AuthManager", "Unexpected credential type: ${credential.type}")
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GoogleIdTokenParsingException) {
            android.util.Log.e("AuthManager", "Invalid Google ID token", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Error handling Google sign-in result", e)
            Result.failure(e)
        }
    }

    /**
     * Authenticate with Firebase using Google ID token
     */
    private suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthManager", "Creating Firebase credential from Google token")

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            android.util.Log.d("AuthManager", "Signing in to Firebase...")

            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
            if (user != null) {
                android.util.Log.d("AuthManager", "Firebase sign-in successful! User ID: ${user.uid}")
                android.util.Log.d("AuthManager", "User email: ${user.email}")
                android.util.Log.d("AuthManager", "User display name: ${user.displayName}")
                Result.success(user)
            } else {
                android.util.Log.e("AuthManager", "Firebase sign-in failed: User is null")
                Result.failure(Exception("Firebase authentication failed: User is null"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Firebase authentication exception", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user and clear credential state
     *
     * @param context The context to clear credential state
     */
    suspend fun signOut(context: Context) {
        try {
            auth.signOut()

            // Clear credential state to sign out from Google account
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())

            android.util.Log.d("AuthManager", "Sign out successful")
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Error during sign out", e)
        }
    }

    /**
     * Sign out the current user (Firebase only)
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get user ID token (useful for API calls)
     *
     * @param forceRefresh Whether to force refresh the token
     * @return The ID token string or null if user not authenticated
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return try {
            val user = getCurrentUser() ?: return null
            val result = user.getIdToken(forceRefresh).await()
            result.token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user claims from the ID token
     *
     * @return Map of claims or null if user not authenticated
     */
    suspend fun getUserClaims(): Map<String, Any>? {
        return try {
            val user = getCurrentUser() ?: return null
            val result = user.getIdToken(false).await()
            result.claims
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Add authentication state listener
     *
     * @param listener The listener to be notified of auth state changes
     */
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    /**
     * Remove authentication state listener
     *
     * @param listener The listener to remove
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return getCurrentUser()?.email
    }

    /**
     * Get user display name
     */
    fun getUserDisplayName(): String? {
        return getCurrentUser()?.displayName
    }

    /**
     * Get user ID
     */
    fun getUserId(): String? {
        return getCurrentUser()?.uid
    }

    /**
     * Get user photo URL
     */
    fun getUserPhotoUrl(): String? {
        return getCurrentUser()?.photoUrl?.toString()
    }
}
