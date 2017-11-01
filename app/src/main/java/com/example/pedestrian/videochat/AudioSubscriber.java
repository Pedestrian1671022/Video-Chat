package com.example.pedestrian.videochat;

/**
 * Created by pedestrian-username on 17-10-18.
 */

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import audio_common_msgs.AudioData;

public class AudioSubscriber extends AbstractNodeMain {

    AudioTrack audioTrack;
    public static final int SAMPLE_RATE = 16000;
    public String topicName;
    AudioManager audioManager;

    public AudioSubscriber(String topicName, AudioManager audioManager) {
        this.topicName = topicName;
        this.audioManager = audioManager;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void onShutdown(Node node) {
        if(audioTrack != null){
            audioTrack.stop();
        }
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)-5, 0);
        audioManager.setSpeakerphoneOn(true);

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        final int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();

        if (NoiseSuppressor.isAvailable() && audioTrack != null) {
            NoiseSuppressor ns = NoiseSuppressor.create(audioTrack.getAudioSessionId());
            if (ns != null) {
                ns.setEnabled(true);
            }
        }

        Subscriber<AudioData> subscriber = connectedNode.newSubscriber(topicName, AudioData._TYPE);

        subscriber.addMessageListener(new MessageListener<AudioData>() {
                    @Override
                    public void onNewMessage(AudioData message) {
                        byte[] buffer = message.getData().array();
                        audioTrack.write(buffer, 4, buffer.length-4);
                    }
                });
    }
}
