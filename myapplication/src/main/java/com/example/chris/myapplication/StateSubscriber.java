package com.example.chris.myapplication;

import com.threed.jpct.Matrix;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;

import raft.jpct.bones.Quaternion;

/**
 * Created by chris on 12/30/15.
 */
public class StateSubscriber implements NodeMain {

    private final ArrayList<Matrix> stateOrientations;
    private Matrix currentState;

    public StateSubscriber() {
        stateOrientations = new ArrayList();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Quaternion> allStateSubscriber = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        allStateSubscriber.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                stateOrientations.add(MyoSubscriber.myoToMat(msg));
            }
        });

        Subscriber<geometry_msgs.Quaternion> stateChangedSubscriber = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        stateChangedSubscriber.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                currentState = MyoSubscriber.myoToMat(msg);
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
        return GraphName.of("exercise_state_sub");
    }

    public ArrayList<Matrix> getStates() {
        return stateOrientations;
    }

    public Matrix getCurrentState() {
        return currentState;
    }

}