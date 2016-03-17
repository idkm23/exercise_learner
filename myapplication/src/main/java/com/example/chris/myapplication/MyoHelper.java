package com.example.chris.myapplication;

import android.util.Log;

import com.ardor3d.math.MathUtils;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;

import raft.jpct.bones.Quaternion;

/**
 * Created by chris on 1/13/16.
 */
public class MyoHelper {

    public static int END_FLAG = -1337;

    public static boolean isEndingQuat(geometry_msgs.Quaternion msg) {
        return msg.getX() == END_FLAG || msg.getY() == END_FLAG || msg.getZ() == END_FLAG || msg.getW() == END_FLAG;
    }

    public static Matrix myoToMat(geometry_msgs.Quaternion msg) {

        Quaternion quat = new Quaternion();

        quat.set((float) msg.getX(), (float) msg.getY(), (float) msg.getZ(), (float) msg.getW());
        Matrix m = quat.getRotationMatrix();

        // Myo's yaw is set to 0 initially and
        // if yaw = 0 then the arm would face the base pose (side ways)
        // so we rotate it to have the arm face forward
        //m.rotateZ(-(float) Math.PI * .34f);

        return m;
    }

    public static SimpleVector quatToEuler(geometry_msgs.Quaternion q) {
        return quatToEuler(new Quaternion((float)q.getX(), (float)q.getY(), (float)q.getZ(), (float)q.getW()));
    }

    public static SimpleVector quatToEuler(Quaternion q) {
        return new SimpleVector(
                Math.atan2(2*q.y*q.w - 2*q.x*q.z, 1 - 2*q.y*q.y - 2*q.z*q.z),
                Math.atan2(2 * q.x * q.w - 2 * q.y * q.z, 1 - 2 * q.x * q.x - 2 * q.z * q.z),
                Math.asin(2*q.x*q.y + 2*q.z*q.w));
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

        return new SimpleVector(ea[0], ea[1], ea[2]);
    }

    private static double clamp(double v) {
        v = Math.IEEEremainder(v, 2.0*Math.PI);
        return v>=0 ? v : 2.0*Math.PI+v;
    }
    
    public static Object3D createBox(SimpleVector[] p) {
        if(p.length != 8) {
            Log.d("MyoHelper", "must provide 8 points for \'createBox\'");
            return null;
        }

        Object3D box = new Object3D(12);
        box.addTriangle(p[0], p[1], p[3]);
        box.addTriangle(p[0], p[3], p[2]);
        box.addTriangle(p[0], p[2], p[6]);
        box.addTriangle(p[0], p[6], p[4]);
        box.addTriangle(p[0], p[4], p[5]);
        box.addTriangle(p[0], p[5], p[1]);
        box.addTriangle(p[7], p[5], p[4]);
        box.addTriangle(p[7], p[4], p[6]);
        box.addTriangle(p[7], p[5], p[1]);
        box.addTriangle(p[7], p[1], p[3]);
        box.addTriangle(p[7], p[3], p[2]);
        box.addTriangle(p[7], p[2], p[6]);

        return box;
    }
}
