package com.example.chris.myapplication;

import android.util.Log;

import org.ros.internal.node.RegistrantListener;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;
import org.ros.node.topic.Subscriber;

import std_msgs.Empty;

/**
 * Created by chris on 12/30/15.
 */
public class MyoSubscriber implements NodeMain {

    private Publisher<std_msgs.Empty> beginPlaybackPublisher;
    private final ExerciseActivity housingActivity = ExerciseActivity.getInstance();

    public MyoSubscriber() {}

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Quaternion> raw_myo_sub = connectedNode.newSubscriber("/myo/ort", geometry_msgs.Quaternion._TYPE);
        raw_myo_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {
                    housingActivity.update(MyoHelper.myoToMat(msg), Constants.RELBOW_ID);
                }
            }
        });

        Subscriber<geometry_msgs.Quaternion> playback_sub = connectedNode.newSubscriber("/exercise/playback", geometry_msgs.Quaternion._TYPE);
        playback_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {

                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {
                    housingActivity.setProgramStatus(ExerciseActivity.ProgramStatus.STUCK_AT_STAGE);

                    housingActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            housingActivity.getTextOverlay().setHeaderText("You seem to be having trouble, the robot is now demonstrating the exercise");

                        }
                    });
                }

                if (MyoHelper.isEndingQuat(msg)) {
                    housingActivity.beginExercise();
                } else {
                    housingActivity.update(MyoHelper.myoToMat(msg), Constants.RELBOW_ID);
                }
            }
        });

        beginPlaybackPublisher = connectedNode.newPublisher("/exercise/playback_trigger", std_msgs.Empty._TYPE);
        beginPlaybackPublisher.addListener(new PublisherListener<Empty>() {
            @Override
            public void onNewSubscriber(Publisher<Empty> publisher, SubscriberIdentifier subscriberIdentifier) {

            }

            @Override
            public void onShutdown(Publisher<Empty> publisher) {

            }

            @Override
            public void onMasterRegistrationSuccess(Publisher<Empty> emptyPublisher) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                beginPlayback();
            }

            @Override
            public void onMasterRegistrationFailure(Publisher<Empty> emptyPublisher) {

            }

            @Override
            public void onMasterUnregistrationSuccess(Publisher<Empty> emptyPublisher) {

            }

            @Override
            public void onMasterUnregistrationFailure(Publisher<Empty> emptyPublisher) {

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

    public void beginPlayback() {
        beginPlaybackPublisher.publish(beginPlaybackPublisher.newMessage());
        Log.d("exercise", "pubbed trigger");
    }

}