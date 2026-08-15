# Metadata
Name:	chiara-select2read

Author: Luca Randazzo

Date:   July 2020

email:  hackahealth.geneva@gmail.com

# Information
## Description
"chiara-select2read" is an Android accessibility service that allows to read aloud text displayed on the screen, to ease access for users with reading difficulties.

When active, the service overlays on top of any visible content on the screen, allowing users to select an area on the screen and to read aloud the text inside it.

This service is inspired by the super cool "Android Select to Speak" (https://support.google.com/accessibility/android/answer/7349565?hl=en), and builds some cool functionalities on top of it. "chiara-select2read" enables indeed to recognize and read aloud also text that is not directly exposed to the Android operating system (e.g. text inside apps, images, and videogames) - where this text is not accessible by the "Android Select to Speak" service.

The project is hosted here: https://github.com/HackaHealth-Geneva/chiara-select2read

## Inspiration
I developed this service to enable my sister, Chiara, to independently play with her favourite videogames - without the need of asking help to read the text to anyone :)

Read and watch a summary of the inspiration behind this project here: https://www.linkedin.com/feed/update/urn:li:activity:6668395321850134528/ <3

# Functioning
A video showing how this service works is available here: https://youtu.be/mUp831sS0lo

In short:
- The user activates the service from the Accessibility Settings of the Android device
- Once active, a "Start" button is shown on top of other apps
- When the user presses the "Start" button, the service takes a screenshot of the whole screen
- The user can then drags his/her finger on the screen to draw a selection rectangle around the area of interest. A simple UI shows the rectangle being drawn by the user
- The service crops the screenshot to the area of interest, it applies OCR to recognize the text inside the cropped image, and then it uses TTS to speak out loud the recognized text

The service was successfully tested under:
- Galaxy Tab A (2016) [SM-T585]
- Android: 8.10 (Oreo) [API level: 27]

The service was compiled with Android Studio 4.0.1

# ToDo
Some ideas for improvement are given below.

## OCR
- "Intelligent" speaking-out-loud of text "around" the user-selected area of interest
- Speaking-out-loud of text around a clicked point (instead of requiring users to create a selection rectangle)

These features could help users who are not able to finely create selection rectangles or to finaly select all text of interest (e.g. users with impaired fine motor control) to use the service.
The "Android Select to Speak" service implements these features.

## UI
- The UI button ("Start") should be movable by the user around the screen, to avoid covering potential areas of interest
- The UI should enable to stop the TTS-engine (e.g. if a very large section of text has been selected, which is not of interest any longer)
- Potentially, pause/rewind features could be implemented as well
- Highlight word by word as they're being spoken out loud

The "Android Select to Speak" service implements these features.

## Language and Locale
- Select the locale of the TTS-engine
- Provide error messages and debug information to the user through the TTS-engine in the selected locale

## Goodies
- Select welcome message by TTS-engine

# Resources
Many freely-available resources were used for the development of this service.
An enormous thanks to Google for providing lots of advanced functions and easy-to-use APIs through their Android OS, and many thanks to the developers whose code helped to create several parts of this service.
Following, a non-comprehensive list of some of these inspiring resources is provided.

## Android Accessibility services
### Tutorials
- https://codelabs.developers.google.com/codelabs/developing-android-a11y-service/#0
- https://developer.android.com/guide/topics/ui/accessibility/service
- https://developer.android.com/guide/topics/ui/accessibility

### Other services
- https://github.com/google/talkback
- https://github.com/googlecodelabs/android-accessibility

### References
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

## Screenshot
- https://stackoverflow.com/questions/51320978/android-taking-a-screenshot-with-mediaprojectionmanager-onimageavailable-doesn
- https://stackoverflow.com/questions/32513379/how-to-record-screen-and-take-screenshots-using-android-api
- https://github.com/mtsahakis/MediaProjectionDemo

### Handling bitmaps
- https://stackoverflow.com/questions/15789049/crop-a-bitmap-image
- https://stackoverflow.com/questions/16804404/create-a-bitmap-drawable-from-file-path/16804467

## OCR
- https://github.com/googlesamples/android-vision
- https://medium.com/@prakash_pun/text-recognition-for-android-using-google-mobile-vision-a8ffabe3f5d6
- https://github.com/prakashpun/TextRecognitionAndroid
- https://firebase.google.com/docs/ml-kit/android/recognize-text
- https://developers.google.com/vision/android/text-overview
- https://www.programcreek.com/java-api-examples/?api=com.google.android.gms.vision.text.TextRecognizer

## TTS
- https://code.tutsplus.com/tutorials/android-sdk-using-the-text-to-speech-engine--mobile-8540

## UI
### Processing touch events
- https://stackoverflow.com/questions/30074771/how-to-detect-whether-screen-is-touched-in-a-background-service-in-android-studi
- https://developer.android.com/training/gestures

### Draw overlay
- https://stackoverflow.com/questions/4481226/creating-a-system-overlay-window-always-on-top
- http://android-er.blogspot.com/2013/09/detect-touch-and-draw-rect-on-bitmap.html
