package com.example.firebaseauth

/**
 * AuthConfig - Data class for OIDC authentication configuration
 *
 * This class holds the configuration values loaded from config.properties
 * at runtime. It provides type-safe access to authentication settings.
 */
data class AuthConfig(
    /**
     * The OIDC provider ID configured in Firebase Console
     * Format: "oidc.{provider-name}" (e.g., "oidc.auth0", "oidc.azure")
     */
    val oidcProviderId: String,

    /**
     * Optional custom parameters to pass to the OIDC provider
     * Example: mapOf("tenant" to "your-tenant-id")
     */
    val customParameters: Map<String, String> = emptyMap(),

    /**
     * Optional scopes to request from the OIDC provider
     * Example: listOf("openid", "email", "profile")
     */
    val scopes: List<String> = emptyList(),

    /**
     * Enable development mode to show provider ID dialog for testing
     */
    val developmentMode: Boolean = false
)
