package com.cn2.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.sound.sampled.LineUnavailableException;

public class VoIP { // voice calls over UDP, port 1243

    private AudioPlayback playback;
    private AudioRecord record;
    private InetAddress remoteAddress;
    private DatagramSocket datagramSocket;
    private volatile boolean isCallActive = false; // read by both audio threads

    public VoIP(DatagramSocket datagramSocket, InetAddress remoteAddress) throws LineUnavailableException {
        this.playback = new AudioPlayback();
        this.record = new AudioRecord();
        this.remoteAddress  = remoteAddress;
        this.datagramSocket = datagramSocket;
    }

    // Sending and receiving run on two separate threads, so both directions work at the same time
    public void start() {
    	isCallActive = true;
		try {
			record.open();
			playback.open();

			new Thread(() -> { // capture and send
				try {
					byte[] sendAudioBuffer = new byte[1024];
					while (isCallActive) {
						sendAudioBuffer = record.read();
						DatagramPacket datagramPacket = new DatagramPacket(sendAudioBuffer, sendAudioBuffer.length, remoteAddress, 1243);
						datagramSocket.send(datagramPacket);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}).start();

			new Thread(() -> { // receive and play
				try {
					byte[] receiveAudioBuffer = new byte[1024];
					while (isCallActive) {
						DatagramPacket datagramPacket = new DatagramPacket(receiveAudioBuffer, receiveAudioBuffer.length);
						datagramSocket.receive(datagramPacket);
						playback.write(datagramPacket.getData());
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		isCallActive = false;
		record.close();
		playback.close();
	}
}
