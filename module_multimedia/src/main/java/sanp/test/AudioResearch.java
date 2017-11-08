package sanp.test;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import sanp.tools.utils.LogManager;

/**
 * Created by Tuyj on 2017/6/19.
 */

public class AudioResearch implements SimpleTesting.Tester {
    static private final boolean gToFile = true;
    static private final boolean gNeedEncode = true;
    static private final boolean gUseBuffer = true;

    static private final String gPCMFileFormat = "/sdcard/voice_%d_%d.pcm";
    static private final String gAACFileFormat = "/sdcard/aac_%d.aac";
    static private final int gAudioSource = MediaRecorder.AudioSource.MIC;
    static private final int gSampleRate = 44100;
    static private final int gChannelMode = AudioFormat.CHANNEL_IN_STEREO;
    static private final int gChannelsCnt = 2;
    static private final int gSampleWidth = AudioFormat.ENCODING_PCM_16BIT;
    static private final int gSampleSizeInByte = 2;
    static private final int gFrameDurationMs = 20;
    static private final int gSampleCntOfFrame = gSampleRate * gFrameDurationMs / 1000 * gChannelsCnt;
    static private final int gFrameSizeInByte = gSampleCntOfFrame * gSampleSizeInByte;
    static private final int gPlayStreamMode = AudioManager.STREAM_VOICE_CALL;

    static private final String gEncoderMime = MediaFormat.MIMETYPE_AUDIO_AAC;
    static private final int gAACProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    static private final int gEncodeBitrate = 64000; // 64000,96000,128000

    static private final String gPCMFile = String.format(gPCMFileFormat, gSampleRate, gChannelsCnt);
    static private final String gAACFile = String.format(gAACFileFormat, gEncodeBitrate);
    static private final long gThreadExitTimeoutMs = 1000;
    static private final int gEncodeTimeoutUs = 50000;

    public interface EncoderCallback {
        void onEncodedData(ByteBuffer data, long ptsUs, int flags);
    }

    public class AACWriter implements EncoderCallback {
        private FileOutputStream mOutFile = null;

        public AACWriter(String path) {
            try {
                mOutFile = new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                LogManager.e("Create FileOutputStream failed");
                LogManager.e(e);
                mOutFile = null;
            }
        }

        public void close() {
            if (mOutFile != null) {
                try {
                    mOutFile.close();
                } catch (IOException e) {
                   LogManager.e(e);
                }
                mOutFile = null;
            }
        }

        public void onEncodedData(ByteBuffer data, long ptsUs, int flags) {
            if (mOutFile == null)
                return;
            int rawLen = data.limit() - data.position();
            int pktLen = rawLen + 7;
            byte bData[] = new byte[pktLen];
            addADTStoPacket(bData, pktLen);
            data.get(bData, 7, rawLen);
            //LogManager.d(String.format("Write to file %d bytes", pktLen));
            try {
                mOutFile.write(bData, 0, pktLen);
            } catch (IOException e) {
               LogManager.e(e);
            }
        }

        /**
         * Add ADTS header at the beginning of each and every AAC packet.
         * This is needed as MediaCodec encoder generates a packet of raw
         * AAC data.
         * Note: the packetLen must count in the ADTS header itself.
         * Refer to https://wiki.multimedia.cx/index.php?title=ADTS
         **/
        private void addADTStoPacket(byte[] packet, int packetLen) {
            int profile = gAACProfile;  //AAC LC
            int freqIdx = 4;  //44.1KHz
            int chanCfg = 2;  //CPE

            // fill in ADTS data
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }
    }

    public class AudioAACEncoder {
        private MediaCodec mMediaCodec = null;
        private Thread mOutputThread = null;
        private boolean mRunning = false;
        private EncoderCallback mCallback = null;

