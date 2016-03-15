package com.example.chris.myapplication;

import android.util.Log;

import com.threed.jpct.Matrix;

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
    private Matrix lastUpperMyoReading = new Matrix();

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<geometry_msgs.Quaternion> raw_myo_l_sub = connectedNode.newSubscriber("/myo/l/ort", geometry_msgs.Quaternion._TYPE);
        raw_myo_l_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {
                    housingActivity.updateArm(lastUpperMyoReading, MyoHelper.myoToMat(msg));
                }
            }
        });

        Subscriber<geometry_msgs.Quaternion> raw_myo_u_sub = connectedNode.newSubscriber("/myo/u/ort", geometry_msgs.Quaternion._TYPE);
        raw_myo_u_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {
                    lastUpperMyoReading = MyoHelper.myoToMat(msg);
                }
            }
        });

        Subscriber<geometry_msgs.Quaternion> playback_sub = connectedNode.newSubscriber("/exercise/playback", geometry_msgs.Quaternion._TYPE);
        playback_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {

                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {

                    if(MyoHelper.isEndingQuat(msg)) {
                        return;
                    }

                    housingActivity.setProgramStatus(ExerciseActivity.ProgramStatus.STUCK_AT_STAGE);
                    housingActivity.getPlayerStats().promptedPlayer();

                    housingActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            housingActivity.getTextOverlay().setHeaderText("You seem to be having trouble, the model is now demonstrating the exercise");

                        }
                    });
                }

                else if (housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.PLAYBACK
                        || housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.STUCK_AT_STAGE) {
                    if(MyoHelper.isEndingQuat(msg))
                    {
                        housingActivity.beginExercise();
                    } else {
                        housingActivity.update(MyoHelper.myoToMat(msg), Constants.RELBOW_ID);
                    }
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

            /**
             * called when the beginPlaybackPublisher
             * @param emptyPublisher
             */
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

        housingActivity.setProgramStatus(ExerciseActivity.ProgramStatus.PLAYBACK);
        housingActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                housingActivity.getTextOverlay().setHeaderText("The model is now demonstrating the exercise you are to perform");

            }
        });

        beginPlaybackPublisher.publish(beginPlaybackPublisher.newMessage());
        Log.d("exercise", "pubbed trigger");
    }

}