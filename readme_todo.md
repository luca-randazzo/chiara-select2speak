# Prioritized
✓ Once this service is installed, an app shall be created among the Android apps. If clicked, it shall lead to the settings of the service.
✓ Add a direct link to the Android accessibility settings where the service must be enabled
✓ The main UI button ("Start") should be transformed into a play button, without any text
✓ Once the text2speech engine finishes readign aloud, the button shall return to its original color
✓ The main UI button should be movable by the user around the screen, to avoid covering potential areas of interest

✓ If the text2speech engine is reading, and the user clicks anywhere else on the screen than the UI buttons, the text2speech engine shall stop reading aloud

- The UI shall highlight word by word (e.g. through a yellow oval) as they're being read-aloud by the text2speech engine 

- Once the UI button is pressed, it shall transform into a pause button
-- If the user presses pause, the text2speech engine shall pause, the button shall transform into play, if the user presses on it, the text2speech engine shall restart reading from where it left off


# Backlog

## UI 
- Once the UI button is pressed, a new button shall appear: rewind
-- If the user presses rewind, the text2speech engine shall restart reading from the beginning

## OCR
- Speaking-out-loud of text around a clicked point (instead of requiring users to create a selection rectangle)
- "Intelligent" speaking-out-loud of the text "around" the user-selected area of interest (e.g. if the user did not manage to precisely select all text)

These features could help users who are not able to finely create selection rectangles or to finally select all text of interest (e.g. users with impaired fine motor control) to use the service.
The "Android Select to Speak" service implements these features.

## Language and Locale
- Select the locale of the TTS-engine
- Provide error messages and debug information to the user through the TTS-engine in the selected locale

## Goodies
- Select welcome message by TTS-engine