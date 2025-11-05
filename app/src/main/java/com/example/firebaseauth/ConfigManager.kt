package com.example.firebaseauth

import android.content.Context
import java.io.IOException
import java.util.Properties

/**
 * ConfigManager - Singleton for managing application configuration
 *
 * This class loads configuration from the config.properties file in res/raw
 * and provides type-safe access to configuration values.
 *
 * Usage:
 * ```
 * val config = ConfigManager.getAuthConfig(context)
 * ```
 */
object ConfigManager {

    @Volatile
    private var authConfig: AuthConfig? = null

    /**
     * Get authentication configuration
     * Loads from config_properties file on first access and caches the result
     *
     * @param context Android context (will use applicationContext internally)
     * @return AuthConfig object with loaded configuration
     * @throws ConfigurationException if config file is missing or invalid
     */
    fun getAuthConfig(context: Context): AuthConfig {
        // Return cached config if available
        authConfig?.let { return it }

        // Load and parse config
        val properties = loadProperties(context.applicationContext)
        val config = parseAuthConfig(properties)

        // Cache and return
        authConfig = config
        return config
    }

    /**
     * Load properties from the config_properties file
     */
    private fun loadProperties(context: Context): Properties {
        val properties = Properties()

        try {
            val inputStream = context.resources.openRawResource(R.raw.config_properties)
            inputStream.use { stream ->
                properties.load(stream)
            }
            android.util.Log.d("ConfigManager", "Successfully loaded config_properties")
        } catch (e: IOException) {
            android.util.Log.e("ConfigManager", "Failed to load config_properties", e)
            throw ConfigurationException(
                "Failed to load config_properties. " +
                "Make sure the file exists in app/src/main/res/raw/config_properties",
                e
            )
        } catch (e: Exception) {
            android.util.Log.e("ConfigManager", "Unexpected error loading config", e)
            throw ConfigurationException("Unexpected error loading configuration", e)
        }

        return properties
    }

    /**
     * Parse properties into AuthConfig object
     * Supports both OIDC and SAML provider configurations simultaneously
     */
    private fun parseAuthConfig(properties: Properties): AuthConfig {
        // Get OIDC provider ID
        val oidcProviderId = properties.getProperty("oidc.provider.id")?.trim()

        // Validate OIDC provider ID format if provided
        if (!oidcProviderId.isNullOrEmpty() && !oidcProviderId.startsWith("oidc.")) {
            throw ConfigurationException(
                "Invalid OIDC provider ID format: '$oidcProviderId'. " +
                "OIDC provider ID must start with 'oidc.' (e.g., 'oidc.auth0')"
            )
        }

        // Get SAML provider ID
        val samlProviderId = properties.getProperty("saml.provider.id")?.trim()

        // Validate SAML provider ID format if provided
        if (!samlProviderId.isNullOrEmpty() && !samlProviderId.startsWith("saml.")) {
            throw ConfigurationException(
                "Invalid SAML provider ID format: '$samlProviderId'. " +
                "SAML provider ID must start with 'saml.' (e.g., 'saml.auth0')"
            )
        }

        // At least one provider must be configured
        if (oidcProviderId.isNullOrEmpty() && samlProviderId.isNullOrEmpty()) {
            throw ConfigurationException(
                "At least one provider must be configured. " +
                "Please add 'oidc.provider.id' and/or 'saml.provider.id' in config_properties."
            )
        }

        // Parse optional custom parameters
        val customParamsString = properties.getProperty("oidc.custom.params", "")?.trim() ?: ""
        val customParameters = parseCustomParameters(customParamsString)

        // Parse optional scopes
        val scopesString = properties.getProperty("oidc.scopes", "")?.trim() ?: ""
        val scopes = parseScopes(scopesString)

        // Parse development mode flag
        val developmentMode = properties.getProperty("auth.development.mode", "false")?.trim()?.toBoolean() ?: false

        android.util.Log.d("ConfigManager", "Loaded config - OIDC: $oidcProviderId, SAML: $samlProviderId, Dev Mode: $developmentMode")

        return AuthConfig(
            oidcProviderId = oidcProviderId,
            samlProviderId = samlProviderId,
            customParameters = customParameters,
            scopes = scopes,
            developmentMode = developmentMode
        )
    }

    /**
     * Parse custom parameters from comma-separated key=value pairs
     * Example: "tenant=abc123,domain=example.com"
     */
    private fun parseCustomParameters(paramsString: String): Map<String, String> {
        if (paramsString.isEmpty()) {
            return emptyMap()
        }

        return try {
            paramsString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .associate { param ->
                    val (key, value) = param.split("=", limit = 2)
                    key.trim() to value.trim()
                }
        } catch (e: Exception) {
            android.util.Log.w("ConfigManager", "Failed to parse custom parameters: $paramsString", e)
            emptyMap()
        }
    }

    /**
     * Parse scopes from comma-separated list
     * Example: "openid,email,profile"
     */
    private fun parseScopes(scopesString: String): List<String> {
        if (scopesString.isEmpty()) {
            return emptyList()
        }

        return scopesString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Reload configuration (useful for testing or development)
     */
    fun reloadConfig() {
        authConfig = null
        android.util.Log.d("ConfigManager", "Configuration cache cleared")
    }
}

/**
 * Exception thrown when configuration cannot be loaded or is invalid
 */
class ConfigurationException(message: String, cause: Throwable? = null) : Exception(message, cause)
