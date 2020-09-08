package xyz.panyi.simpleplayer;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import me.rosuh.filepicker.config.FilePickerManager;

/**
 * https://bigflake.com/mediacodec/#overview
 */
public class VideoPlayerActivity extends AppCompatActivity {
    boolean isRun = false;

    private MyGLSurfaceView mGLSurfaceView;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video2);

        findViewById(R.id.select_file_btn).setOnClickListener((v)->{
            selectFile();
        });

        mGLSurfaceView = findViewById(R.id.surfaceView);
        //mSurfaceView = findViewById(R.id.surfaceView2);

        findViewById(R.id.player_btn).setOnClickListener((v) -> {
            isRun = false;
        });
    }

    private void playVideo(final String file){
        if (isRun)
            return;

        isRun = true;
        new DecoderThread(file).start();
    }

    @Override
    protected void onDestroy() {
        isRun = false;
        super.onDestroy();
    }

    private void selectFile(){
        FilePickerManager.INSTANCE.from(this).forResult(FilePickerManager.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FilePickerManager.REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> paths = FilePickerManager.INSTANCE.obtainData();
            if (paths != null && paths.size() > 0) {
                final String filePath = paths.get(0);
                System.out.println("filepath = " + filePath);

                Toast.makeText(this , filePath  ,Toast.LENGTH_LONG).show();

                playVideo(filePath);
            }
        }
    }



    class DecoderThread extends Thread {
        final String filepath;

        public DecoderThread(String path){
            filepath = path;
        }

        @Override
        public void run() {
            MediaExtractor mediaExtractor = new MediaExtractor();
            try {
                //final AssetFileDescriptor fileDescriptor = getAssets().openFd("gakki.mp4");
                //FileDescriptor fileDesp  = new FileDescriptor();

                File file = new File(filepath);
                FileInputStream inputStream = new FileInputStream(file);
                mediaExtractor.setDataSource(inputStream.getFD());

                int numTracks = mediaExtractor.getTrackCount();
                MediaFormat format = null;
                String mineType = null;
                for (int i = 0; i < numTracks; i++) {
                    format = mediaExtractor.getTrackFormat(i);
                    mineType = format.getString(MediaFormat.KEY_MIME);
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
                mediaDecoder.configure(format , mGLSurfaceView.getSurface() ,null , 0 );
                //mediaDecoder.configure(format, mSurfaceView.getHolder().getSurface(), null, 0);
                mediaDecoder.start();

                int timeoutUs = 1_000_000; // 1 second timeout
                boolean eos = false;
                long playStartTime = System.currentTimeMillis();
                long frameDisplayTime = playStartTime;

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                while (!eos && isRun) {
                    int inputBufferIndex = mediaDecoder.dequeueInputBuffer(timeoutUs);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(inputBufferIndex);
                        int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);

                        if (sampleSize > 0) {
                            frameDisplayTime = (mediaExtractor.getSampleTime() >> 10) + playStartTime;

                            mediaDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                        } else {
                            mediaDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            eos = true;
                        }
                    }

                    int outputBufferIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, timeoutUs);

                    if (outputBufferIndex >= 0) {
                        // Frame rate control
                        while (frameDisplayTime > System.currentTimeMillis()) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }//end while

                        // outputBuffer is ready to be processed or rendered.
                        System.out.println("outputBufferIndex  = " + outputBufferIndex);
                        mediaDecoder.releaseOutputBuffer(outputBufferIndex, true);
                    }
                }//end while

                mediaExtractor.release();
                mediaDecoder.release();
                isRun = false;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}//end class
