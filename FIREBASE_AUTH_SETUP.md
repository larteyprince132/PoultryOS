# Firebase Authentication Setup

The Android app is wired for Firebase email and password authentication. The Firebase project itself is not connected yet because this repository does not contain `google-services.json`.

## Firebase project settings

1. Create or select the Firebase project for PoultryOS.
2. Add an Android app with package name `com.aistudio.poultryos.ghfarm`.
3. In **Authentication → Sign-in method**, enable **Email/Password**.
4. Download the Android `google-services.json` and place it at `app/google-services.json`.
5. Build and run the app, then verify registration, sign-in, sign-out, and password reset email delivery.

The current screens use email and password. Phone number sign-in requires a separate SMS verification flow and is not enabled yet.

Farm records continue to live in local Room storage. Firebase Authentication verifies the account; it does not sync farm records to Firestore. The app requires internet for first sign-in, registration, and password reset. A signed-in account and its local farm records remain available offline.

Firebase's Android setup and password authentication guides:

- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
