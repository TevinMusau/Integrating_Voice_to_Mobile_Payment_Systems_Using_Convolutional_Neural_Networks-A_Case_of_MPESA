from flask import Flask, request
from keyword_spotting_service import Keyword_Spotting_Service
from pyannote.audio import Pipeline

import speech_recognition as sr
# from waitress import serve
import os
import re


app = Flask(__name__)
predictions = []
audios = {}
temps = {}


# -------------------------------- KEY WORD SPOTTING MODEL (START) --------------------------------

@app.route("/predict", methods=['POST'])
def predict():
    # get the audio file
    audio_file = request.files["file"]
    print("Received: ", audio_file.filename)
    
    # Check if audio filename is empty
    if audio_file.filename != "":
        
        # check if audio file name is the first one 
        if audio_file.filename != "1yeboo":
            
            # split the integer from the string
            temp = re.compile("([0-9]+)([a-zA-Z]+)")
            res = temp.match(audio_file.filename).groups()
            
            # name the file with it's embedded integer
            file_name = str(res[0])
            
            # save the file
            audio_file.save(file_name)
            
            # initialize the Key Word Spotting Service
            kss = Keyword_Spotting_Service()
            
            # get predicted keyword
            predicted_keyword = kss.predict(file_name)
            
            # store the predicted keyword in a dictionary with it's embedded integer as the key
            temps = {
                res[0] : predicted_keyword
            }
            
            # remove the audio file
            os.remove(file_name)
            
        else:
            # name the file with it's embedded integer, i.e., which is 1
            file_name = str(1)
            
            # save the file
            audio_file.save(file_name)
            
            # initialize the Key Word Spotting Service
            kss = Keyword_Spotting_Service()
            
            # get predicted keyword
            predicted_keyword = kss.predict(file_name)
            
            # store the predicted keyword in a dictionary with it's embedded integer as the key
            temps = {
                1 : predicted_keyword
            }
            
            # remove the audio file
            os.remove(file_name)
            
    # sort the dictionary of predicted keywords
    temps = dict(sorted(temps.items()))
    print(temps)
    
    # get the keys
    key_list = list(temps.keys())
    
    # get the values
    val_list = list(temps.values())
    
    # append the predictions to a list
    for i in val_list:
        position = val_list.index(i)
        predictions.append(str(key_list[position])+i)
    
    # sort the list of predictions in ascending order
    predictions.sort()
    print(predictions)
    
    # Join the predictions to convert the array to string
    preds = ' '.join(predictions)
    
    # write the string to a file
    with open("result.txt", "a") as fp:
        fp.write(str(preds))
    
    # clear the predictions list (to avoid duplicates)
    predictions.clear()
    
    # return the string of predictions
    return preds

# -------------------------------- KEY WORD SPOTTING MODEL (END) --------------------------------
       
    
# --------------------------------  SPEECH RECOGNIZER (START) -----------------------------------
@app.route("/recognise_name", methods=['POST'])
def name_recognize():
    # get the audio file
    audio_file = request.files["file"]
    
    # name the audio file
    file_name = "nameOuser"
    
    # save the audio file
    audio_file.save(file_name)
    
    # start an instance of speech recognizer
    r = sr.Recognizer() 
    
    # record audio file
    rec_speech = sr.AudioFile(file_name)
    with rec_speech as source:
        audio = r.record(source)
    
    
    speech_recognised_name = r.recognize_google(audio)
    print(speech_recognised_name)
    
    with open("names.txt", "a") as fp:
        fp.write(str(speech_recognised_name))
    
    return speech_recognised_name
            
# -------------------------------- SPEECH RECOGNIZER (END) ---------------------------------------


# -------------------------------- SPEAKER VERIFICATION (START) ----------------------------------
@app.route("/verify_speaker", methods=['POST'])
def verify_speaker():
    # get the audio file
    audio_file = request.files["file"]
    
    # list of speakers
    speakers = []
    
    # name the audio file
    file_name = "speakerORspeakers"
    
    # save the file
    audio_file.save(file_name)
    
    # instantiate the pre-trained speaker segementation model
    pipeline = Pipeline.from_pretrained("pyannote/speaker-segmentation")
    
    # get the output from the pretrained model
    output = pipeline(file_name)
    
    # this would check at what segments the speaker is speaking
    for turn, _, speaker in output.itertracks(yield_label=True):
        speakers.append(speaker)
        # print(f"start={turn.start}s stop={turn.end}s speaker_{speaker}")
    
    if (len(speakers) > 1):
        return "Different Speakers"
    else:
        return "Same Speakers"
 
# -------------------------------- SPEAKER VERIFICATION (END) ------------------------------------ 

if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True, port=5000)
    # serve(app, host='0.0.0.0', port=5000)