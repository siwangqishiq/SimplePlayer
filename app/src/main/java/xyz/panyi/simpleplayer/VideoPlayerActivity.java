package xyz.panyi.simpleplayer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 *   https://bigflake.com/mediacodec/#overview
 *
 *
 *
 *
 */
public class VideoPlayerActivity extends AppCompatActivity {
    boolean isRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);

        findViewById(R.id.player_btn).setOnClickListener((v)->{
            if(isRun)
                return;

            isRun = true;
            new DecoderThread().start();
        });
    }

    class DecoderThread extends Thread{
        @Override
        public void run() {


        }
    }
}//end class
