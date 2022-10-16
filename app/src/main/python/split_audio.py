import mutagen
from mutagen.wave import WAVE
from pydub import AudioSegment
import os

def audio_duration(length):
    hours = length // 3600  # calculate in hours
    length %= 3600
    mins = length // 60  # calculate in minutes
    length %= 60
    seconds = length  # calculate in seconds
  
    return hours, mins, seconds  # returns the duration

def split_audio(path, current_time):
  
  my_audio = path + "/final_record"+str(current_time)+".wav"

  if os.path.isfile(my_audio):
    # Create a WAVE object
    audio = WAVE(my_audio)
    
    # contains all the metadata about the wavpack file
    audio_info = audio.info
    
    
    length = int(audio_info.length)
    hours, mins, seconds = audio_duration(length)
    
    t1 = 0 # Works in milliseconds
    t2 = 1000
    
    for i in range(seconds):
      i = i + 1
      newAudio = AudioSegment.from_wav(my_audio)
      newAudio = newAudio[t1:t2]
      
      newAudio.export("/storage/emulated/0/MVoice/Voice Prints Split/"+str(current_time)+"/"+f"yeboo{[i]}"+str(current_time)+".wav", format="wav")
      if i is 1:
        t1 = (t1 + i)
        t1 = t1 * 1000
        t2 = t2 + 1000
      else:
        t1 = t1 + 1000
        t2 = t2 + 1000
    return True
  else:
    return False