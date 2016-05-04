package com.example.chris.myapplication;

import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Logger;
import com.threed.jpct.RGBColor;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import edu.uml.odgboxtherapy.R;
import raft.jpct.bones.Animated3D;

/**
 * Created by Chris Munroe on 1/2/15.
 */
public class MyRenderer implements GLSurfaceView.Renderer {

    // class singleton
    private static MyRenderer instance = null;

    private FrameBuffer frameBuffer = null;
    private final JPCTHolder jpctHolder;

    public MyRenderer(JPCTHolder jpctHolder) {
        instance = this;
        Config.maxPolysVisible = 5000;
        Config.farPlane = 1500;

        this.jpctHolder = jpctHolder;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        Logger.log("onSurfaceChanged");
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }

        frameBuffer = new FrameBuffer(gl, w, h);

        jpctHolder.cameraController.placeCamera();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Logger.log("onSurfaceCreated");

        TextureManager.getInstance().flush();
        Resources res = ExerciseActivity.getInstance().getResources();

        Texture texture = new Texture(res.openRawResource(R.raw.no_symbol_vincent));
        texture.keepPixelData(true);
        TextureManager.getInstance().addTexture("vincent", texture);

        for (Animated3D a : jpctHolder.actor)
            a.setTexture("vincent");

    }

//    private int fps = 0;
//    private int lfps = 0;
//    private long fpsTime = System.currentTimeMillis();

    @Override
    public void onDrawFrame(GL10 gl) {
        if (frameBuffer == null) {
            return;
        }

        ExerciseActivity.ProgramStatus programStatus = ExerciseActivity.getInstance().getProgramStatus();

        if(programStatus == ExerciseActivity.ProgramStatus.STUCK_AT_STAGE
                || programStatus == ExerciseActivity.ProgramStatus.PLAYBACK) {
            frameBuffer.clear(new RGBColor(33, 6, 0));

        } else {
            frameBuffer.clear(RGBColor.BLACK);

        }

        jpctHolder.world.renderScene(frameBuffer);
        jpctHolder.world.draw(frameBuffer);

        frameBuffer.display();

        //Code below will flood log with FPS info

//        if (System.currentTimeMillis() - fpsTime >= 1000) {
//            lfps = (fps + lfps) >> 1;
//            Log.d("myExercise", "fps: " + fps);
//            fps = 0;
//            fpsTime = System.currentTimeMillis();
//        }
//
//        fps++;

    }

    // Calculates and returns whole bounding box of skinned group
    protected float[] calcBoundingBox() {
        float[] box = null;

        for (Animated3D skin : jpctHolder.actor) {
            float[] skinBB = skin.getMesh().getBoundingBox();

            if (box == null) {
                box = skinBB;
            } else {
                // x
                box[0] = Math.min(box[0], skinBB[0]);
                box[1] = Math.max(box[1], skinBB[1]);
                // y
                box[2] = Math.min(box[2], skinBB[2]);
                box[3] = Math.max(box[3], skinBB[3]);
                // z
                box[4] = Math.min(box[4], skinBB[4]);
                box[5] = Math.max(box[5], skinBB[5]);
            }
        }
        return box;
    }

    public static MyRenderer getRenderer() {
        return instance;
    }

}