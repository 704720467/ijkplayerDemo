package com.zp.libvideoedit.EditCore;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.modle.AudioSegment;
import com.zp.libvideoedit.modle.MediaComposition;
import com.zp.libvideoedit.modle.MediaTrack;
import com.zp.libvideoedit.modle.MediaType;
import com.zp.libvideoedit.modle.TrackType;
import com.zp.libvideoedit.modle.VideoTimer;
import com.zp.libvideoedit.utils.Resample;

import java.util.ArrayList;

import static com.zp.libvideoedit.Constants.DEFAULT_AUDIO_CHANNEL_CONFIG;
import static com.zp.libvideoedit.Constants.DEFAULT_AUDIO_SAMPLE_RATE;
import static com.zp.libvideoedit.Constants.TAG;
import static com.zp.libvideoedit.Constants.TAG_A;
import static com.zp.libvideoedit.Constants.TAG_EN;
import static com.zp.libvideoedit.Constants.VERBOSE;
import static com.zp.libvideoedit.Constants.VERBOSE_A;
import static com.zp.libvideoedit.Constants.VERBOSE_LOOP_A;


/**
 * Created by gwd on 2018/3/13.
 */

public class AudioPlayerCoreManager implements AudioTrackDecoderThread.CallBack {
    private ArrayList<MediaTrack> mediaTracks;
    private VideoTimer timer;
    private AudioTrack audioTrackPlayer;
    private boolean running = false;
    private MediaTrack<AudioSegment> mainAudioTrack;
    private MediaTrack<AudioSegment> bgmAudioTrack;
    private MediaTrack<AudioSegment> recAudioTrack;
    private AudioTrackDecoderThread audioTrackDecoderThread;
    private AudioTrackDecoderThread backGroundAudioTrackDecoderThread;
    private AudioTrackDecoderThread recAudioTrackDecoderThread;
    private boolean playerCreated = false;
    private Resample audioResample;
    private int currentSampleRate;
    private int currentChannels;
    private Object audioResimpleObj = new Object();
    private MediaComposition mediaComposition;
    private AudioMixer audioMixer;
    private boolean beQuiet = false;
    private int sampleRatePer = DEFAULT_AUDIO_SAMPLE_RATE;

    public AudioPlayerCoreManager(VideoTimer timer) {
        this.mediaTracks = mediaTracks;
        this.timer = timer;
        audioResample = new Resample(DEFAULT_AUDIO_SAMPLE_RATE);
        audioMixer = new AudioMixer();
    }

    public void setMediaComposition(MediaComposition mediaComposition, VideoTimer timer) {
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_setMediaComposition:" + mediaComposition);
        this.mediaComposition = mediaComposition;
        this.timer = timer;
        this.mediaTracks = mediaComposition.trackOfType(MediaType.MEDIA_TYPE_Audio);
        mainAudioTrack = null;
        bgmAudioTrack = null;
        recAudioTrack = null;
        for (int i = 0; i < mediaTracks.size(); i++) {
            MediaTrack track = mediaTracks.get(i);
            if (track.getTrackType() == TrackType.TrackType_Main_Audio) {
                mainAudioTrack = track;
                audioMixer.onSegEmpty(TrackType.TrackType_Main_Audio, false);

            }
            if (track.getTrackType() == TrackType.TrackType_Audio_BackGround) {
                bgmAudioTrack = track;
                audioMixer.onSegEmpty(TrackType.TrackType_Audio_BackGround, false);
            }
            if (track.getTrackType() == TrackType.TrackType_Audio_Recoder) {
                recAudioTrack = track;
                audioMixer.onSegEmpty(TrackType.TrackType_Audio_Recoder, false);
            }
        }
        createAndPlaye();
        seekTo(CMTime.zeroTime());
        if (VERBOSE)
            Log.d(TAG, "AudioPlayerCoreManager_setMediaComposition oooook:" + mediaComposition);

    }

