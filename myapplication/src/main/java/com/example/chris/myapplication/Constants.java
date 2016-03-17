package com.example.chris.myapplication;

import com.threed.jpct.SimpleVector;

/**
 * Created by chris on 1/19/16.
 */
public class Constants {
    public static final int RHAND_ID = 26;
    public static final int LHAND_ID = 25;
    public static final int RELBOW_ID = 24;
    public static final int LELBOW_ID = 22;
    public static final int RSHOULDER_ID = 21;
    public static final int LSHOULDER_ID = 19;

    public static final float HAND_ELBOW_ROTATION_RATIO = .6f;
    public static final float HAND_ROTATION_OFFSET = 0;

    public static final SimpleVector p1 = new SimpleVector(-37.27, -57.9, 2);
    public static final SimpleVector p2 = new SimpleVector(-37.27, -57.9, -0.8);
    public static final SimpleVector p3 = new SimpleVector(-37.27, -55.1, 2);
    public static final SimpleVector p4 = new SimpleVector(-37.27, -55.1, -0.07);
    public static final SimpleVector p5 = new SimpleVector(-18.5,   -57.9, 2);
    public static final SimpleVector p6 = new SimpleVector(-18.5,   -57.9, -0.07);
    public static final SimpleVector p7 = new SimpleVector(-18.5,   -55.1, 2);
    public static final SimpleVector p8 = new SimpleVector(-18.5,   -55.1, -0.07);
    public static final SimpleVector[] armPoints = {p1, p2, p3, p4, p5, p6, p7, p8};
}
