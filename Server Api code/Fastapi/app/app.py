from fastapi import FastAPI, status, File, Form, UploadFile, Response
from io import BytesIO
from base64 import b64encode
from PIL import Image as im
import classify_img 
import os
from fastapi import FastAPI, File, UploadFile
from fastapi import FastAPI, status, File, Form, UploadFile, Response
from fastapi.responses import JSONResponse, FileResponse
from typing import Optional
from io import BytesIO
from base64 import b64encode
import CT

import base64

import NLP.openai as audio
import NLP.TTS as speech

app=FastAPI()

@app.get('/',tags=['Root'])
async def root():
    return "Use Mobile app"

@app.post("/predict")
async def predict(  ltlg : str = Form(),file: UploadFile = File()):
    try:
    # Use BytesIO to convert the bytes object to a file-like object
        img = im.open(BytesIO(file.file.read()))
        classification_result = classify_img.classify(img.convert('RGB'))

        return {
            "predname": classification_result,
            "predconf": 0.0,
            "infTime": '',
            "maskimg": ''
        }
    except Exception as e:
         return {
            "predname": 'failed',
            "predconf": 0.0,
            "infTime": '',
            "maskimg": ''
        }
 
@app.post("/get_audio")
async def get_audio(temple_name : str = Form(),file: UploadFile = File(...)):
    print('File received:', file.filename)
    print('Temple Name: ', temple_name)
    # Ensure the upload directory exists
    os.makedirs('NLP/uploaded_files', exist_ok=True)
    try:
        # Read the file content once
        file_content = await file.read()

        # Save the uploaded audio file to the server
        with open(f"NLP/uploaded_files/{file.filename}", "wb") as audio_file:
            print('Saving File')
            audio_file.write(file_content)
            print('Saved File')
        
        # Call the question function to obtain the answe
            replace_temp=" " + temple_name + " "
        inp = audio.text(f"NLP/uploaded_files/{file.filename}").replace(' THIS ',replace_temp).replace(' THE ',replace_temp).replace(' IT ',replace_temp).replace(' THAT ',replace_temp).replace('THEY',replace_temp)
        print('REceived from whisper',inp)
        answer = CT.infer_answer(inp)
        
        print("Got answer from NLP model ")
        
        speech_answer = speech.speech(answer)
        
        print("Answer converted to speech")

        # Return the answer along with other data
        return {
            "text_from_audio": answer,
            "audio_file": b64encode(speech_answer).decode("utf-8")
            
        }
    
    except Exception as e:
        print('error: ',e)
        return {"error": str(e)}