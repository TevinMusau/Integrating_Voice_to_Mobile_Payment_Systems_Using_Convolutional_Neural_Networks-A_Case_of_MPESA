from flask import Flask, request
from keyword_spotting_service import Keyword_Spotting_Service
# from waitress import serve
import random
import os

app = Flask(__name__)

@app.route("/predict", methods=['POST'])
def predict():
    # get audio file and save it
    audio_file = request.files["file"]    
    file_name = str(random.randint(0, 10000))
    audio_file.save(file_name)

    # invoke key word spotting service
    kss = Keyword_Spotting_Service()

    # make a prediction
    words = []
    predicted_keyword = kss.predict(file_name)
    words.append(predicted_keyword)
    
    for i in range(len(words)):
        with open("result.txt", "a") as fp:
            fp.write("\n"+words[i]+"relates to: "+file_name)
    
    # remove the audio file
    os.remove(file_name)

    # send back the predicted keywords
    return predicted_keyword 

if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)
    # serve(app, host='0.0.0.0', port=5000)