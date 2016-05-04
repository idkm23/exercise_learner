package com.example.chris.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

import org.ros.android.RosActivity;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by Chris Munroe on 12/30/15.
 *
 * Uses JPCT to animate a 3D model
 * with Quaternions that are recorded from the myo sensor
 * and delivered to the app via ROS
 */
public class ExerciseActivity extends RosActivity implements View.OnTouchListener {

    protected enum ProgramStatus {
        PLAYBACK, EXERCISING, STUCK_AT_STAGE, COMPLETE
    }

    private ProgramStatus programStatus;

    // class singleton
    private static ExerciseActivity instance;
    public static ExerciseActivity getInstance() {
        return instance;
    }

    // Keeps track of basic stats of the particular trial, like time taken to do the exercise
    private PlayerStats playerStats = new PlayerStats();
    private TextOverlay txtOverlay;

    private GLSurfaceView mGLView;
    private MyRenderer renderer;

    private PowerManager.WakeLock wakeLock;

    JPCTHolder jpctHolder;

    // ROS objects
    private MyoSubscriber myoSub;
    private StateSubscriber stateSub;

    // Appends a random string to the node name "exercisedetector". This allows two devices to use the app and connect to the same master
    // otherwise there could be two devices (perhaps two phones) that share the same node name
    public ExerciseActivity() {
        super("exercisedetector" + GraphName.newAnonymous(), "exercisedetector");
        instance = this;
    }

    // setting up ROS objects
    // this code is mostly copy-paste from ROS wiki
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        myoSub = new MyoSubscriber();
        stateSub = new StateSubscriber();

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(myoSub, nodeConfiguration);
            nodeMainExecutor.execute(stateSub, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e("exercisedetector", "socket error trying to get networking information from the master uri");
        }
    }

    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Contains and creates all the 3D objects
        jpctHolder = new JPCTHolder();

        // this is the surface which draws the 3D objects
        renderer = new MyRenderer(jpctHolder);
        jpctHolder.setupLight(renderer);

        wakeLockSetup();
        layoutSetup();

        beginPlayback();
    }

    // Makes it so the screen doesn't go to sleep
    private void wakeLockSetup() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "exercisedetector");
    }

    // Rather than using XML, sets up the apps GUI with just Java
    // Perhaps some of this is bad practice?
    private void layoutSetup() {
        FrameLayout frame = new FrameLayout(this);
        addContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mGLView = new GLSurfaceView(getApplication());
        mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                // Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
                // back to Pixelflinger on some device (read: Samsung I7500)
                int[] attributes = new int[]{EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE};
                EGLConfig[] configs = new EGLConfig[1];
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, configs, 1, result);
                return configs[0];
            }
        });
        mGLView.setRenderer(renderer);
        frame.addView(mGLView);

        txtOverlay = new TextOverlay(getApplicationContext());
        addContentView(txtOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        txtOverlay.setVisibility(View.VISIBLE);
        txtOverlay.bringToFront();
        txtOverlay.setOnTouchListener(this);
    }

    // Updates the model's right arm with the two matrices provided
    // Each matrix is in the context of the world. Meaning that,
    // the lowerArm matrix should NOT be relative to the upperArm matrix
    public void updateArm(Matrix upperArm, Matrix lowerArm) {

        // adjusts the upper arm joint
        jpctHolder.skeletonHelper.transformJointOnPivot(Constants.RSHOULDER_ID, upperArm);

        // calculates what the lowerArm matrix is relative to the upperArm matrix
        Matrix finalLowerArm = lowerArm.cloneMatrix();
        finalLowerArm.matMul(upperArm.invert());

        // adjusts the lower arm joint
        jpctHolder.skeletonHelper.transformJointOnPivot(Constants.RELBOW_ID, finalLowerArm);

        jpctHolder.skeletonHelper.getPose().updateTransforms();

        // L1 and L2 are points which accurately describe a line which runs through the arm
        SimpleVector L1 = jpctHolder.skeletonHelper.getPose().getGlobal(
                Constants.RELBOW_ID).getTranslation();
        L1.matMul(jpctHolder.actor.get(0).getWorldTransformation());
        SimpleVector L2 = jpctHolder.skeletonHelper.getPose().getGlobal(
                Constants.RMIDDLEFINGERTIP_ID).getTranslation();
        L2.matMul(jpctHolder.actor.get(0).getWorldTransformation());

        // Check if that line collides with the chest
        SimpleVector hit = MyoHelper.checkLineBox(Constants.q2, Constants.q7, L1, L2);

        // if the arm collides with the chest, does not proceed with the model update
        if(hit != null) {
            return;
        }

        // finalize adjustments and display it on the model
        jpctHolder.skeletonHelper.getGroup().applySkeletonPose();
        jpctHolder.skeletonHelper.getGroup().applyAnimation();

    }

    public void beginExercise() {
        programStatus = ProgramStatus.EXERCISING;
        playerStats.setProgress(0);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                txtOverlay.setHeaderText(Constants.CUE2);

            }
        });
    }

    public void beginPlayback() {

        programStatus = ExerciseActivity.ProgramStatus.PLAYBACK;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                txtOverlay.setHeaderText(Constants.CUE1);

            }
        });
    }

    public void completeExercise() {
        programStatus = ProgramStatus.COMPLETE;

        if(playerStats != null) {
            playerStats.finish();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(programStatus == ProgramStatus.COMPLETE) {

            clear();
            beginPlayback();

        }

        return true;
    }

    public void clear() {
        playerStats.setProgress(0);
        playerStats = new PlayerStats();
        txtOverlay.invalidate();
    }


    @Override
    protected void onPause() {
        Logger.log("onPause");
        super.onPause();
        mGLView.onPause();

        if (wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    protected void onResume() {
        Logger.log("onResume");
        super.onResume();
        mGLView.onResume();

        if (!wakeLock.isHeld())
            wakeLock.acquire();
    }

    @Override
    protected void onStop() {
        Logger.log("onStop");
        super.onStop();
    }

    public TextOverlay getTextOverlay() {
        return txtOverlay;
    }

    public StateSubscriber getStateSub() {
        return stateSub;
    }

    public MyoSubscriber getMyoSub() {
        return myoSub;
    }

    public ProgramStatus getProgramStatus() {
        return programStatus;
    }

    public void setProgramStatus(ProgramStatus programStatus) {
        this.programStatus = programStatus;
    }

    public PlayerStats getPlayerStats() {
        return playerStats;
    }
}