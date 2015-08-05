package com.projecttango.experiments.videooverlaysample;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.projecttango.tangoutils.Renderer;
import com.projecttango.tangoutils.renderables.PointCloud;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by julenka on 8/5/15.
 */
public class PCRenderer extends Renderer implements GLSurfaceView.Renderer {
    private PointCloud _pointCloud;
    private int _maxDepthPoints;

    public PCRenderer(int maxDepthPoints) {
        _maxDepthPoints = maxDepthPoints;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d("JULIA", "surface created");
        GLES20.glClearColor(0,0,0, 0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        _pointCloud = new PointCloud(_maxDepthPoints);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, 5f, 5f, 5f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d("JULIA", "surfaceChanged");
        GLES20.glViewport(0, 0, width, height);
        mCameraAspect = (float) width / height;
        Matrix.perspectiveM(mProjectionMatrix, 0, CAMERA_FOV, mCameraAspect, CAMERA_NEAR,
                CAMERA_FAR);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        synchronized (MainActivity.RenderLock) {
            _pointCloud.draw(mViewMatrix, mProjectionMatrix);
        }
        Log.d("JULIA", _pointCloud.getPointCount() + ", " + _pointCloud.getAverageZ());
    }

    public PointCloud getPointCloud() {
        return _pointCloud;
    }
}
