package com.example.pedestrian.videochat;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import sensor_msgs.CompressedImage;

public class MainActivity extends RosActivity {

    private AudioPublisher audioPublisher;
    private AudioManager audioManager;
    private AudioSubscriber audioSubscriber;

    private RosImageView<CompressedImage> android_videoSubscriber;
    private RosCameraPreviewView android_videoPublisher;
    private int cameraId = 1;

    private VirtualJoystickView virtualJoystickView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioPublisher = new AudioPublisher("android_audioPublisher");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioSubscriber = new AudioSubscriber("android_audioSubscriber", audioManager);

        android_videoSubscriber = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.android_videoSubscriber);
        android_videoSubscriber.setTopicName("/camera/rgb/image_color/compressed");
        android_videoSubscriber.setMessageType(sensor_msgs.CompressedImage._TYPE);
        android_videoSubscriber.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        android_videoPublisher = (RosCameraPreviewView) findViewById(R.id.android_videoPublisher);
        android_videoPublisher.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    cameraId = (cameraId + 1) % 2;
                    android_videoPublisher.releaseCamera();
                    android_videoPublisher.setCamera(Camera.open(cameraId));
                }
                return true;
            }
        });

        virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtualJoystick);
        virtualJoystickView.setTopicName("/mobile_base/commands/velocity");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(audioPublisher, nodeConfiguration);
        nodeMainExecutor.execute(audioSubscriber, nodeConfiguration);

        nodeMainExecutor.execute(android_videoSubscriber, nodeConfiguration);

        android_videoPublisher.setCamera(Camera.open(cameraId));
        nodeMainExecutor.execute(android_videoPublisher, nodeConfiguration);

        nodeMainExecutor.execute(virtualJoystickView, nodeConfiguration.setNodeName("android_velocityPublisher"));
    }

    public MainActivity() {
        super("Communication", "Communication");
    }
}
