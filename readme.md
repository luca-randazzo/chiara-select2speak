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

# Requirements
The service was successfully tested under:
- Galaxy Tab A (2016) [SM-T585]
- Android: 8.10 (Oreo) [API level: 27]

# Setup
## 1. Configure Firebase and Android 
To enable storing crashes to Firebase Analytics & Crashlytics, follow these instructions 

### 1.1. Obtain `google-services.json`
To connect this app to your Firebase project and enable crash reporting and analytics:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project or select your existing project.
3. In your project overview, click the **Add app** button (or **Android** icon) to add an Android app.
4. Enter the Android package name:
   * **Package name**: `com.chiara.accessibilityservices`
   * *(Optional)* App nickname and Debug signing certificate SHA-1.
5. Click **Register app**.
6. Download the **`google-services.json`** configuration file.

## 1.2. Install `google-services.json`
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

### 1.3. Verifying Firebase Crashlytics installation
This app includes built-in test crash handling to verify your Firebase Crashlytics setup:

1. Build and install the app on your Android device or tablet:
   ```bash
   ./gradlew app:assembleDebug
   ```
2. Open the app and navigate to the **Settings** screen.
3. Tap the **Test Crash** button. This will force a test exception and crash the app.
4. **Re-open the app** once. The Firebase SDK will automatically upload the crash report in the background.
5. Open your [Firebase Console](https://console.firebase.google.com/) -> **Crashlytics** dashboard to view the test crash report and device stack traces.
