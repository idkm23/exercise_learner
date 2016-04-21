package com.example.chris.myapplication;

import com.threed.jpct.Matrix;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;


/**
 * Created by chris on 12/30/15.
 */
public class MyoSubscriber implements NodeMain {

    private final ExerciseActivity housingActivity = ExerciseActivity.getInstance();
    private Matrix lastUpperMyoReading = new Matrix(), lastUpperMyoPlaybackReading = new Matrix(), lower;

    @Override
    public void onStart(ConnectedNode connectedNode) {

        initializeShoulderPose();
        initializeRosObjects(connectedNode);

    }

    @Override
    public void onShutdown(Node node) {}

    @Override
    public void onShutdownComplete(Node node) {}

    @Override
    public void onError(Node node, Throwable throwable) {}

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("exercise_myo_sub").join(GraphName.newAnonymous());
    }

    // With two myos, this value will be overwritten, however if there is one myo this value
    // will not be overwritten and as a result it puts the shoulder in a natural position
    public void initializeShoulderPose() {
        lastUpperMyoReading.rotateY(-(float) Math.PI * .4f);
        lastUpperMyoReading.rotateZ(-.4f);
    }

    // Instantiates all the subscribers that handle quaternion data which is sent to the model to be visualized
    public void initializeRosObjects(ConnectedNode connectedNode) {

        Subscriber<geometry_msgs.Quaternion> raw_myo_l_sub = connectedNode.newSubscriber("/myo/l/ort", geometry_msgs.Quaternion._TYPE);
        raw_myo_l_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {
                    lower = MyoHelper.myoToMat(msg);
                    housingActivity.updateArm(lastUpperMyoReading, lower, MyoHelper.quatToEuler(msg));
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

        Subscriber<geometry_msgs.Quaternion> playback_u_sub = connectedNode.newSubscriber("/exercise/u/playback", geometry_msgs.Quaternion._TYPE);
        playback_u_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {
                lastUpperMyoPlaybackReading = MyoHelper.myoToMat(msg);
            }
        });


        Subscriber<geometry_msgs.Quaternion> playback_l_sub = connectedNode.newSubscriber("/exercise/l/playback", geometry_msgs.Quaternion._TYPE);
        playback_l_sub.addMessageListener(new MessageListener<geometry_msgs.Quaternion>() {
            @Override
            public void onNewMessage(geometry_msgs.Quaternion msg) {

                if(housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.EXERCISING) {

                    housingActivity.beginPlayback();
                    housingActivity.updateArm(lastUpperMyoPlaybackReading, MyoHelper.myoToMat(msg), MyoHelper.quatToEuler(msg));
                }

                else if (housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.PLAYBACK
                        || housingActivity.getProgramStatus() == ExerciseActivity.ProgramStatus.STUCK_AT_STAGE) {
                    if(MyoHelper.isEndingQuat(msg)) {
                        housingActivity.beginExercise();
                    } else {
                        housingActivity.updateArm(lastUpperMyoPlaybackReading, MyoHelper.myoToMat(msg), MyoHelper.quatToEuler(msg));
                    }
                }

            }
        });
    }

}