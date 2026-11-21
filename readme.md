# Description
"chiara-select2speak" is an Android accessibility service that allows to read aloud text displayed on the screen, to ease access for users with reading difficulties.

Users can select an area on the screen and the service will read aloud the text inside it.
Demo video: [here](https://youtu.be/mUp831sS0lo).

Functioning:
- The user activates the service from the Android device Accessibility Settings
- Once active, a "Play" button is shown on top of any viable content on the screen
- When user presses the "Play" button, the service takes a screenshot of the whole screen
- The user can then:
  - drag their finger on the screen, to draw a selection rectangle around the area of interest
  - touch on one point on the screen, to select a point around which there is text of interest
- The service applies OCR to recognize the text inside the rectangle or around the selected point
- The service uses TTS to speak out loud the recognized text to the user

# Inspiration
I developed this service to enable my sister, Chiara, to independently play with her favourite videogames - without the need of asking help to read the text to anyone :)

Read and watch a summary of the inspiration behind this project [here](https://www.linkedin.com/feed/update/urn:li:activity:6668395321850134528/) <3

This service is inspired by the [Android Select to Speak](https://support.google.com/accessibility/android/answer/7349565?hl=en) service, and builds additional functionalities on top of it. Indeed the native the "Android Select to Speak" service can **not** read text not directly exposed to the Android operating system (e.g. text inside apps, images, and videogames).
"chiara-select2speak" is specifically developed to also enable recognizing (and reading-aloud) such text.

# Setup
## Requirements
This service was successfully tested under:
- Galaxy Tab S7 FE [SM-T733]
- Android: v14

## Integrations
- Firebase Analytics & Crashlytics are used to store execution errors on Firebase, to enable remote analysis if
physical access to the tablet running this app is not possible
- Firebase is used to store all text OCR'd by this app

The steps below explain how to setup these integrations

### 1. Firebase and Android 
To enable storing crashes to Firebase Analytics & Crashlytics, follow these instructions 

#### 1.1. Obtain `google-services.json`
To connect this app to your Firebase project and enable crash reporting and analytics:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project or select your existing project.
3. In your project overview, click the **Add app** button (or **Android** icon) to add an Android app.
4. Enter the Android package name:
   * **Package name**: `com.chiara.accessibilityservices`
   * *(Optional)* App nickname and Debug signing certificate SHA-1.
5. Click **Register app**.
6. Download the **`google-services.json`** configuration file.

#### 1.2. Install `google-services.json`
Once downloaded, place the `google-services.json` file directly into your Android app module directory:

```text
android-project/
├── app/
│   ├── google-services.json   <-- Place your downloaded file here!
│   ├── build.gradle
│   └── src/
├── build.gradle
└── README.md
```

#### 1.3. Verifying Firebase Crashlytics installation
This app includes built-in test crash handling to verify your Firebase Crashlytics setup:

1. Build and install the app on your Android device or tablet:
   ```bash
   ./gradlew app:assembleDebug
   ```
2. Open the app and navigate to the **Settings** screen.
3. Tap the **Test Crash** button. This will force a test exception and crash the app.
4. **Re-open the app** once. The Firebase SDK will automatically upload the crash report in the background.
5. Open your [Firebase Console](https://console.firebase.google.com/) -> **Crashlytics** dashboard to view the test crash report and device stack traces.

### 2. Firebase authentication (Google sign-in)
The app saves **every text it OCR's from the screen** to Firebase Storage, under a daily folder named `YYYYMMDD_select2speak` (e.g. `20261121_select2speak`), in a single shared `data.json` file.

A selection is stored **all or nothing, and only if the Text-to-Speech engine reads it to the end**: when the user taps anywhere on the screen (or presses **Stop**) while Chiara is still speaking, the engine is stopped and **nothing** from that selection is uploaded — not even the blocks that had already been read. Status messages ("Nessun testo trovato", capture errors, the welcome message) and replays of a previous selection are never recorded either.

The rule is implemented with a pending queue keyed by `utteranceId`: `bitmapToSpeech()` queues each OCR text block, `UtteranceProgressListener.onDone()` marks a block as read, `finishSpeechOverlay(true)` flushes the whole selection to Firebase in the order it was spoken, and every interruption path (`onTouch`, Stop button, replay, new selection, engine restart, `onInterrupt()`, `onDestroy()`, `onError()`) drops the queue.

Each record follows **`chiara-speech2text_schema-data` v1.2.2** (`docs/schemas/schema-data_v1.2.2.json` of `chiara-speech2text_private-v3`). This app records no audio and runs no ASR inference, so only `entry_id`, `entry_timestamp` and `inference_transcript` (the OCR'd text) carry real data; the other fields are written with the `"n/a"` placeholder so the complete set of keys is always present — except `reviewer_is_reviewed`, which is `false` because no record has been reviewed when the app stores it:

```json
{
  "schema_version": "1.2.2_20261121",
  "data": [
    {
      "entry_id": "f6143cc1-dc39-4520-8034-1316bbbc93a2",
      "entry_timestamp": "2026-11-21T18:31:24.324Z",
      "inference_transcript": "Ciao Luca, come stai?",
      "audio_data": "n/a", "audio_duration": "n/a", "audio_source": "n/a",
      "inference_confidence": "n/a", "inference_model": "n/a",
      "user_is_validated": "n/a", "user_transcript_processed": "n/a",
      "reviewer_is_reviewed": false, "reviewer_transcript_corrected": "n/a"
    }
  ]
}
```

`entry_timestamp` is the moment the block was spoken, not the moment of the upload: the blocks of a selection are uploaded together, right after the last one has been read.

Records written by older builds (`{"timestamp": ..., "text": ...}`) are migrated to this shape automatically the next time the app appends to that day's file.

Storage access is **authenticated**: uploads only happen when a user is signed in, so the Storage security rules can require `request.auth != null` instead of exposing the data publicly.

The app uses **Sign in with Google through the browser**: tapping **Sign in with Google** on the Settings screen opens the Google account page in a Chrome Custom Tab, and Firebase handles the token exchange. There is **no API key, client ID, or password to store anywhere** — everything comes from `google-services.json` and the Firebase Console configuration.

#### 2.1. Enable the Google sign-in provider
1. Go to the [Firebase Console](https://console.firebase.google.com/) -> **Build** -> **Authentication**.
2. Click **Get started** (if this is the first time) and select **Google** as your sign-in provider.
3. Open the **Sign-in method** tab, click **Google**, set **Enable** to *on*.
4. Enter a **Project public email** (shown to users on the account-chooser page) and click **Save**.
5. Go to **Authentication -> Settings -> Authorized domains** and verify that `<project-id>.firebaseapp.com` is listed. This is the domain the browser redirects back to; if it is missing, the sign-in flow fails with `CONFIGURATION_NOT_FOUND`.

#### 2.2. Register the signing certificate fingerprint
Firebase identifies an Android app by **package name + signing certificate hash**. If the certificate of the APK running on the device is not registered, Firebase Auth rejects the request with `INVALID_CERT_HASH`.

1. Get the fingerprint of the build you are actually installing (from the project root):
   ```bash
   # prints SHA-1 / SHA-256 / MD5 of the signing certificate
   $ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs \
       app/build/outputs/apk/debug/app-debug.apk
   ```
   Alternatively, for the default debug keystore:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -storepass android
   ```
2. In the Firebase Console, open **Project settings -> General -> Your apps -> `com.chiara.accessibilityservices`**.
3. Under **SHA certificate fingerprints**, click **Add fingerprint** and paste the **SHA-1** value. Add the **SHA-256** as a second entry as well.
4. Click **Save**, then **download `google-services.json` again** and replace `android-project/app/google-services.json`. Registering the fingerprint with the Google provider enabled also populates the previously empty `oauth_client` array in that file.
5. Wait about a minute for the new config to propagate server-side before signing in.

> [!IMPORTANT]
> Debug and release builds are signed with different certificates. When you ship a release build, add the SHA-1 of your **release keystore** too, otherwise Firebase Authentication will reject the release build with `INVALID_CERT_HASH`.

#### 2.3. Enable Firebase Storage
1. In the Firebase Console, go to **Build** -> **Storage** -> **Get started** and choose a location.
2. The bucket name is written into `google-services.json` under `project_info.storage_bucket`. The app uses that value automatically (`FirebaseStorage.getInstance()`), so **do not hardcode a bucket URL in the code** — if you change the bucket, just re-download `google-services.json`.

Lock the data down to signed-in users only with security rules (**Storage -> Rules**):

```text
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Only signed-in users may write/read the daily spoken-text files
    match /{folder}/data.json {
      allow read, write: if request.auth != null
                        && folder.matches('^[0-9]{8}_select2speak$');
    }
  }
}
```

#### 2.4. Sign in from the app
1. Build and install the app: `./gradlew app:assembleDebug`.
2. Open the app and go to the **Settings** screen.
3. Tap **Sign in with Google** — the Google account page opens in the browser.
4. Pick an account and approve; the app returns to the Settings screen and shows **Signed in as: <email>**.
5. The session is persisted by the Firebase SDK, so you only need to sign in once; the button then becomes **Sign out**.

#### 2.5. Verifying the OCR-text upload
1. With the app signed in, activate the accessibility service and have it read some text aloud.
2. Open **Firebase Console -> Storage**: you should see `<YYYYMMDD>_select2speak/data.json`.
3. The file is a JSON object `{"schema_version": ..., "data": [...]}`: every entry carries the
   schema v1.2.2 keys, with the recognized text in `inference_transcript` (see the example above).
   A selection appears only after Chiara has read all of it: interrupt her with a tap and
   nothing is written at all.
4. `logcat -s Chiara_MainService` shows `[Firebase] Successfully saved spoken text to ...` for each
   record, or why it was skipped (`User is not authenticated`, failed attempts).

#### 2.6. Troubleshooting
| Message / log | Cause | Fix |
|---|---|---|
| `An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]` | Google provider disabled, or the `firebaseapp.com` authorized domain is missing | Steps 2.1.3 and 2.1.5 |
| `[GetAuthDomainTask] Error getting project config. Failed with INVALID_CERT_HASH 400` | The SHA-1 of the installed APK is not registered in Firebase | Step 2.2 |
| `User does not have permission to access this object` (Storage `403`) | The user is signed out, or the Storage rules do not allow authenticated access | Tap **Sign in with Google**; check the rules in step 2.3 |
| Nothing is uploaded although the app is signed in | The bucket in `google-services.json` does not exist / was renamed | Re-download `google-services.json` (step 2.3.2) |
| No browser opens when tapping the button | No browser supporting Custom Tabs is installed on the device | Install/enable Chrome on the device |

> [!NOTE]
> Authentication is fully handled at runtime by the Firebase SDK: no secret token, client ID or password is stored in the repository, in `strings.xml`, or in any config file.
