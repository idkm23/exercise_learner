package com.example.chris.myapplication;

import com.threed.jpct.Matrix;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;

/**
 * Created by chris on 12/30/15.
 */
public class StateSubscriber implements NodeMain {

    private final ExerciseActivity housingActivity = ExerciseActivity.getInstance();

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<std_msgs.Float64> accuracyStatSubscriber = connectedNode.newSubscriber("/exercise/accuracy", std_msgs.Float64._TYPE);
        accuracyStatSubscriber.addMessageListener(new MessageListener<std_msgs.Float64>() {
            @Override
            public void onNewMessage(final std_msgs.Float64 msg) {
                housingActivity.getPlayerStats().setAccuracy(msg.getData());
            }
        });

        Subscriber<std_msgs.Int32> modeSubscriber = connectedNode.newSubscriber("/exercise/mode", std_msgs.Int32._TYPE);
        modeSubscriber.addMessageListener(new MessageListener<std_msgs.Int32>() {
            @Override
            public void onNewMessage(final std_msgs.Int32 msg) {
                switch(msg.getData()) {
                    case 0:
                        housingActivity.beginPlayback();
                        break;

                    case 1:
                    case 2:
                        housingActivity.beginExercise();
                        break;
                }
            }
        });

        Subscriber<std_msgs.Float64> stateChangedSubscriber = connectedNode.newSubscriber("/exercise/progress", std_msgs.Float64._TYPE);
        stateChangedSubscriber.addMessageListener(new MessageListener<std_msgs.Float64>() {
            @Override
            public void onNewMessage(final std_msgs.Float64 msg) {

                if(housingActivity.getProgramStatus() != ExerciseActivity.ProgramStatus.EXERCISING
                        && housingActivity.getProgramStatus() != ExerciseActivity.ProgramStatus.STUCK_AT_STAGE) {
                    return;
                }

                housingActivity.getPlayerStats().setProgress(msg.getData());
            }
        });
    }

    @Override
    public void onShutdown(Node node) {}

    @Override
    public void onShutdownComplete(Node node) {}

    @Override
    public void onError(Node node, Throwable throwable) {}

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("exercise_state_sub").join(GraphName.newAnonymous());
    }
}