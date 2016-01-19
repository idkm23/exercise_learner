package com.example.chris.myapplication;

import android.util.Log;

import com.threed.jpct.Matrix;

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
        m.rotateZ(-(float)Math.PI * .3f);
        m.rotateY((float)Math.PI / 3);

        return m;
    }

}
