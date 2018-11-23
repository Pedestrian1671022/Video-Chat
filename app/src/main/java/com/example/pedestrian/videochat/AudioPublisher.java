package com.example.pedestrian.videochat;

/**
 * Created by pedestrian-username on 17-10-18.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;

import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.nio.ByteOrder;

import rosjava_custom_msg.CustomMessage;


public class AudioPublisher extends AbstractNodeMain {

    public String topicName;

    public AudioPublisher(String topicName) {
        this.topicName = topicName;
    }

    AudioRecord audioRecord;
    public static final int SAMPLE_RATE = 16000;


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void onShutdown(Node node) {
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        final int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor ns = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            ns.setEnabled(true);
        }

        audioRecord.startRecording();

        final Publisher<CustomMessage> publisher = connectedNode.newPublisher(topicName, CustomMessage._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            final byte[] buffer = new byte[bufferSize*2];

            @Override
            protected void setup() {
            }

            @Override
            protected void loop() throws InterruptedException {
                audioRecord.read(buffer, 0, bufferSize*2);
                CustomMessage data = publisher.newMessage();
                data.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, buffer, 0, bufferSize*2));
                publisher.publish(data);
            }
        });
    }
}