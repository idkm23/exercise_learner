package com.example.chris.myapplication;

import com.threed.jpct.Matrix;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import raft.jpct.bones.Quaternion;

/**
 * Created by chris on 12/30/15.
 */
public class PlaybackSubscriber implements NodeMain {

    private final ExerciseActivity housingActivity;

    public PlaybackSubscriber(ExerciseActivity housingActivity) {
        this.housingActivity = housingActivity;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Quaternion> subscriber = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                housingActivity.update(MyoSubscriber.myoToMat(msg), 11);
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
        return GraphName.of("exercise_playback_sub");
    }
}