package xyz.panyi.simpleplayer;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 *
 */
public class AudioPlayerActivity extends AppCompatActivity {
    ParserTask task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        findViewById(R.id.play_btn).setOnClickListener((v)->{
            play();
        });
    }

    @Override
    protected void onDestroy() {
        if(task != null){
            task.cancel(true);
        }
        super.onDestroy();
    }

    private void play(){
        if(task != null){
            task.cancel(true);
        }

        task = new ParserTask();
        task.execute(1);
    }

    private class ParserTask extends AsyncTask<Integer , Void , Integer>{
        private MediaExtractor extractor;
        private long duration = 0;
        private MediaCodec mediaCodec;

        private CountDownLatch countDownLatch;

        private boolean isEnd = false;
        private int sampleRate;

        private AudioTrack audioTrack;

        @Override
        protected Integer doInBackground(Integer... integers) {
            extractor = new MediaExtractor();

            countDownLatch = new CountDownLatch(1);
            try {
                final AssetFileDescriptor fileDescriptor=getAssets().openFd("jiangtiandao.mp3");
                extractor.setDataSource(fileDescriptor.getFileDescriptor() , fileDescriptor.getStartOffset() , fileDescriptor.getLength());

                int numTracks = extractor.getTrackCount();
                MediaFormat format = null;
                String mineType = null;
                for (int i = 0; i < numTracks; i++){
                    format = extractor.getTrackFormat(i);
                    mineType =format.getString(MediaFormat.KEY_MIME);
                    System.out.println("mineType = " + mineType);

                    if(!TextUtils.isEmpty(mineType) && mineType.startsWith("audio")){
                        extractor.selectTrack(i);
                        long value = format.getLong(MediaFormat.KEY_DURATION);
                        duration = value / 1000;
                        System.out.println("时长 = " + duration);

                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        System.out.println("sampleRate = " + sampleRate);
                    }
                }//end for i

                mediaCodec = MediaCodec.createDecoderByType(mineType);

                int buffsize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC , sampleRate , AudioFormat.CHANNEL_OUT_STEREO , AudioFormat.ENCODING_PCM_16BIT , buffsize , AudioTrack.MODE_STREAM);
                audioTrack.play();

                mediaCodec.setCallback(new MediaCodec.Callback(){
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                        if(isEnd)
                            return;

                        final ByteBuffer inputBuf = codec.getInputBuffer(index);
                        int sampleSize = extractor.readSampleData(inputBuf, 0);
                        long timestampTemp = extractor.getSampleTime();

                        System.out.println("input sampleSize = "  + sampleSize);
                        if (sampleSize <= 0){
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEnd = true;
                            countDownLatch.countDown();
                        }else{
                            codec.queueInputBuffer(index , 0 , sampleSize , timestampTemp , 0);
                        }
                        extractor.advance();
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                        if(isEnd)
                            return;

                        final ByteBuffer outputBuf =codec.getOutputBuffer(index);
                        System.out.println("outData size = "  + info.size+"   index = " + index);

                        byte[] outData = new byte[info.size];
                        outputBuf.get(outData , 0 , info.size);
                        codec.releaseOutputBuffer(index, true);
                        //audioPlayer.play(outData , 0 , outData.length);
                        audioTrack.write(outData , info.offset , info.offset + info.size);
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        System.out.println("onError ==> " + e.toString());
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        System.out.println("onOutputFormatChanged ==> " + format.getString(MediaFormat.KEY_MIME));
                    }
                });

                mediaCodec.configure(format , null , null , 0);
                mediaCodec.start();

                //mediaCodec.wait(100 * 1000);
                countDownLatch.await();
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }  finally {
                releaseMedia();
            }
            return 1;
        }

        private void releaseMedia(){
            if(mediaCodec != null){
                mediaCodec.stop();
                mediaCodec.release();
            }

            if(extractor != null){
                extractor.release();
            }

            if(audioTrack != null){
                audioTrack.release();
            }

            System.out.println("end parse file ...");
        }

        @Override
        protected void onCancelled() {
            isEnd = true;
            releaseMedia();
        }
    }
}//end class
