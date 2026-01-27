package cc.tomko.outify.playback;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {
    private AudioTrack track;
    private int currentSampleRate = -1;
    private int currentChannels = -1;
    private AudioFormat currentFormat = null;

    public synchronized void onPCM(byte[] data, int sampleRate, int channels, AudioFormat format) {
        if(!ensureAudioTrack(sampleRate, channels, format)){
            return; // unsupported format
        }

        writePcm(data, format);
    }

    private boolean ensureAudioTrack(int sampleRate, int channels, AudioFormat format){
        if(track != null && sampleRate == currentSampleRate && channels == currentChannels && format == currentFormat){
            return true;
        }

        releaseAudioTrack();

        int channelMask = (channels == 1) ? android.media.AudioFormat.CHANNEL_OUT_MONO
                : android.media.AudioFormat.CHANNEL_OUT_STEREO;

        int encoding;
        switch (format) {
            case S16:
                encoding = android.media.AudioFormat.ENCODING_PCM_16BIT;
                break;
            default:
                Log.e("AudioPlayer", "Unsupported AudioFormat: " + format);
                return false;
        }

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding);
        if(minBufferSize <= 0){
            Log.e("AudioPlayer", "Invalid buffer size");
            return false;
        }

        // TODO: Rework to new method
        track = new AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, channelMask, encoding, minBufferSize * 2, AudioTrack.MODE_STREAM
        );

        track.play();

        currentSampleRate = sampleRate;
        currentChannels = channels;
        currentFormat = format;

        return true;
    }

    private void writePcm(byte[] data, AudioFormat format){
        // TODO: Implement more formats
        switch (format) {
            case S16:
                track.write(data, 0, data.length);
                break;
        }
    }

    public synchronized void releaseAudioTrack(){
        if(track == null) return;
        try {
            track.stop();
        } catch (IllegalStateException ignored) {
        }

        track.release();
        track = null;
    }
}
