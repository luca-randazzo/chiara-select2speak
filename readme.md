# Description
"chiara-select2speak" is an Android accessibility service that allows to read aloud text displayed on the screen, to ease access for users with reading difficulties.

The service enables users to select an area on the screen and to read aloud the text inside it.
Demo video: https://youtu.be/mUp831sS0lo

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

Read and watch a summary of the inspiration behind this project here: https://www.linkedin.com/feed/update/urn:li:activity:6668395321850134528/ <3

This service is inspired by the "Android Select to Speak" (https://support.google.com/accessibility/android/answer/7349565?hl=en), and builds additional functionalities on top of it. Indeed the native the "Android Select to Speak" service can **not** read text not directly exposed to the Android operating system (e.g. text inside apps, images, and videogames).
"chiara-select2speak" is specifically developed to also enable recognizing (and reading-aloud) such text.  

# Requirements
The service was successfully tested under:
- Galaxy Tab A (2016) [SM-T585]
- Android: 8.10 (Oreo) [API level: 27]

The service was compiled with Android Studio 4.0.1
