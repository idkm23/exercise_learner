package com.example.chris.myapplication;

import android.content.res.Resources;

import com.threed.jpct.Light;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

import bones.samples.android.SkeletonHelper;
import edu.uml.odgboxtherapy.R;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;

/**
 * Created by Chris Munroe on 5/4/16.
 * Before, all these components were defined in the ExerciseActivity.
 * This class' only purpose is to organize some of the JPCT components to improve code readability
 */
public class JPCTHolder {
    public final World world;
    public final AnimatedGroup actor;
    public final SkeletonHelper skeletonHelper;

    public final MyCamera cameraController;

    public JPCTHolder() {

        world = new World();

        try {
            Resources res = ExerciseActivity.getInstance().getResources();
            actor = BonesIO.loadGroup(res.openRawResource(R.raw.fortypolyvincent));
            actor.addToWorld(world);

            skeletonHelper = new SkeletonHelper(actor);

            actor.getRoot().rotateX(-(float) Math.PI / 2);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        world.setAmbientLight(127, 127, 127);
        world.buildAllObjects();


        cameraController = new MyCamera(world.getCamera());
    }

    public void setupLight(MyRenderer renderer) {

        float[] bb = renderer.calcBoundingBox();
        float height = (bb[3] - bb[2]); // actor height
        new Light(world).setPosition(new SimpleVector(0, -height / 2, height));

    }
}
