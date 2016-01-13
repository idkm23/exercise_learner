package com.example.chris.myapplication;

import bones.samples.android.CameraOrbitController;
import bones.samples.android.SkeletonHelper;
import glfont.GLFont;
import edu.uml.odgboxtherapy.R;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.Quaternion;
import raft.jpct.bones.SkeletonPose;
import raft.jpct.bones.SkinClip;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;

import com.threed.jpct.Animation;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Mesh;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class ExerciseActivity extends RosActivity implements SensorEventListener {

    private GLSurfaceView mGLView;
    private final MyRenderer renderer = new MyRenderer();
    private World world = null;

    private CameraOrbitController cameraController;

    private PowerManager.WakeLock wakeLock;

    private AnimatedGroup actor;

    private SkeletonHelper skeletonHelper;

    //Sensor-data variables for camera tilt
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor rotVectSensor;

    //data buffers for camera tilt
    Matrix baseCameraRotation, cameraRotation, camBasePose;

    /*** ROS STUFF ***/

    private MyoSubscriber myoSub;

    Matrix RShoulderRot;

    public ExerciseActivity() {
        super("exercisedetector", "exercisedetector");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        myoSub = new MyoSubscriber(this);

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutor.execute(myoSub, nodeConfiguration);
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

        mGLView = new GLSurfaceView(getApplication());
//        setContentView(mGLView);

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
            actor = BonesIO.loadGroup(res.openRawResource(R.raw.seymour_group));
            actor.addToWorld(world);

            skeletonHelper = new SkeletonHelper(actor);
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

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotVectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        initializeModelPose();
    }

    void initializeModelPose() {

        RShoulderRot = new Matrix();
        RShoulderRot.rotateZ((float) Math.PI / 3);
        RShoulderRot.rotateX(.6f);

        update(RShoulderRot, 10);
        RShoulderRot = RShoulderRot.invert();

        Matrix LShoulderRot = new Matrix();
        LShoulderRot.rotateZ(-(float) Math.PI * 2 / 6);

        update(LShoulderRot, 7);

        Matrix LElbowRot = new Matrix();
        LElbowRot.rotateZ(-(float) Math.PI * 5 / 11);
        LElbowRot.matMul(LShoulderRot.invert());

        update(LElbowRot, 8);

    }

    @Override
    protected void onPause() {
        Logger.log("onPause");
        super.onPause();
        mGLView.onPause();

        mSensorManager.unregisterListener(this);

        if (wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    protected void onResume() {
        Logger.log("onResume");
        super.onResume();
        mGLView.onResume();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, rotVectSensor, SensorManager.SENSOR_DELAY_UI);

        if (!wakeLock.isHeld())
            wakeLock.acquire();
    }

    @Override
    protected void onStop() {
        Logger.log("onStop");
        super.onStop();
    }

    int selector = 2;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            baseCameraRotation = null;
            selector = ++selector%27;
            Log.d("myNinja", "selector: " + selector);
        }

        if (cameraController.onTouchEvent(event))
            return true;

        return super.onTouchEvent(event);
    }

    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            Quaternion glassOrientation = new Quaternion();

            glassOrientation.set(event.values[0], event.values[2], event.values[1],
                    (event.values.length == 4 ? event.values[3] : 0));
//            Log.d("myNinja", "ort: " + event.values[0] + " " + event.values[1] + " " + event.values[2]
//                    + " " + ( event.values.length == 4 ? event.values[3] : 0));
            cameraRotation = glassOrientation.getRotationMatrix();

            if (baseCameraRotation == null) {
                baseCameraRotation = cameraRotation.invert();
            }

            cameraRotation.matMul(baseCameraRotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

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

            Texture texture = new Texture(res.openRawResource(R.raw.seymour));
            texture.keepPixelData(true);
            TextureManager.getInstance().addTexture("ninja", texture);

            for (Animated3D a : actor)
                a.setTexture("ninja");
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (frameBuffer == null) {
                return;
            }

            if (cameraRotation != null) {
                synchronized (cameraRotation) {
                    Matrix m = new Matrix(camBasePose);
                    m.matMul(cameraRotation);
                    //world.getCamera().setBack(m);
                }
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

    //make this a callback function for a subscriber
    public void update(Matrix rotation, int selector) {

        if(selector == 11)
            rotation.matMul(RShoulderRot);

        skeletonHelper.transformJointOnPivot(selector, rotation);

        skeletonHelper.getPose().updateTransforms();
        skeletonHelper.getGroup().applySkeletonPose();
        skeletonHelper.getGroup().applyAnimation();

    }
}