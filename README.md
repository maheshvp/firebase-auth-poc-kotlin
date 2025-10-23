# Firebase Authentication POC - OpenID Connect (Android Kotlin)

A proof-of-concept Android application demonstrating OpenID Connect (OIDC) authentication with Firebase using Kotlin.

## Features

- ✅ OpenID Connect authentication flow
- ✅ Firebase Authentication integration
- ✅ Clean Material Design UI
- ✅ Secure token management
- ✅ User session persistence
- ✅ Sign-out functionality

## Prerequisites

Before running this project, make sure you have:

1. **Android Studio** or **Visual Studio Code** with Android development setup
2. **JDK 11** or higher
3. **Android SDK** (API level 24 or higher)
4. **Firebase project** with Authentication enabled
5. **OIDC provider** configured (Azure AD, Okta, Auth0, etc.)

## Firebase Setup

Since you mentioned Firebase setup is already completed, ensure you have:

1. ✅ Created a Firebase project
2. ✅ Added an Android app to your Firebase project
3. ✅ Configured OIDC provider in Firebase Console

### Configure OIDC Provider in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Authentication** → **Sign-in method**
4. Click on **Custom providers** section
5. Click **Add new provider** → **OpenID Connect**
6. Fill in the required information:
   - **Name**: Your provider name (e.g., "azure_test", "okta", "auth0")
   - **Client ID**: From your OIDC provider
   - **Issuer URL**: Your OIDC provider's issuer URL
   - **Client Secret**: From your OIDC provider
7. Save the configuration
8. Note the **Provider ID** (format: `oidc.{your-provider-name}`)

### Download google-services.json

1. In Firebase Console, go to **Project Settings** → **Your apps**
2. Click on your Android app
3. Download the `google-services.json` file
4. Place it in the `app/` directory of this project

## Project Setup

### 1. Clone or Copy the Project

```bash
cd firebase-auth-poc-kotlin
```

### 2. Add google-services.json

Copy your `google-services.json` file to the `app/` directory:

```
firebase-auth-poc-kotlin/
├── app/
│   ├── google-services.json  ← Place your file here
│   ├── build.gradle
│   └── src/
```

### 3. Configure OIDC Provider Settings

Create the configuration file from the template:

```bash
# Copy the template file
cp app/src/main/res/raw/config_properties_template app/src/main/res/raw/config_properties
```

Then edit `app/src/main/res/raw/config_properties` and update the OIDC provider ID:

```properties
# REQUIRED: Update with your provider ID from Firebase Console
oidc.provider.id=oidc.your_provider_name

# OPTIONAL: Add custom parameters if needed
# oidc.custom.params=tenant=your-tenant-id

# OPTIONAL: Specify scopes
# oidc.scopes=openid,email,profile

# OPTIONAL: Enable development mode for testing
oidc.development.mode=false
```

Replace `your_provider_name` with the name you configured in Firebase Console.

**Note**: The `config_properties` file is excluded from git to keep your credentials safe. Always use the template file as a reference.

### 4. Configure Gradle (if needed)

The project is already configured with necessary dependencies. If you need to update versions, check:

- `build.gradle` (project level)
- `app/build.gradle` (app level)

## Build and Run

### Using Visual Studio Code

1. Install required VS Code extensions:
   - **Kotlin Language**
   - **Gradle for Java**
   - **Android iOS Emulator** (optional)

2. Open the project in VS Code:
   ```bash
   code .
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Connect an Android device or start an emulator

5. Install and run:
   ```bash
   ./gradlew installDebug
   ```

### Using Android Studio

1. Open Android Studio
2. Click **File** → **Open** → Select the `firebase-auth-poc-kotlin` folder
3. Wait for Gradle sync to complete
4. Click the **Run** button or press `Shift + F10`

### Using Command Line

```bash
# Build the project
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one step
./gradlew installDebug
```

## Project Structure

```
firebase-auth-poc-kotlin/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/firebaseauth/
│   │   │   │   ├── MainActivity.kt          # Entry point, auth state check
│   │   │   │   ├── LoginActivity.kt         # OIDC sign-in flow
│   │   │   │   ├── HomeActivity.kt          # Protected home screen
│   │   │   │   ├── AuthManager.kt           # Auth utility class
│   │   │   │   ├── AuthConfig.kt            # Configuration data class
│   │   │   │   └── ConfigManager.kt         # Configuration loader
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_login.xml
│   │   │   │   │   └── activity_home.xml
│   │   │   │   ├── raw/
│   │   │   │   │   ├── config_properties            # Your config (git ignored)
│   │   │   │   │   └── config_properties_template   # Template file
│   │   │   │   └── values/
│   │   │   │       ├── strings.xml
│   │   │   │       ├── colors.xml
│   │   │   │       └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   └── google-services.json                 # Add your Firebase config here
├── build.gradle
├── settings.gradle
└── README.md
```

## How It Works

### Authentication Flow

1. **App Launch** (`MainActivity.kt`)
   - Checks if user is already authenticated
   - Routes to `HomeActivity` if authenticated
   - Routes to `LoginActivity` if not authenticated

2. **Sign In** (`LoginActivity.kt`)
   - User clicks "Sign in with OpenID Connect"
   - App loads OIDC Provider ID from `config_properties`
   - Firebase initiates OAuth flow with OIDC provider
   - User authenticates with OIDC provider
   - Firebase receives and validates the ID token
   - User is signed in to Firebase
   - Note: Development mode can be enabled to show provider ID dialog for testing

3. **Home Screen** (`HomeActivity.kt`)
   - Displays user information (name, email, user ID)
   - Shows ID token claims
   - Provides sign-out functionality

4. **Sign Out**
   - Signs out from Firebase
   - Returns to login screen

### Key Files

#### MainActivity.kt
Entry point that checks authentication state and routes accordingly.

#### LoginActivity.kt
Handles OIDC authentication using `OAuthProvider`:
```kotlin
val provider = OAuthProvider.newBuilder(providerId)
auth.startActivityForSignInWithProvider(this, provider.build())
```

#### HomeActivity.kt
Protected screen displaying user information and sign-out option.

#### AuthManager.kt
Utility class providing:
- Sign-in with OIDC (coroutine-based)
- Sign-out
- Get ID token
- Get user claims
- Auth state listeners

#### ConfigManager.kt
Singleton class that:
- Loads configuration from `config_properties` file
- Parses OIDC settings (provider ID, scopes, custom parameters)
- Provides type-safe access to configuration
- Handles configuration errors gracefully

#### AuthConfig.kt
Data class holding authentication configuration:
- OIDC provider ID
- Custom parameters
- OAuth scopes
- Development mode flag

## Common OIDC Providers Configuration

### Azure AD (Microsoft)

**Provider ID**: `oidc.azure`

**Configuration**:
- Issuer URL: `https://login.microsoftonline.com/{tenant-id}/v2.0`
- Scopes: `openid`, `profile`, `email`

