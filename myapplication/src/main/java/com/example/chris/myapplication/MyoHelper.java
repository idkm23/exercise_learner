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

        return quat.getRotationMatrix();
    }

    // This doesnt work correctly, sorry.
    public static SimpleVector quatToEuler(Quaternion q) {
        return new SimpleVector(
                Math.atan2(2*q.y*q.w - 2*q.x*q.z, 1 - 2*q.y*q.y - 2*q.z*q.z),
                Math.atan2(2 * q.x * q.w - 2 * q.y * q.z, 1 - 2 * q.x * q.x - 2 * q.z * q.z),
                Math.asin(2*q.x*q.y + 2*q.z*q.w));
    }

    // Overloaded function to make ros msgs more convenient
    public static SimpleVector quatToEuler(geometry_msgs.Quaternion q) {
        return quatToEuler(new Quaternion((float)q.getX(), (float)q.getY(), (float)q.getZ(), (float)q.getW()));
    }

    // helper function for 'checkLineBox'
    public static SimpleVector getIntersection( float fDst1, float fDst2, SimpleVector P1, SimpleVector P2) {

        if ( (fDst1 * fDst2) >= 0.0f) return null;
        if ( fDst1 == fDst2) return null;

        SimpleVector hit = new SimpleVector(P2);
        hit.sub(P1);
        hit.scalarMul(-fDst1 / (fDst2 - fDst1));
        hit.add(P1);

        return hit;
    }

    // helper function for 'checkLineBox'
    public static SimpleVector inBox(SimpleVector hit, SimpleVector B1, SimpleVector B2, final int AXIS) {
        if(hit == null) {
            return null;
        }

        if ( AXIS==1 && hit.z > B1.z && hit.z < B2.z && hit.y > B1.y && hit.y < B2.y) return hit;
        if ( AXIS==2 && hit.z > B1.z && hit.z < B2.z && hit.x > B1.x && hit.x < B2.x) return hit;
        if ( AXIS==3 && hit.x > B1.x && hit.x < B2.x && hit.y > B1.y && hit.y < B2.y) return hit;

        return null;
    }

    // returns the intersection if line (L1, L2) intersects with the box (B1, B2)
    // otherwise null, algorithm from: http://www.3dkingdoms.com/weekly/weekly.php?a=3
    public static SimpleVector checkLineBox( SimpleVector B1, SimpleVector B2, SimpleVector L1, SimpleVector L2) {

        if (L2.x < B1.x && L1.x < B1.x
            || L2.x > B2.x && L1.x > B2.x
            || L2.y < B1.y && L1.y < B1.y
            || L2.y > B2.y && L1.y > B2.y
            || L2.z < B1.z && L1.z < B1.z
            || L2.z > B2.z && L1.z > B2.z) {
            return null;
        }

        if (L1.x > B1.x && L1.x < B2.x &&
                L1.y > B1.y && L1.y < B2.y &&
                L1.z > B1.z && L1.z < B2.z)
        {
            return L1;
        }

        SimpleVector hit;
        if( (hit = inBox( getIntersection( L1.x-B1.x, L2.x-B1.x, L1, L2), B1, B2, 1 )) != null
                || (hit = inBox( getIntersection( L1.y-B1.y, L2.y-B1.y, L1, L2), B1, B2, 2 )) != null
                || (hit = inBox( getIntersection( L1.z-B1.z, L2.z-B1.z, L1, L2), B1, B2, 3 )) != null
                || (hit = inBox( getIntersection( L1.x-B2.x, L2.x-B2.x, L1, L2), B1, B2, 1 )) != null
                || (hit = inBox( getIntersection( L1.y-B2.y, L2.y-B2.y, L1, L2), B1, B2, 2 )) != null
                || (hit = inBox( getIntersection( L1.z-B2.z, L2.z-B2.z, L1, L2), B1, B2, 3 )) != null )
            return hit;

        return null;
    }
}
