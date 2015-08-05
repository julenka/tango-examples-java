/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.projecttango.experiments.videooverlaysample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoTextureCameraPreview;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.videooverlaysample.R;

import java.util.ArrayList;

/**
 * An example showing the usage of TangoCameraPreview class
 * Usage of TangoCameraPreviewClass:
 * To use this class, we first need initialize the TangoCameraPreview class with the activity's 
 * context and connect to the camera we want by using connectToTangoCamera class.Once the connection 
 * is established we need to manually update the TangoCameraPreview's texture by using the
 * onFrameAvailable callbacks.
 * Note:
 * To use TangoCameraPreview class we need to ask the user permissions for MotionTracking 
 * at the minimum level. This is because in Java all the call backs such as 
 * onPoseAvailable,onXyzIjAvailable, onTangoEvents, onFrameAvailable are set together at once. 
 */
public class MainActivity extends Activity {
    public static Object RenderLock;

    private TangoCameraPreview _tangoCameraPreview;
	private Tango _tango;

	private int _cameraType;
    private TextView _xyzDataInfo;
    private PCRenderer _pcRenderer;


    public MainActivity() {
        _cameraType = TangoCameraIntrinsics.TANGO_CAMERA_COLOR;
        RenderLock = new Object();
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_tangoCameraPreview = new TangoCameraPreview(this);
		_tango = new Tango(this);
		startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
                Tango.TANGO_INTENT_ACTIVITYCODE);

        setupUI();
	}

    private void setupUI() {
        setContentView(R.layout.activity_main);
        FrameLayout cameraPreviewFrame = (FrameLayout) findViewById(R.id.videoPreviewFrame);
        cameraPreviewFrame.addView(_tangoCameraPreview);
        _xyzDataInfo = (TextView) findViewById(R.id.xyzDataInfo);

        int maxDepthPoints = _tango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT)
                .getInt("max_point_cloud_elements");
        _pcRenderer = new PCRenderer(maxDepthPoints);
        GLSurfaceView glView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glView.setRenderer(_pcRenderer);
        glView.setZOrderOnTop(true);
        _pcRenderer.setFirstPersonView();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Motion Tracking Permissions Required!",
						Toast.LENGTH_SHORT).show();
				finish();
			} else {
				startCameraPreview();
			}
		}
        setUpExtrinsics();
	}

    private void setUpExtrinsics() {
        // Set device to imu matrix in Model Matrix Calculator.
        TangoPoseData device2IMUPose = new TangoPoseData();
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        try {
            device2IMUPose = _tango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "Tango Error", Toast.LENGTH_SHORT).show();
        }
        _pcRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
                device2IMUPose.getTranslationAsFloats(), device2IMUPose.getRotationAsFloats());

        // Set color camera to imu matrix in Model Matrix Calculator.
        TangoPoseData color2IMUPose = new TangoPoseData();

        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        try {
            color2IMUPose = _tango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "Tango Error", Toast.LENGTH_SHORT).show();
        }
        _pcRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
                color2IMUPose.getTranslationAsFloats(), color2IMUPose.getRotationAsFloats());
    }


    // Camera Preview
	private void startCameraPreview() {
	    // Connect to color camera
		_tangoCameraPreview.connectToTangoCamera(_tango,
                _cameraType);
		// Use default configuration for Tango Service.
		TangoConfig config = _tango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
		_tango.connect(config);
		
		// No need to add any coordinate frame pairs since we are not using 
		// pose data. So just initialize.
		ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
		_tango.connectListener(framePairs, new MyTangoUpdateListener(framePairs));
	}

    private String _xyzInfo;
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (RenderLock) {
                    _xyzDataInfo.setText(_xyzInfo);
                }
            }
        });
    }

	@Override
	protected void onPause() {
		super.onPause();
		_tango.disconnect();
	}

    class MyTangoUpdateListener implements OnTangoUpdateListener {
        private ArrayList<TangoCoordinateFramePair> _framePairs;
        public MyTangoUpdateListener(ArrayList<TangoCoordinateFramePair> framePairs) {
            _framePairs = framePairs;
        }
        @Override
        public void onPoseAvailable(TangoPoseData pose) {
            Log.d("JULIA", "pose");
            _pcRenderer.getModelMatCalculator().updateModelMatrix(
                    pose.getTranslationAsFloats(), pose.getRotationAsFloats());
            _pcRenderer.updateViewMatrix();
        }

        @Override
        public void onFrameAvailable(int cameraId) {

            // Check if the frame available is for the camera we want and
            // update its frame on the camera preview.
            if (cameraId == _cameraType) {
                _tangoCameraPreview.onFrameAvailable();
            }
        }

        @Override
        public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
            // We are not using OnPoseAvailable for this app
            _xyzInfo =
                    "rows: " + xyzIj.ijRows + "\n" +
                            "cols: " + xyzIj.ijCols + "\n" +
                            "count: " + xyzIj.xyzCount + "\n" +
                            "time: " + xyzIj.timestamp + "\n";
            updateUI();

            synchronized (RenderLock) {
                try {
                    TangoPoseData pointCloudPose = _tango.getPoseAtTime(xyzIj.timestamp,
                            _framePairs.get(0));
                    _pcRenderer.getPointCloud().UpdatePoints(xyzIj.xyz);
                    _pcRenderer.getModelMatCalculator().updatePointCloudModelMatrix(
                            pointCloudPose.getTranslationAsFloats(),
                            pointCloudPose.getRotationAsFloats());
                    _pcRenderer.getPointCloud().setModelMatrix(
                            _pcRenderer.getModelMatCalculator().getPointCloudModelMatrixCopy());
                } catch (TangoErrorException e) {
                    Toast.makeText(getApplicationContext(), "Tango Error",
                            Toast.LENGTH_SHORT).show();
                } catch (TangoInvalidException e) {
                    Toast.makeText(getApplicationContext(), "Tango Error",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onTangoEvent(TangoEvent event) {
            // We are not using OnPoseAvailable for this app
        }
    }
}