### Okta

**Provider ID**: `oidc.okta`

**Configuration**:
- Issuer URL: `https://{your-domain}.okta.com`
- Scopes: `openid`, `profile`, `email`

### Auth0

**Provider ID**: `oidc.auth0`

**Configuration**:
- Issuer URL: `https://{your-domain}.auth0.com/`
- Scopes: `openid`, `profile`, `email`

### Google (as OIDC)

**Provider ID**: `oidc.google`

**Configuration**:
- Issuer URL: `https://accounts.google.com`
- Scopes: `openid`, `email`, `profile`

## Testing

### Test the Authentication Flow

1. Launch the app
2. Click "Sign in with OpenID Connect"
3. Authenticate with your OIDC provider (provider ID from config is used automatically)
4. Verify user information on home screen
5. Test sign-out functionality

### Development Mode Testing

To test different provider IDs without editing the config file:

1. Set `oidc.development.mode=true` in `config_properties`
2. Launch the app
3. Click "Sign in with OpenID Connect"
4. A dialog will appear allowing you to enter/change the provider ID
5. Test with different provider IDs as needed

### Debug Mode

The app includes logging for debugging:
- Check Android Logcat for authentication logs
- Tag: `LoginActivity`, `HomeActivity`

```bash
# View logs
adb logcat | grep -E "LoginActivity|HomeActivity"
```

## Troubleshooting

### Issue: "Provider not configured"

**Solution**:
- Verify OIDC provider is added in Firebase Console
- Check Provider ID matches exactly (case-sensitive)
- Ensure `google-services.json` is in the `app/` directory

### Issue: "Network error"

**Solution**:
- Check internet connection
- Verify Firebase project is active
- Check if OIDC provider endpoints are accessible

### Issue: Build fails with "Missing google-services.json"

**Solution**:
- Download `google-services.json` from Firebase Console
- Place it in `app/` directory
- Rebuild the project

### Issue: Authentication succeeds but user info is null

**Solution**:
- Check OIDC provider configuration
- Verify requested scopes include `email` and `profile`
- Check token claims in Logcat

## Security Considerations

1. **Never commit sensitive files** to version control:
   - `google-services.json` (already in .gitignore)
   - `config_properties` (already in .gitignore)
   - Use the template files instead
2. Use **ProGuard/R8** for release builds to obfuscate code
3. Implement **certificate pinning** for production apps
4. Store sensitive data using **EncryptedSharedPreferences**
5. Validate ID tokens on your backend server
6. Handle token refresh appropriately
7. Implement proper **session management**
8. Review `config_properties` before deploying to production

## Next Steps

To extend this POC:

1. **Backend Integration**: Send ID tokens to your backend for verification
2. **Token Refresh**: Implement automatic token refresh
3. **Multi-provider**: Add support for multiple OIDC providers
4. **Error Handling**: Add comprehensive error handling and retry logic
5. **UI/UX**: Enhance UI with animations and better error messages
6. **Testing**: Add unit tests and UI tests
7. **Analytics**: Integrate Firebase Analytics for user tracking

## Dependencies

### Main Dependencies

- **Firebase BOM**: 32.7.0
- **Firebase Auth**: Latest (from BOM)
- **Firebase Analytics**: Latest (from BOM)
- **Play Services Auth**: 20.7.0
- **Material Components**: 1.11.0
- **AndroidX Core KTX**: 1.12.0
- **Coroutines**: 1.7.3

### Build Tools

- **Gradle**: 8.1.0
- **Kotlin**: 1.9.0
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## Resources

### Documentation

- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Firebase OIDC Documentation](https://firebase.google.com/docs/auth/android/openid-connect)
- [React Native Firebase OIDC](https://rnfirebase.io/auth/oidc-auth)
- [OAuth 2.0 and OpenID Connect](https://developers.google.com/identity/protocols/oauth2/openid-connect)

### OIDC Providers

- [Azure AD Configuration](https://docs.microsoft.com/en-us/azure/active-directory/)
- [Okta Configuration](https://developer.okta.com/)
- [Auth0 Configuration](https://auth0.com/docs)

## License

This is a proof-of-concept project for educational purposes.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Firebase documentation
3. Check your OIDC provider's documentation
4. Review Android Logcat for error details

---

**Note**: This is a POC (Proof of Concept) project. For production use, implement proper error handling, security measures, testing, and follow best practices for mobile app development.
