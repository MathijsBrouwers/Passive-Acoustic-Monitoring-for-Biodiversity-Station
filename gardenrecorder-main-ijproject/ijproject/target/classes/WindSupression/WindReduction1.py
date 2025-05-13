import librosa
import noisereduce as nr
import soundfile as sf
import os


main_audio_path = r"path_to_noisy"
noise_audio_path = r"path_to_sample_noise"
output_folder = r"path_output_folder"

os.makedirs(output_folder, exist_ok=True)

y, sr = librosa.load(main_audio_path, sr=None)

y_noise, sr_noise = librosa.load(noise_audio_path, sr=sr)  

noise_duration_sec = 3
y_noise = y_noise[:int(sr * noise_duration_sec)]

reduced_audio = nr.reduce_noise(y=y, y_noise=y_noise, sr=sr, prop_decrease=1.0, stationary=True)

output_filename = os.path.splitext(os.path.basename(main_audio_path))[0] + "_cleaned.wav"
output_path = os.path.join(output_folder, output_filename)

sf.write(output_path, reduced_audio, sr)

print(f" Noise reduction complete. File saved to: {output_path}")
