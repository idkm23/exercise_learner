package com.example.chris.myapplication;

import bones.samples.android.CameraOrbitController;
import bones.samples.android.SkeletonHelper;
import edu.uml.odgboxtherapy.R;

import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class ExerciseActivity extends RosActivity {

    protected enum ProgramStatus {
        PLAYBACK, PLAY2EXER, EXERCISING, STUCK_AT_STAGE, COMPLETE
    }

    private ProgramStatus programStatus = ProgramStatus.PLAYBACK;

    private static ExerciseActivity instance;

    public static ExerciseActivity getInstance() {
        return instance;
    }

    private TextOverlay txtOverlay;
    private GLSurfaceView mGLView;
    private final MyRenderer renderer = new MyRenderer();

    private World world = null;
    private AnimatedGroup actor;
    private SkeletonHelper skeletonHelper;
    private CameraOrbitController cameraController;

    private PowerManager.WakeLock wakeLock;

    //data buffers for camera tilt
    Matrix baseCameraRotation, camBasePose;

    /* the rotational matrix for the shoulder, necessary to properly position the elbow with the myo */
    Matrix RShoulderRot;

    /*** ROS STUFF ***/
    private MyoSubscriber myoSub;
    private StateSubscriber stateSub;


    public ExerciseActivity() {
        super("exercisedetector", "exercisedetector");
        instance = this;
    }

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

    /*** JPCT STUFF ***/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.log("onCreate");

        super.onCreate(savedInstanceState);

        //setContentView(new LoadingView(getApplicationContext()));

        FrameLayout frame = new FrameLayout(this);
        addContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mGLView = new GLSurfaceView(getApplication());
        frame.addView(mGLView);

        txtOverlay = new TextOverlay(getApplicationContext());
        addContentView(txtOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        txtOverlay.setVisibility(View.VISIBLE);
        txtOverlay.bringToFront();
        txtOverlay.setHeaderText("The model is now demonstrating the exercise you are to perform");

        mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                // Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
                // back to Pixelflinger on some device (read: Samsung I7500)
                int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
                EGLConfig[] configs = new EGLConfig[1];
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, configs, 1, result);
                return configs[0];
            }
        });

        mGLView.setRenderer(renderer);

        if (world != null)
            return;

        world = new World();

        try {
            Resources res = getResources();
            actor = BonesIO.loadGroup(res.openRawResource(R.raw.man));
            actor.addToWorld(world);

            skeletonHelper = new SkeletonHelper(actor);

            actor.getRoot().rotateX(-(float) Math.PI / 2);
//            actor.getRoot().rotateMesh();
//            actor.getRoot().clearRotation();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        world.setAmbientLight(127, 127, 127);
        world.buildAllObjects();

        float[] bb = renderer.calcBoundingBox();
        float height = (bb[3] - bb[2]); // actor height
        new Light(world).setPosition(new SimpleVector(0, -height / 2, height));

        cameraController = new CameraOrbitController(world.getCamera());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "exercisedetector");

        initializeModelPose();
    }

    /**
     * Puts the actor in a more natural pose
     */
    void initializeModelPose() {

        RShoulderRot = new Matrix();
        RShoulderRot.rotateY(-(float) Math.PI / 3);
        RShoulderRot.rotateZ(-.4f);

        update(RShoulderRot, Constants.RSHOULDER_ID); //10);
        RShoulderRot = RShoulderRot.invert();

        Matrix LShoulderRot = new Matrix();
        LShoulderRot.rotateY((float) Math.PI * 4 / 11);

        update(LShoulderRot, Constants.LSHOULDER_ID);//7);

        Matrix LElbowRot = new Matrix();
        LElbowRot.rotateY((float) Math.PI * 5 / 11);
        LElbowRot.matMul(LShoulderRot.invert());

        update(LElbowRot, Constants.LELBOW_ID); //8);

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cameraController.onTouchEvent(event))
            return true;

        return super.onTouchEvent(event);
    }

    class MyRenderer implements GLSurfaceView.Renderer {

        private FrameBuffer frameBuffer = null;

        public MyRenderer() {
            Config.maxPolysVisible = 5000;
            Config.farPlane = 1500;
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {
            Logger.log("onSurfaceChanged");
            if (frameBuffer != null) {
                frameBuffer.dispose();
            }

            frameBuffer = new FrameBuffer(gl, w, h);

            cameraController.placeCamera();
            camBasePose = world.getCamera().getDirection().getRotationMatrix();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Logger.log("onSurfaceCreated");

            TextureManager.getInstance().flush();
            Resources res = getResources();

            Texture texture = new Texture(res.openRawResource(R.raw.no_symbol_vincent));
            texture.keepPixelData(true);
            TextureManager.getInstance().addTexture("vincent", texture);

            for (Animated3D a : actor)
                a.setTexture("vincent");

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (frameBuffer == null) {
                return;
            }

            frameBuffer.clear();
            world.renderScene(frameBuffer);
            world.draw(frameBuffer);

            frameBuffer.display();
        }

        /** calculates and returns whole bounding box of skinned group */
        protected float[] calcBoundingBox() {
            float[] box = null;

            for (Animated3D skin : actor) {
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

    }

    public void update(Matrix rotation, int selector) {

        if(selector == Constants.RELBOW_ID) {
            rotation.matMul(RShoulderRot);

            SimpleVector armEulers = MyoHelper.rotMatToEuler(rotation);
            Matrix handRotation = new Matrix();

            if(armEulers.x > Math.PI) {
                armEulers.x -= 2*Math.PI;
            }

            handRotation.rotateX(armEulers.x * .6f);
            update(handRotation, Constants.RHAND_ID);
        }

        skeletonHelper.transformJointOnPivot(selector, rotation);

        skeletonHelper.getPose().updateTransforms();
        skeletonHelper.getGroup().applySkeletonPose();
        skeletonHelper.getGroup().applyAnimation();

    }

    public void beginExercise() {
        programStatus = ProgramStatus.EXERCISING;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                txtOverlay.setHeaderText("Now it is your turn to perform the exercise");

            }
        });
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

}