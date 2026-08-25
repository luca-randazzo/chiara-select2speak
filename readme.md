# Metadata
Name:	chiara-select2speak

Author: Luca Randazzo

# Information
## Description
"chiara-select2speak" is an Android accessibility service that allows to read aloud text displayed on the screen, to ease access for users with reading difficulties.

When active, the service overlays on top of any visible content on the screen, allowing users to select an area on the screen and to read aloud the text inside it.

This service is inspired by the "Android Select to Speak" (https://support.google.com/accessibility/android/answer/7349565?hl=en), and builds additional functionalities on top of it. Indeed the native the "Android Select to Speak" service can not read text not directly exposed to the Android operating system (e.g. text inside apps, images, and videogames). "chiara-select2speak" is specifically developed to also enable recognizing (and reading-aloud) such text.  

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
