package com.example.chris.myapplication;

import android.util.Log;

import com.ardor3d.math.MathUtils;
import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

import raft.jpct.bones.Quaternion;

/**
 * Created by chris on 1/13/16.
 */
public class MyoHelper {

    public static int END_FLAG = -1337;

    public static boolean isEndingQuat(geometry_msgs.Quaternion msg) {
        return msg.getX() == END_FLAG && msg.getY() == END_FLAG && msg.getZ() == END_FLAG && msg.getW() == END_FLAG;
    }

    public static Matrix myoToMat(geometry_msgs.Quaternion msg) {

        Quaternion quat = new Quaternion();

        //old ninja way
        //quat.set(-(float)msg.getZ(), (float)msg.getX(),
        //    -(float)msg.getY(), (float)msg.getW());

        //old seymour way
        //quat.set((float) msg.getX(), -(float) msg.getZ(), (float) msg.getY(), (float) msg.getW());

        quat.set((float) msg.getX(), (float) msg.getY(), (float) msg.getZ(), (float) msg.getW());
        Matrix m = quat.getRotationMatrix();
        m.rotateZ(-(float) Math.PI * .4f);

//        eulerAngles(quat.x, quat.y, quat.z, quat.w);

        return m;
    }

    public static SimpleVector rotMatToEuler(Matrix m) throws IllegalArgumentException {
        double[] ea = new double[3];

        ea[1] = Math.asin(m.get(0, 2));

        double cos1 = Math.cos(ea[1]);
        if(Math.abs(cos1)<0.005*Math.PI/180.0) {        // Gimball lock?
            ea[0] = 0.0;
            ea[2] = Math.atan2(m.get(1, 0), m.get(1, 1));
        } else {
            ea[0] = Math.atan2(-m.get(1, 2)/cos1, m.get(2, 2)/cos1);
            ea[2] = Math.atan2(-m.get(0, 1)/cos1, m.get(0, 0)/cos1);
        }

        for(int i=0; i<3; i++) ea[i] = clamp(ea[i]);

        if(Math.random() < .2)
            Log.d("exercise", "r: " + MathUtils.RAD_TO_DEG * ea[0] + " p: "
                    + MathUtils.RAD_TO_DEG * ea[1] + " y: " + MathUtils.RAD_TO_DEG * ea[2]);

        return new SimpleVector(ea[0], ea[1], ea[2]);
    }

    private static double clamp(double v) {
        v = Math.IEEEremainder(v, 2.0*Math.PI);
        return v>=0 ? v : 2.0*Math.PI+v;
    }

    public static void eulerAngles(float x, float y, float z, float w) {
        float roll, pitch, yaw;
        float test = x * y + z * w;

        if (test > 0.499) { // singularity at north pole
            pitch = 2 * (float)Math.atan2(x, w);
            yaw = (float)Math.PI / 2;
            roll = 0;

        } else if (test < -0.499) { // singularity at south pole
            pitch = -2 * (float)Math.atan2(x, w);
            yaw = -(float)Math.PI / 2;
            roll = 0;

        } else {
            float sqx = x * x;
            float sqy = y * y;
            float sqz = z * z;
            pitch = (float) Math.atan2(2 * y * w - 2 * x * z, 1 - 2 * sqy - 2 * sqz);
            yaw = (float) Math.asin(2 * test);
            roll = (float) Math.atan2(2 * x * w - 2 * y * z, 1 - 2 * sqx - 2 * sqz);
        }

        Log.d("exercise", "r: " + MathUtils.RAD_TO_DEG * roll + " p: "
                + MathUtils.RAD_TO_DEG * pitch + " y: " + MathUtils.RAD_TO_DEG * yaw);
    }

}
