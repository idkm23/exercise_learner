package com.example.chris.myapplication;

import android.util.Log;

import com.threed.jpct.Matrix;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import bones.samples.android.*;
import raft.jpct.bones.Quaternion;
import std_msgs.Int32;

/**
 * Created by chris on 12/30/15.
 */
public class MyoSubscriber implements NodeMain {

    private final ExerciseActivity housingActivity;

    public MyoSubscriber(ExerciseActivity housingActivity) {
        this.housingActivity = housingActivity;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Quaternion> subscriber = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                housingActivity.update(myoToMat(msg), 11);
            }
        });
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("exercise_myo_sub");
    }

    public static Matrix myoToMat(geometry_msgs.Quaternion msg) {

        Quaternion quat = new Quaternion();

        //old ninja way
        //quat.set(-(float)msg.getZ(), (float)msg.getX(),
        //    -(float)msg.getY(), (float)msg.getW());

        quat.set((float) msg.getX(), -(float) msg.getZ(), (float) msg.getY(), (float) msg.getW());
        Matrix m = quat.getRotationMatrix();
        m.rotateY(-(float)Math.PI/2);

        return m;
    }
}