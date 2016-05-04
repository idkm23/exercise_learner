package com.example.chris.myapplication;

import com.threed.jpct.Camera;
import com.threed.jpct.SimpleVector;

public class MyCamera {

	public float cameraRadius = 49f;
	public float cameraHeight = -46f;
	public float cameraHorzOffset = -11f;

	private final SimpleVector cameraTarget = new SimpleVector(cameraHorzOffset, cameraHeight, 0);

	private Camera camera;
	
	public MyCamera(Camera camera) {
		this.camera = camera;
	}
	
    public void placeCamera() {
        SimpleVector camPos = new SimpleVector(0, 1f, -cameraRadius);
        camPos.add(cameraTarget);
        camera.setPosition(camPos);
        camera.lookAt(cameraTarget);
	}

}
