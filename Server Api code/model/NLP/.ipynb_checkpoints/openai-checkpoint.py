
from whisper import load_model
model = load_model("base")
def text(path):
    FILE_URL = path
    result = model.transcribe(FILE_URL)
    return result["text"].upper()