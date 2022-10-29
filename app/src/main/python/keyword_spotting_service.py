import librosa
import tensorflow as tf
import numpy as np

# Path to the saved model
SAVED_MODEL_PATH = "model.h5"

# 1 sec. worth of sound
SAMPLES_TO_CONSIDER = 22050

class _Keyword_Spotting_Service:
    """Singleton class for keyword spotting inference with trained models.
    :param model: Trained model
    """

    model = None
    _mapping = [
        "yes",
        "zero",
        "two",
        "three",
        "six",
        "nine",
        "one",
        "no",
        "seven",
        "four",
        "eight",
        "five"
    ]
    _instance = None


    def predict(self, file_path):
        
        # extract MFCC (inputs to neural net)
        MFCCs = self.preprocess(file_path)  # (number of segments, number of co-efficients)

        # convert 2D MFCCs array into a 4D array (number of samples, number of segments, number of co-efficients = 13, number of channels = 1)
          # we need a 4-dim array to feed to the model for prediction
        MFCCs = MFCCs[np.newaxis, ..., np.newaxis]

        # get the predicted label
          # 2D array [ [scores to the different keywords, i.e., the logits] ]
        predictions = self.model.predict(MFCCs)

          # getting the logit with the highest score (probability)
        predicted_index = np.argmax(predictions)

          # map the logit to the output it represents
        predicted_keyword = self._mapping[predicted_index]

        return predicted_keyword


    def preprocess(self, file_path, num_mfcc = 13, n_fft = 2048, hop_length = 512):
        """Extract MFCCs from audio file.
        :param file_path (str): Path of audio file
        :param num_mfcc (int): # of coefficients to extract
        :param n_fft (int): Interval we consider to apply STFT. Measured in # of samples
        :param hop_length (int): Sliding window for STFT. Measured in # of samples
        :return MFCCs (ndarray): 2-dim array with MFCC data of shape (# time steps, # coefficients)
        """

        # load audio file
        signal, sample_rate = librosa.load(file_path)

        # ensure consistency in the audio file length
        if len(signal) >= SAMPLES_TO_CONSIDER:

            # resize the signal to consider only the first second (1 sec) worth of the audio file
            signal = signal[:SAMPLES_TO_CONSIDER]

            # extract MFCCs
            MFCCs = librosa.feature.mfcc(signal, sample_rate, n_mfcc = num_mfcc, n_fft = n_fft, hop_length = hop_length)

        # return the transpose fo the MFCCs
        return MFCCs.T


def Keyword_Spotting_Service():

    # ensure an instance is created only the first time the factory function is called
    if _Keyword_Spotting_Service._instance is None:

        # if we haven't created a Keyword Spotting Service instance, create it
        _Keyword_Spotting_Service._instance = _Keyword_Spotting_Service()

        # load the trained model
        _Keyword_Spotting_Service.model = tf.keras.models.load_model(SAVED_MODEL_PATH)

    return _Keyword_Spotting_Service._instance


if __name__ == "__main__":

    # create 2 instances of the keyword spotting service
    kss = Keyword_Spotting_Service()
    kss1 = Keyword_Spotting_Service()

    # check that different instances of the keyword spotting service point back to the same object (singleton)
    assert kss is kss1

    # make a prediction
    keyword = kss.predict("Mine.wav")
    print(keyword)