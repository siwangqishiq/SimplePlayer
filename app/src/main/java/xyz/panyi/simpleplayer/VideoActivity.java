package xyz.panyi.simpleplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *   https://bigflake.com/mediacodec/#overview
 *
 *
 *
 *
 */
public class VideoActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);

        new DecoderThread().start();
    }

    final class DecoderThread extends Thread{
        @Override
        public void run() {
            AssetFileDescriptor afd =getResources().openRawResourceFd(R.raw.clipcanvas_14348_h264_640x360);

            MediaExtractor mediaExtractor = new MediaExtractor();
            try {
                mediaExtractor.setDataSource(afd.getFileDescriptor() , afd.getStartOffset(), afd.getDeclaredLength());

                int numTracks = mediaExtractor.getTrackCount();
                MediaFormat format = null;
                String mineType = null;
                for (int i = 0; i < numTracks; i++){
                    format = mediaExtractor.getTrackFormat(i);
                    mineType =format.getString(MediaFormat.KEY_MIME);
                    System.out.println("mineType = " + mineType);

                    if (mineType.startsWith("video/")) {
                        // Must select the track we are going to get data by readSampleData()
                        mediaExtractor.selectTrack(i);
                        // Set required key for MediaCodec in decoder mode
                        // Check http://developer.android.com/reference/android/media/MediaFormat.html
                        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 24);
                        format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);
                        break;
                    }
                }//end for i

                MediaCodec mediaDecoder = MediaCodec.createDecoderByType(mineType);
                mediaDecoder.configure(format , mSurfaceView.getHolder().getSurface() ,null , 0 );
                mediaDecoder.start();

                int timeoutUs = 1_000_000; // 1 second timeout
                boolean eos = false;
                long playStartTime = System.currentTimeMillis();
                long frameDisplayTime = playStartTime;

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                while(!eos){
                    int inputBufferIndex = mediaDecoder.dequeueInputBuffer(timeoutUs);
                    if (inputBufferIndex >= 0){
                        ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(inputBufferIndex);
                        int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);

                        if (sampleSize > 0) {
                            frameDisplayTime = (mediaExtractor.getSampleTime() >> 10) + playStartTime;

                            mediaDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                        }else{
                            mediaDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            eos = true;
                        }
                    }

                    int outputBufferIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
                    if (outputBufferIndex >= 0){
                        // Frame rate control
                        while(frameDisplayTime > System.currentTimeMillis()) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }//end while

                        // outputBuffer is ready to be processed or rendered.
                        mediaDecoder.releaseOutputBuffer(outputBufferIndex, true /*true:render to surface*/);
                    }

                }//end while
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}//end class