    private void createAndPlaye() {
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_createAndPlaye..");
        running = true;
        safeCreateTrackPlayer();
        relaseDecoderThread(audioTrackDecoderThread);
        relaseDecoderThread(backGroundAudioTrackDecoderThread);
        relaseDecoderThread(recAudioTrackDecoderThread);
        if (mainAudioTrack != null) {
            audioTrackDecoderThread = new AudioTrackDecoderThread(mainAudioTrack, this.timer, audioMixer, this);
            audioTrackDecoderThread.start();
        }
        if (bgmAudioTrack != null) {
            backGroundAudioTrackDecoderThread = new AudioTrackDecoderThread(bgmAudioTrack, this.timer, audioMixer, this);
            backGroundAudioTrackDecoderThread.start();
        }
        if (recAudioTrack != null) {
            recAudioTrackDecoderThread = new AudioTrackDecoderThread(recAudioTrack, this.timer, audioMixer, this);
            recAudioTrackDecoderThread.start();
        }


        playerCreated = true;
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_createAndPlaye oooook");
    }

    private void safeCreateTrackPlayer() {
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_safeCreateTrackPlayer..");
        relaseAudioTrackPlayer();
        audioTrackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_AUDIO_SAMPLE_RATE,
                DEFAULT_AUDIO_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(DEFAULT_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT) * 2,
                AudioTrack.MODE_STREAM);
        if (audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED)
            audioTrackPlayer.play();
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_safeCreateTrackPlayer oooook");
    }

    public void start() {
        if (VERBOSE)
            Log.i(TAG, "AudioPlayerCoreManager_start..");
        if (audioTrackDecoderThread != null) {
            audioTrackDecoderThread.setBeQuiet(beQuiet);
            audioTrackDecoderThread.resumeDecode();
        }
        if (backGroundAudioTrackDecoderThread != null) {
            backGroundAudioTrackDecoderThread.setBeQuiet(beQuiet);
            backGroundAudioTrackDecoderThread.resumeDecode();
        }
        if (recAudioTrackDecoderThread != null) {
            recAudioTrackDecoderThread.setBeQuiet(beQuiet);
            recAudioTrackDecoderThread.resumeDecode();
        }
        startAudioTrackPlayer();
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_start oooook");
    }

    public void stop() {
        if (VERBOSE)
            Log.i(TAG, "AudioPlayerCoreManager_stop..");
        stopAndRelease();
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_stop oooook");
    }

    public void resume() {
        if (VERBOSE)
            Log.i(TAG, "AudioPlayerCoreManager_resume..");
        if (!playerCreated) {
            createAndPlaye();
        } else {
            if (audioTrackDecoderThread != null) audioTrackDecoderThread.resumeDecode();
            if (backGroundAudioTrackDecoderThread != null)
                backGroundAudioTrackDecoderThread.resumeDecode();
            if (recAudioTrackDecoderThread != null) {
                recAudioTrackDecoderThread.resumeDecode();
            }
            audioTrackPlayer.play();
        }

        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_resume oooook");

    }

    public void pause() {
        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_pause..");

        if (backGroundAudioTrackDecoderThread != null) backGroundAudioTrackDecoderThread.pause();
        if (audioTrackDecoderThread != null) audioTrackDecoderThread.pause();
        if (recAudioTrackDecoderThread != null) recAudioTrackDecoderThread.pause();
        if (audioTrackPlayer != null) audioTrackPlayer.pause();
        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_pause oooook");
    }

    public void seekTo(CMTime time) {
        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_seekTo:" + time.getSecond());
        if (audioTrackDecoderThread != null) {
            audioTrackDecoderThread.seek(time.getUs());
        }
        if (backGroundAudioTrackDecoderThread != null) {
            backGroundAudioTrackDecoderThread.seek(time.getUs());
        }
        if (recAudioTrackDecoderThread != null) {
            recAudioTrackDecoderThread.seek(time.getUs());
        }
        if (audioMixer != null) audioMixer.clear();
        stopAudioTrackPlayer();
        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_seekTo ok!!:" + time.getSecond());

    }


    public void setVolume(float mainVolume, float bgmVolume, float recVolume) {
        if (VERBOSE)
            Log.i(TAG, "AudioPlayerCoreManager_setVolume.." + mainVolume + "," + bgmVolume + "," + recVolume);
        audioMixer.setVolume(mainVolume, bgmVolume, recVolume);
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_setVolume oooook");
    }


    private synchronized void stopAndRelease() {
        if (VERBOSE)
            Log.i(TAG_A, "AudioPlayerTrackManager stopAndRelease audio stop");

        relaseDecoderThread(backGroundAudioTrackDecoderThread);
        relaseDecoderThread(audioTrackDecoderThread);
        relaseDecoderThread(recAudioTrackDecoderThread);
        relaseAudioTrackPlayer();

        playerCreated = false;
        if (VERBOSE) Log.i(TAG, "AudioPlayerCoreManager_stopAndRelease oooook");
    }

    private void relaseDecoderThread(AudioTrackDecoderThread decoderThread) {
        String trackName = "EMPTY_TRACK";
        if (decoderThread != null && decoderThread.getMediaTrack() != null && decoderThread.getMediaTrack().getTrackType() != null)
            trackName = decoderThread.getMediaTrack().getTrackType().getName();
        if (VERBOSE)
            Log.d(TAG, "AudioPlayerCoreManager_relaseDecoderThread |" + trackName + "| ..");
        try {
            if (decoderThread != null) {
                decoderThread.stopDecode();
                if (decoderThread == audioTrackDecoderThread) {
                    audioTrackDecoderThread = null;
                }
                if (decoderThread == backGroundAudioTrackDecoderThread) {
                    backGroundAudioTrackDecoderThread = null;
                }
                if (decoderThread == recAudioTrackDecoderThread) {
                    recAudioTrackDecoderThread = null;
                }
            }
        } catch (Exception e) {
            Log.w(TAG_A, "AudioPlayerCoreManager_relaseDecoderThread error by release  |" + trackName + "|" + decoderThread.getName() + ", " + e.getMessage(), e);
        }
        if (VERBOSE)
            Log.d(TAG, "AudioPlayerCoreManager_relaseDecoderThread |" + trackName + "|  oooook");
    }

    private void relaseAudioTrackPlayer() {
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_relaseAudioTrackPlayer..");
        try {
            if (audioTrackPlayer != null) {
                audioTrackPlayer.stop();
                audioTrackPlayer.release();
                audioTrackPlayer = null;
            }
        } catch (Exception e) {
            Log.w(TAG_A, "AudioPlayerCoreManager_relaseAudioTrackPlayer error by release" + e.getMessage(), e);
        }
    }

    @Override
    public void onAudioFormatChanged(AudioTrackDecoderThread decoderThread, int sampleRate, int channelCount) {
//        currentChannels = playerChannelCount;
//        currentSampleRate = sampleRate;
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, "AudioDecoderCallBack_onAudioFormatChanged sampleRate：" + sampleRate + ", channelCount:" + channelCount);

    }

    @Override
    public void onAudioDecoderFinish(AudioTrackDecoderThread decoderThread, MediaTrack mediaTrack) {
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, "AudioDecoderCallBack_onAudioDecoderFinish_" + mediaTrack.getTrackType().getName() + ", mediaTrack:" + mediaTrack);
        audioMixer.onSegEmpty(mediaTrack.getTrackType(), true);
    }

    @Override
    //TODO TIME resample
    public void onFrameArrive(AudioTrackDecoderThread decoderThread, AudioSegment segment, short[] audioBuffer, int sampleRate, int channelCount, long pts) {
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, "AudioDecoderCallBack_onFrameArrive_" + decoderThread.getMediaTrack().getTrackType().getName() + "_ bufferSize:" + (audioBuffer != null ? audioBuffer.length : "-1") + ",sampleRate:" + sampleRate + ",channelCount:" + channelCount + ",pts:" + String.format("%,d", pts) + ",reduceSize:" + (segment != null ? segment.getScale() : 0) + ", volume:" + (segment != null ? segment.getVolume() : 0));
        float volume = -1;
        if (segment != null) {
            sampleRatePer = (int) Math.round(sampleRate / segment.getScale());
            volume = segment.getVolume();
        }
        volume = beQuiet ? 0 : volume;
        sampleRatePer = sampleRate;
        // resample后，pcm数据为 双声道，44100的标准数据.
        short[] outBuffer = audioResample.resampleAudio(audioBuffer, channelCount, sampleRatePer);
        while (!audioMixer.canAddData(decoderThread.getMediaTrack().getTrackType(), outBuffer)) {
            try {
                Thread.sleep(10);
                if (VERBOSE_LOOP_A)
                    Log.d(TAG_A, "finished_AudioDecoderCallBack_onFrameArrive: Sleep 10ms");

            } catch (InterruptedException e) {
                Log.w(TAG_EN, "onFrameArrive sleep error:" + e.getMessage());
            }
        }
