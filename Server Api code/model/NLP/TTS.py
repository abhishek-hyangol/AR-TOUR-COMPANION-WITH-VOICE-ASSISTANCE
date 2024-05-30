from gtts import gTTS
from io import BytesIO

def speech(text, lang='en'):
    tts = gTTS(text=text, lang=lang)
    audio_bytes = BytesIO()
    tts.write_to_fp(audio_bytes)
    audio_bytes.seek(0)  # Reset the position to the start of the BytesIO object
    return audio_bytes.read()


