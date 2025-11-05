package com.example.firebaseauth

/**
 * AuthConfig - Data class for authentication configuration (OIDC and SAML)
 *
 * This class holds the configuration values loaded from config.properties
 * at runtime. It provides type-safe access to authentication settings.
 */
data class AuthConfig(
    /**
     * The OIDC provider ID configured in Firebase Console
     * Format: "oidc.{provider-name}" (e.g., "oidc.auth0", "oidc.azure")
     */
    val oidcProviderId: String?,

    /**
     * The SAML provider ID configured in Firebase Console
     * Format: "saml.{provider-name}" (e.g., "saml.auth0", "saml.okta")
     */
    val samlProviderId: String?,

    /**
     * Optional custom parameters to pass to the provider
     * Note: Primarily used for OIDC providers
     * Example: mapOf("tenant" to "your-tenant-id")
     */
    val customParameters: Map<String, String> = emptyMap(),

    /**
     * Optional scopes to request from the provider
     * Note: Primarily used for OIDC providers
     * Example: listOf("openid", "email", "profile")
     */
    val scopes: List<String> = emptyList(),

    /**
     * Enable development mode to show provider ID dialog for testing
     */
    val developmentMode: Boolean = false
) {
    /**
     * Check if OIDC provider is configured
     */
    fun hasOIDC(): Boolean = !oidcProviderId.isNullOrEmpty()

    /**
     * Check if SAML provider is configured
     */
    fun hasSAML(): Boolean = !samlProviderId.isNullOrEmpty()
}