        public AudioAACEncoder(EncoderCallback callback) throws IOException {
            mCallback = callback;
            mMediaCodec = MediaCodec.createEncoderByType(gEncoderMime);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, gEncoderMime);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, gSampleRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, gChannelsCnt);
            format.setInteger(MediaFormat.KEY_BIT_RATE, gEncodeBitrate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, gAACProfile);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, gFrameSizeInByte * 2);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            mOutputThread = new Thread(new Runnable() {
                public void run() {
                    outputThreadFunc();
                }
            }, "Audio Encoder OutputThread");
            mRunning = true;
            mOutputThread.start();
        }

        public void release() {
            mRunning = false;
            try {
                mOutputThread.join(gThreadExitTimeoutMs);
                //mOutputThread.interrupt();
            } catch (InterruptedException e) {
               LogManager.e(e);
            }
            mOutputThread = null;
        }

        public void push(ByteBuffer pcmFrame, long ptsUs) {
            if (mMediaCodec == null || !mRunning)
                return;

            int index = mMediaCodec.dequeueInputBuffer(gEncodeTimeoutUs);
            if (index < 0) {
                LogManager.e("get encoder input buffer failed, drop this frame");
                return;
            }
            int dataLen = pcmFrame.limit() - pcmFrame.position();
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(pcmFrame);
            inputBuffer.flip();
            //LogManager.d(String.format("Push into encoder %d bytes", inputBuffer.limit()-inputBuffer.position()));
            mMediaCodec.queueInputBuffer(index, 0, dataLen, ptsUs, 0);
        }

        private void outputThreadFunc() {
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (mRunning) {
                int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, gEncodeTimeoutUs);
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    outputBuffers = mMediaCodec.getOutputBuffers();
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    LogManager.d("encoder output format changed: " + newFormat);
                } else if (index < 0) {
                    LogManager.w("unexpected result from encoder.dequeueOutputBuffer: " + index);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = outputBuffers[index];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + index + " was null");
                    }
                    if (bufferInfo.size != 0 && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        //LogManager.d(String.format("Encoder output %d bytes with pts %d", encodedData.limit()-encodedData.position(), bufferInfo.presentationTimeUs));
                        mCallback.onEncodedData(encodedData, bufferInfo.presentationTimeUs, bufferInfo.flags);
                    }
                    mMediaCodec.releaseOutputBuffer(index, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        LogManager.w("End of Audio Encoder");
                        break;      // out of while
                    }
                }
            }
            LogManager.i("Audio Encode Output thread stopped");
            mRunning = false;
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    private AudioRecord mAudioRecord = null;
    private AudioTrack mAudioTrack = null;
    private FileOutputStream mOutFile = null;
    private AACWriter mAACWriter = null;
    private AudioAACEncoder mAudioAACEncoder = null;
    private boolean mRecording = false;
    private Thread mRecordingThread = null;

    public void start(Object obj) {
        mRecordingThread = new Thread(new Runnable() {
            public void run() {
                do {
                    int minRecSizeInByte = AudioRecord.getMinBufferSize(gSampleRate, gChannelMode, gSampleWidth);
                    if (minRecSizeInByte == AudioRecord.ERROR_BAD_VALUE) {
                        LogManager.e("AudioRecord.getMinBufferSize invalid parameter");
                        break;
                    }
                    minRecSizeInByte = minRecSizeInByte > gFrameSizeInByte ? minRecSizeInByte : gFrameSizeInByte;
                    mAudioRecord = new AudioRecord(gAudioSource, gSampleRate, gChannelMode, gSampleWidth, minRecSizeInByte);
                    if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                        LogManager.e("AudioRecord initialize failed");
                        break;
                    }

                    if (gToFile) {
                        if (gNeedEncode) {
                            mAACWriter = new AACWriter(gAACFile);
                            try {
                                mAudioAACEncoder = new AudioAACEncoder(mAACWriter);
                            } catch (IOException e) {
                                LogManager.e("AudioAACEncoder create failed");
                               LogManager.e(e);
                                break;
                            }
                        } else {
                            try {
                                mOutFile = new FileOutputStream(gPCMFile);
                            } catch (FileNotFoundException e) {
                                LogManager.e("Create FileOutputStream failed");
                               LogManager.e(e);
                                break;
                            }
                        }
                    } else {
                        int minPlaySizeInByte = AudioTrack.getMinBufferSize(gSampleRate, gChannelMode, gSampleWidth);
                        if (minPlaySizeInByte == AudioTrack.ERROR_BAD_VALUE) {
                            LogManager.e("AudioTrack.getMinBufferSize invalid parameter");
                            break;
                        }
                        minPlaySizeInByte = minRecSizeInByte > minPlaySizeInByte ? minRecSizeInByte : minPlaySizeInByte;
                        mAudioTrack = new AudioTrack(gPlayStreamMode, gSampleRate, gChannelMode, gSampleWidth, minPlaySizeInByte, AudioTrack.MODE_STREAM);
                        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                            LogManager.e("AudioTrack initialize failed");
                            break;
                        }
                        mAudioTrack.play();
                    }

                    short sFrame[] = new short[gSampleCntOfFrame];
                    byte bData[] = new byte[gFrameSizeInByte];
                    ByteBuffer bFrame = ByteBuffer.allocateDirect(gFrameSizeInByte);

                    mRecording = true;
                    mAudioRecord.startRecording();
                    LogManager.i("Start to record voice");
                    while (mRecording) {
                        if (gUseBuffer) {
                            bFrame.clear();
                            int rret = mAudioRecord.read(bFrame, gFrameSizeInByte);
                            if (rret < 0) {
                                LogManager.e("AudioRecord read error: " + rret);
                                break;
                            } else if (rret != gFrameSizeInByte) {
                                LogManager.w(String.format("AudioRecord read %d(%d)", rret, gFrameSizeInByte));
                                continue;
                            }
                            if (gToFile) {
                                if (gNeedEncode) {
                                    long ptsUs = System.nanoTime() / 1000;
                                    //LogManager.d(String.format("Record %d bytes with pts %d", bFrame.limit()-bFrame.position(), ptsUs));
                                    mAudioAACEncoder.push(bFrame, ptsUs);
                                } else {
                                    bFrame.get(bData);
                                    try {
                                        mOutFile.write(bData, 0, gFrameSizeInByte);
                                    } catch (IOException e) {
                                       LogManager.e(e);
                                    }
                                }
                            } else {
                                int wret = mAudioTrack.write(bFrame, gFrameSizeInByte, AudioTrack.WRITE_BLOCKING);
                                if (wret < 0) {
                                    LogManager.e("AudioTrack write error: " + wret);
                                    break;
                                } else if (wret != gFrameSizeInByte) {
                                    LogManager.w(String.format("AudioTrack write %d(%d)", wret, gFrameSizeInByte));
                                    continue;
                                }
                            }
                        } else {
                            int rret = mAudioRecord.read(sFrame, 0, gSampleCntOfFrame);
                            if (rret < 0) {
                                LogManager.e("AudioRecord read error: " + rret);
                                break;
                            } else if (rret != gSampleCntOfFrame) {
                                LogManager.w(String.format("AudioRecord read %d(%d)", rret, gSampleCntOfFrame));
                                continue;
                            }
                            if (gToFile) {
                                if (gNeedEncode)
                                    throw new RuntimeException("logical error");
                                try {
                                    mOutFile.write(short2byte(sFrame), 0, gFrameSizeInByte);
                                } catch (IOException e) {
                                   LogManager.e(e);
                                }
                            } else {
                                int wret = mAudioTrack.write(sFrame, 0, gSampleCntOfFrame);
                                if (wret < 0) {
                                    LogManager.e("AudioTrack write error: " + wret);
                                    break;
                                } else if (wret != gSampleCntOfFrame) {
                                    LogManager.w(String.format("AudioTrack write %d(%d)", wret, gSampleCntOfFrame));
                                    continue;
                                }
                            }
                        }
                    }
                    mRecording = false;
                } while (false);

                LogManager.i("Voice record stopped");
                if (mAudioAACEncoder != null) {
                    mAudioAACEncoder.release();
                    mAudioAACEncoder = null;
                }
                if (mOutFile != null) {
                    try {
                        mOutFile.close();
                    } catch (IOException e) {
                       LogManager.e(e);
                    }
                    mOutFile = null;
                }
                if (mAACWriter != null) {
                    mAACWriter.close();
                    mAACWriter = null;
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }, "AudioRecorder Thread");
        mRecordingThread.start();
    }

    public void next() {
        mRecording = false;
        try {
            if (mRecordingThread != null) {
                mRecordingThread.join(gThreadExitTimeoutMs);
                //mRecordingThread.interrupt();
            }
        } catch (InterruptedException e) {
           LogManager.e(e);
        }
        mRecordingThread = null;
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private byte[] byteBuffer2Array(ByteBuffer sData) {
        int size = sData.limit() - sData.position();
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i += 2) {
            bytes[i + 1] = sData.get();
            bytes[i] = sData.get();
        }
        return bytes;
    }
}
