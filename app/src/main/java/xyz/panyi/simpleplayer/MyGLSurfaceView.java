package xyz.panyi.simpleplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31Ext;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer , SurfaceTexture.OnFrameAvailableListener{
    private int textureID;
    private int renderVideoProgramId;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private int mUniformMatLoc;
    private int mUvUniformMatLoc;

    private float vertexData[] = {
            -1.0f , 1.0f ,
//            -0.5f , 0.5f,
            1.0f , 1.0f ,
            1.0f , -1.0f,
            -1.0f , -1.0f
    };
    private FloatBuffer vertexBuf;

    private float textureCoordData[] = {
            0.0f , 1.0f ,
            1.0f , 1.0f ,
            1.0f , 0.0f,
            0.0f , 0.0f
    };
    private FloatBuffer textureCoordBuf;

//     -1.0f, -1.0f, 0, 0.f, 0.f,
//             1.0f, -1.0f, 0, 1.f, 0.f,
//             -1.0f,  1.0f, 0, 0.f, 1.f,
//             1.0f,  1.0f, 0, 1.f, 1.f,

    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    //private boolean updateSurface = false;

    int _updateTexImageCompare = 0;
    int _updateTexImageCounter = 0;

    public Surface getSurface(){
        return surface;
    }

    public MyGLSurfaceView(Context context) {
        super(context);
        initView();
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    private void initView(){
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        ShaderUtil.ctx = getContext();

        textureID = createOesTexture();
        renderVideoProgramId = ShaderUtil.buildShaderProgram(R.raw.render_video_vs , R.raw.render_video_frag);
        if(renderVideoProgramId >= 0){
            System.out.println("编译成功 programId = " + renderVideoProgramId);
        }else{
            System.out.println("编译错误!!! = " + renderVideoProgramId);
        }


        mUniformMatLoc = GLES30.glGetUniformLocation(renderVideoProgramId , "uMVPMatrix");
        mUvUniformMatLoc = GLES30.glGetUniformLocation(renderVideoProgramId , "uSTMatrix");

        System.out.println("mUniformMatLoc = " + mUniformMatLoc+"  mUvUniformMatLoc =  " +mUvUniformMatLoc);

        surfaceTexture = new SurfaceTexture(textureID);
        surfaceTexture.setOnFrameAvailableListener(this);

        surface = new Surface(surfaceTexture);
//        surface.release();

        vertexBuf = ByteBuffer.allocateDirect(vertexData.length * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuf.put(vertexData);
        vertexBuf.position(0);

        textureCoordBuf = ByteBuffer.allocateDirect(textureCoordData.length * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureCoordBuf.put(textureCoordData);
        textureCoordBuf.position(0);

//        updateSurface = false;
    }

    /**
     * 创建oes纹理
     * @return
     */
    private int createOesTexture(){
        int[] textures = new int[1];

        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        int textureId = textures[0];
        return textureId;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

        synchronized(this){
//            if (updateSurface) {
//                surfaceTexture.updateTexImage();
//                surfaceTexture.getTransformMatrix(mSTMatrix);
//
//                for(int i = 0 ; i < mSTMatrix.length;i++){
//                    System.out.print(mSTMatrix[i] +"   ");
//                }
//                System.out.println();
//                updateSurface = false;
//            }

            if( _updateTexImageCompare != _updateTexImageCounter ) {
                // loop and call updateTexImage() for each time the onFrameAvailable() method was called below.
                while(_updateTexImageCompare != _updateTexImageCounter) {
                    surfaceTexture.updateTexImage();
                    surfaceTexture.getTransformMatrix(mSTMatrix);

                    _updateTexImageCompare++;  // increment the compare value until it's the same as _updateTexImageCounter
                }
            }

        }

        GLES30.glClearColor(1.0f , 1.0f , 0.0f , 1.0f);
        GLES30.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //draw frame
        GLES30.glUseProgram(renderVideoProgramId);

        Matrix.setIdentityM(mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mUniformMatLoc, 1, false, mMVPMatrix, 0);

        GLES30.glUniformMatrix4fv(mUvUniformMatLoc , 1 , false , mSTMatrix , 0);

        GLES30.glVertexAttribPointer(0 , 2,GLES30.GL_FLOAT ,false , 2 * Float.BYTES  , vertexBuf);
        GLES30.glEnableVertexAttribArray(0);

        GLES30.glVertexAttribPointer(1 , 2 , GLES30.GL_FLOAT , false , 2 * Float.BYTES , textureCoordBuf);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES , textureID);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN , 0 , 4);

         GLES30.glBindTexture(GLES20.GL_TEXTURE_2D , 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture sTexture) {
        System.out.println("sTexture.getTimestamp() = " + sTexture.getTimestamp());

        // requestRender();
         //urfaceTexture.updateTexImage();

        _updateTexImageCounter++;
    }
}
