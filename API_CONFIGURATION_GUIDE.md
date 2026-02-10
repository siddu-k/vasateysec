# API Configuration Guide

This guide explains how to configure API keys and secrets for the Vasateysec application.

## Overview

All API keys and sensitive configuration are now stored in `local.properties` file, which is:
- **Never committed to version control** (listed in `.gitignore`)
- **Machine-specific** (each developer/build environment has their own)
- **Secure** (keeps secrets out of source code)

## Setup Instructions

### 1. Create local.properties file

Copy the example template:
```bash
cp local.properties.example local.properties
```

### 2. Configure Supabase

Edit `local.properties` and add your Supabase credentials:

```properties
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

**How to get these values:**
1. Go to your Supabase project dashboard
2. Click on Settings → API
3. Copy "Project URL" as `SUPABASE_URL`
4. Copy "anon public" key as `SUPABASE_ANON_KEY`

### 3. Configure FCM/Notification Endpoint

Add your notification service endpoint:

```properties
FCM_ENDPOINT=https://your-notification-service.vercel.app/api/sendNotification
```

### 4. Configure Keystore (for Release Builds)

Add your keystore credentials:

```properties
KEYSTORE_FILE=../vasateysec-release.jks
KEYSTORE_PASSWORD=your-keystore-password
KEY_ALIAS=vasateysec
KEY_PASSWORD=your-key-password
```

## File Structure

```
vasateysec/
├── local.properties          # Your actual configuration (NOT committed)
├── local.properties.example  # Template (committed to repo)
└── app/
    ├── build.gradle.kts      # Reads from local.properties
    └── src/main/java/
        └── com/sriox/vasateysec/
            └── config/
                └── ApiConfig.kt  # Centralized API configuration
```

## How It Works

1. **Build Time**: Gradle reads `local.properties` during build
2. **BuildConfig Generation**: Values are injected into `BuildConfig` class
3. **Runtime Access**: `ApiConfig.kt` provides easy access to these values
4. **Type Safety**: Kotlin object ensures compile-time safety

## Security Best Practices

✅ **DO:**
- Keep `local.properties` private and secure
- Use different credentials for development vs. production
- Rotate API keys regularly
- Share credentials through secure channels (password manager, etc.)

❌ **DON'T:**
- Commit `local.properties` to version control
- Share credentials in plain text (email, chat, etc.)
- Use production credentials in development
- Hardcode API keys in source code

## Troubleshooting

### Build fails with "property not found"
- Make sure `local.properties` exists in the project root
- Verify all required properties are defined
- Check property names match exactly (case-sensitive)

### App crashes at runtime
- Verify your API keys are correct
- Check network connectivity
- Review logcat for specific error messages

## For CI/CD

In continuous integration environments:
1. Create `local.properties` as part of the build script
2. Use environment variables or secrets management
3. Example GitHub Actions:
   ```yaml
   - name: Create local.properties
     run: |
       echo "SUPABASE_URL=${{ secrets.SUPABASE_URL }}" >> local.properties
       echo "SUPABASE_ANON_KEY=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
       echo "FCM_ENDPOINT=${{ secrets.FCM_ENDPOINT }}" >> local.properties
   ```

## Migration from Hardcoded Values

**IMPORTANT**: The values below are examples only and may be outdated. Contact your team lead or check your project documentation for current credentials.

If you were using the old hardcoded values, your `local.properties` might look similar to this:

```properties
# Example configuration (replace with your actual credentials)
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-actual-anon-key-here

# Example FCM endpoint (replace with your actual endpoint)
FCM_ENDPOINT=https://your-notification-service.vercel.app/api/sendNotification

# Example keystore configuration (if doing release builds)
KEYSTORE_FILE=../vasateysec-release.jks
KEYSTORE_PASSWORD=your-keystore-password
KEY_ALIAS=vasateysec
KEY_PASSWORD=your-key-password
```

**Note:** Replace all example values with your actual credentials. Never copy credentials from documentation or commit messages.
