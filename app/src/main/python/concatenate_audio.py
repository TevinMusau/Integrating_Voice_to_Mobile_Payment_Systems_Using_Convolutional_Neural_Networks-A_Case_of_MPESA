from pydub import AudioSegment
import os

def concatenate_audio(path, first_timestamp, second_timestamp):
    # get the audios
    audio_1 = path + "/final_record" + str(first_timestamp)+".wav"
    audio_2 = path + "/final_record" + str(second_timestamp)+".wav"
    
    first_audio = AudioSegment.from_wav(audio_1)
    second_audio = AudioSegment.from_wav(audio_2)
    
    # check if they exist
    if ((os.path.isfile(audio_1)) and (os.path.isfile(audio_2))):
        # combine the audios
        combined_audios = first_audio + second_audio
        
        # export the audios
        combined_audios.export("/storage/emulated/0/MVoice/Voice Prints Split/concatenation"+str(second_timestamp)+".wav", format="wav")
        
        return True
    else:
        return False