//        short[] outBuffer = audioBuffer;
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, "AudioDecoderCallBack_onFrameArrive_" + decoderThread.getMediaTrack().getTrackType().getName() + "_resampleAudio outBuffer:" + (outBuffer == audioBuffer ? "need_not_resample" : "") + (outBuffer != null ? outBuffer.length : "-1") + ",pts:" + String.format("%,d", pts));
        short[] mixedBuffer = audioMixer.onPCMArrived(decoderThread.getMediaTrack().getTrackType(), outBuffer, volume);
        if (mixedBuffer == null || mixedBuffer.length == 0) {
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, "finished_AudioDecoderCallBack_onFrameArrive_" + decoderThread.getMediaTrack().getTrackType().getName() + "_mixedBufferIS_NULL_ bufferSize:" + (audioBuffer != null ? audioBuffer.length : "-1") + ",channelCount:" + channelCount + ",pts:" + String.format("%,d", pts));
            return;
        }
        int pos = 0;
        while (mixedBuffer.length - pos > 0) {
            int writingSize = mixedBuffer.length - pos;
//            startAudioTrackPlayer();
            int writeResult = audioTrackPlayer.write(mixedBuffer, pos, writingSize);
            if (VERBOSE_LOOP_A)
                Log.d(TAG_A, "AudioDecoderCallBack_onFrameArrive_writeTo_AudioTrack" + decoderThread.getMediaTrack().getTrackType().getName() + ", writingSize:" + writingSize + ",  writeResult:" + writeResult);
            pos += writeResult;
            if (writeResult == 0) break;
        }
        if (VERBOSE_LOOP_A)
            Log.d(TAG_A, "finished_AudioDecoderCallBack_onFrameArrive_" + decoderThread.getMediaTrack().getTrackType().getName() + "_ mixedBuffer:" + (mixedBuffer != null ? mixedBuffer.length : 0) + ", bufferSize:" + (audioBuffer != null ? audioBuffer.length : "-1") + ",channelCount:" + channelCount + ",pts:" + String.format("%,d", pts));
    }

    public void release() {
        stopAndRelease();
        if (VERBOSE) Log.d(TAG, "AudioPlayerCoreManager_release..");
    }

    public boolean isBeQuiet() {
        return beQuiet;
    }

    public void setBeQuiet(boolean beQuiet) {
        this.beQuiet = beQuiet;
    }

    /**
     * 开始启动播放
     */
    private void startAudioTrackPlayer() {
//        if (audioTrackPlayer != null) {
//            if (audioTrackPlayer.getState() != AudioTrack.PLAYSTATE_PLAYING
//                    && audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED) {
//                audioTrackPlayer.play();
//                int ii = audioTrackPlayer.getState();
//                boolean bb = ii != AudioTrack.PLAYSTATE_PLAYING;
//            } else {
//                safeCreateTrackPlayer();
//            }
//        }
        if (audioTrackPlayer != null && audioTrackPlayer.getState() == AudioTrack.STATE_INITIALIZED)
            audioTrackPlayer.play();
    }

    /**
     * 暂停音频播放
     */
    private void stopAudioTrackPlayer() {
        try {
            if (audioTrackPlayer != null && audioTrackPlayer.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrackPlayer.stop();
                audioTrackPlayer.flush();
            }
        } catch (Exception e) {
            if (VERBOSE_A)
                Log.d(TAG_A, "AudioPlayerCoreManager_stopAudioTrackPlayer_" + e.getMessage());
            safeCreateTrackPlayer();
        }
    }
}
