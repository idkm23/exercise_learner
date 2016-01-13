/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2014 Robert Maupin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package bones.samples.android;

import com.threed.jpct.Matrix;
import com.threed.jpct.SimpleVector;

import java.util.HashMap;
import java.util.IdentityHashMap;

import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.Joint;
import raft.jpct.bones.Skeleton;
import raft.jpct.bones.SkeletonPose;

/**
 * SkeletonHelper is a simple hlper class for manually manipulating a skeleton.
 * @author Robert Maupin (Chase)
 */
public class SkeletonHelper {
    /**
     * Blends between any number of matrixes based on a set of weights. The
     * weights can be any value that corresponds to how strongly that matrix
     * should be weighted compared to the other matrixes. The values will
     * automatically be modified.<br>
     * <br>
     *
     * Meaning that weights 12, 6, 2 would become 0.6, 0.3, and 0.1. Weights 1
     * and 1 would become 0.5 and 0.5.
     */
    public static Matrix blendMatrix(Matrix[] mats, float[] weights) {
        if (mats == null || weights == null || mats.length != weights.length
                || mats.length == 0) {
            return null;
        }
        // normalize weights
        float total = 0;
        for (int i = 0; i < weights.length; ++i) {
            if (mats[i] == null) {
                return null;
            }
            total += weights[i];
        }
        for (int i = 0; i < weights.length; ++i) {
            weights[i] /= total;
        }

        return doBlendMatrix(mats, weights);
    }

    /**
     * Blends between any number of skeleton poses based on a set of weights.
     */
    public static SkeletonPose blendSkeletonPose(SkeletonPose[] poses,
                                                 float[] weights) {
        if (poses == null || weights == null || poses.length != weights.length
                || poses.length == 0 || poses[0] == null) {
            return null;
        }

        float total = 0;
        Skeleton skel = poses[0].getSkeleton();
        for (int i = 0; i < weights.length; ++i) {
            if (poses[i] == null || poses[i].getSkeleton() != skel) {
                return null;
            }
            total += weights[i];
        }
        for (int i = 0; i < weights.length; ++i) {
            weights[i] /= total;
        }
        SkeletonPose pose = poses[0].clone();
        pose.setToBindPose();

        int jointCount = skel.getNumberOfJoints();
        Matrix[] mats = new Matrix[poses.length];
        for (int j = 0; j < jointCount; ++j) {
            for (int i = 0; i < weights.length; ++i) {
                mats[i] = poses[i].getLocal(j);
            }
            pose.getLocal(j).setTo(doBlendMatrix(mats, weights));
        }
        return pose;
    }

    private static Matrix doBlendMatrix(Matrix[] mats, float[] weights) {
        float[] out = new float[16];
        float[] tmp = new float[16];
        for (int i = 0; i < weights.length; ++i) {

            mats[i].fillDump(tmp);
            for (int j = 0; j < out.length; ++j) {
                out[j] += tmp[j] * weights[i];
            }
        }
        Matrix mat = new Matrix();
        mat.setDump(out);
        return mat;
    }

    /**
     * Interpolates between two skeleton poses. The interpolation is
     * (poseA*(1-weight)+poseB*weight).
     */
    public static SkeletonPose interpolateSkeletonPose(SkeletonPose poseA,
                                                       SkeletonPose poseB, float weight) {
        if (poseA == null || poseB == null) {
            return null;
        }
        Skeleton skel = poseA.getSkeleton();
        if (skel != poseB.getSkeleton()) {
            return null;
        }
        int count = skel.getNumberOfJoints();
        SkeletonPose poseC = poseA.clone();
        for (int i = 0; i < count; ++i) {
            Matrix matA = poseA.getLocal(i).cloneMatrix();
            Matrix matB = poseB.getLocal(i);

            matA.interpolate(matA, matB, weight);
            poseC.getLocal(i).setTo(matA);
        }
        return poseC;
    }

    public final AnimatedGroup group;
    public final Skeleton skel;

    public SkeletonPose pose;

    private final HashMap<String, Integer> joints;

    private final IdentityHashMap<Joint, Integer> indexes;

    private final int jointCount;

    public SkeletonHelper(AnimatedGroup group) {
        this.group = group;
        pose = group.get(0).getSkeletonPose();
        skel = pose.getSkeleton();

        jointCount = skel.getNumberOfJoints();
        joints = new HashMap<String, Integer>();
        // may be problematic, need Joint to implement hashCode()
        indexes = new IdentityHashMap<Joint, Integer>();

        for (int i = 0; i < jointCount; ++i) {
            Joint joint = skel.getJoint(i);
            joints.put(joint.getName(), i);
            indexes.put(joint, i);
        }
    }

    /**
     * Sets this joints matrix to the given matrix, offset by the parent joints
     * inverse bind matrix.
     */
    public void applyJointMatrixP(int jointIndex, Matrix mat) {
        Joint joint = getJoint(jointIndex);
        if (joint != null) {
            mat = mat.cloneMatrix();
            mulMatrixByInverse(getJoint(joint.getParentIndex()), mat);
            pose.getLocal(jointIndex).setTo(mat);
        }
    }

    /**
     * Sets this joints matrix to the given matrix, offset by the parent joints
     * inverse bind matrix.
     */
    public void applyJointMatrixP(Joint joint, Matrix mat) {
        mat = mat.cloneMatrix();
        mulMatrixByInverse(getJoint(joint.getParentIndex()), mat);
        pose.getLocal(getJointIndex(joint)).setTo(mat);
    }

    /**
     * Sets this joints matrix to the given matrix, offset by the parent joints
     * inverse bind matrix.
     */
    public void applyJointMatrixP(String jointName, Matrix mat) {
        applyJointMatrixP(getJointIndex(jointName), mat);
    }

