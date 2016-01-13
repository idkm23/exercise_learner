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
        Subscriber<geometry_msgs.Quaternion> raw_myo_sub = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        raw_myo_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                housingActivity.update(MyoHelper.myoToMat(msg), 11);
            }
        });

        Subscriber<geometry_msgs.Quaternion> playback_sub = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        playback_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                if(MyoHelper.isEndingQuat(msg)) {
                    //Alert the activity
                } else {
                    housingActivity.update(MyoHelper.myoToMat(msg), 11);
                }
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

}