    public SimpleVector getBoneDirection(int jointIndex) {
        return getBoneDirection(getJoint(jointIndex));
    }

    public SimpleVector getBoneDirection(Joint joint) {
        if (joint != null) {
            Joint parent = getParentJoint(joint);
            if (parent != null) {
                return joint.getBindPose().getTranslation()
                        .calcSub(parent.getBindPose().getTranslation())
                        .normalize();
            }
        }
        return null;
    }

    public SimpleVector getBoneDirection(String jointName) {
        return getBoneDirection(getJoint(jointName));
    }

    public Joint getJoint(int jointIndex) {
        return skel.getJoint(jointIndex);
    }

    public Joint getJoint(String jointName) {
        Integer index = joints.get(jointName);
        if (index != null) {
            return skel.getJoint(index);
        }
        return null;
    }

    public Matrix getJointBindMatrix(int jointIndex) {
        return getJointBindMatrix(getJoint(jointIndex));
    }

    public Matrix getJointBindMatrix(Joint joint) {
        if (joint == null) {
            return null;
        }
        return new Matrix(joint.getBindPose());
    }

    public Matrix getJointBindMatrix(String jointName) {
        return getJointBindMatrix(getJoint(jointName));
    }

    public int getJointIndex(Joint joint) {
        Integer index = indexes.get(joint);
        if (index != null) {
            return index;
        }
        return -1;
    }

    public int getJointIndex(String jointName) {
        Integer index = joints.get(jointName);
        if (index != null) {
            return index;
        }
        return -1;
    }

    public String[] getJointList() {
        String[] list = new String[jointCount];
        for (int i = 0; i < list.length; ++i) {
            list[i] = skel.getJoint(i).getName();
        }
        return list;
    }

    public Joint getParentJoint(int jointIndex) {
        return getParentJoint(getJoint(jointIndex));
    }

    public Joint getParentJoint(Joint joint) {
        if (joint != null) {
            int parent = joint.getParentIndex();
            if (parent != -1) {
                return getJoint(parent);
            }
        }
        return null;
    }

    public Joint getParentJoint(String jointName) {
        return getParentJoint(getJoint(jointName));
    }

    /**
     * Interpolates the current skeleton pose with the given skeleton pose and
     * applies it to the current skeleton pose.
     */
    public void interpolateSkeletonPose(SkeletonPose poseB, float weight) {
        if (poseB == null || poseB.getSkeleton() != skel) {
            return;
        }
        for (int i = 0; i < jointCount; ++i) {
            Matrix mat = pose.getLocal(i);
            mat.interpolate(mat, poseB.getLocal(i), weight);
        }
    }

    private void mulMatrixByInverse(Joint ref, Matrix mat) {
        if (ref != null) {
            mat.matMul(ref.getInverseBindPose());
        }
    }

    public void setSkeletonPose(SkeletonPose pose) {
        if (pose == null) {
            return;
        }
        if (pose.getSkeleton() != skel) {
            return;
        }
        group.setSkeletonPose(this.pose = pose);
    }

    /**
     * Transforms this joint by the given matrix.
     */
    public void transformJoint(int jointIndex, Matrix mat) {
        transformJoint(getJoint(jointIndex), mat);
    }

    /**
     * Transforms this joint by the given matrix.
     */
    public void transformJoint(Joint joint, Matrix mat) {
        Matrix bind = getJointBindMatrix(joint);
        bind.matMul(mat);

        mulMatrixByInverse(getJoint(joint.getParentIndex()), bind);
        pose.getLocal(getJointIndex(joint)).setTo(bind);
    }

    /**
     * Transforms this joint by the given matrix.
     */
    public void transformJoint(String jointName, Matrix mat) {
        transformJoint(getJoint(jointName), mat);
    }

    /**
     * Transforms the joint on its pivot point by the given matrix.
     */
    public void transformJointOnPivot(int jointIndex, Matrix mat) {
        transformJointOnPivot(getJoint(jointIndex), mat);
    }

    /**
     * Transforms the joint on its pivot point by the given matrix.
     */
    public void transformJointOnPivot(Joint joint, Matrix mat) {
        Matrix bind = getJointBindMatrix(joint);

        SimpleVector pos = bind.getTranslation();

        // set the matrix to the origin
        pos.scalarMul(-1);
        bind.translate(pos);

        // apply the transformation (this may translate it as well of course)
        bind.matMul(mat);

        // place it back where it was
        pos.scalarMul(-1);
        bind.translate(pos);

        // TODO see if we need to do anything if the parent is not in its bind
        // position
        mulMatrixByInverse(getJoint(joint.getParentIndex()), bind);
        pose.getLocal(getJointIndex(joint)).setTo(bind);
    }

    /**
     * Transforms the joint on its pivot point by the given matrix.
     */
    public void transformJointOnPivot(String jointName, Matrix mat) {
        transformJointOnPivot(getJoint(jointName), mat);
    }

    /**
     * Rotates a joint from it's bind-pose to a specified angle
     */
    public void rotateJoint(int jointIndex, float angle) {
//        Log.d("myNinja", "rotated");
//        Matrix rotated = new Matrix(pose.getLocal(jointIndex));
//        rotated.rotateX(angle);
//        pose.getLocal(jointIndex).setTo(rotated);

        Matrix rotated = new Matrix();
        rotated.rotateX(angle);
        transformJointOnPivot(jointIndex, rotated);
    }

    public SkeletonPose getPose() {
        return pose;
    }

    public AnimatedGroup getGroup() {
        return group;
    }

}