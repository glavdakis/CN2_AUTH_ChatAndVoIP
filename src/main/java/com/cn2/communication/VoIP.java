package com.cn2.communication; 

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.sound.sampled.LineUnavailableException;

public class VoIP { // class for VoIP 
	
    private AudioPlayback playback; // define object for playing sound  
    private AudioRecord record; // define object for recording sound  
    private InetAddress remoteAddress; // define IP address remoteAddress, to set it as IP of remote 
    private DatagramSocket datagramSocket; // define DatagramSocket datagramSocket 
    private volatile boolean isCallActive = false; // VoIP call state
    public VoIP(DatagramSocket datagramSocket, InetAddress remoteAddress) throws LineUnavailableException {
    // conctructor VoIP, initialize datagramSocket and remoteAddress  
        
        this.playback = new AudioPlayback();
        this.record = new AudioRecord();
        this.remoteAddress  = remoteAddress;
        this.datagramSocket = datagramSocket;
    }
    
    public void start() { // method start, start VoIP call 
    	
    	isCallActive = true; // set isCallActive to true, change state when "End Call" is pressed
		try {
			record.open(); // call method open from AudioRecord, open targetLine-stream and start recording audio
			playback.open(); // call method open from AudioPlayback, open sourceLine-stream and start playing audio

			new Thread(() -> { // Thread the capture and send audio process
				try {
					byte[] sendAudioBuffer = new byte[1024]; // sendAudioBuffer, size=1024 bytes, to capture audio stream from microphone
					while (isCallActive) { // while VoIP call is happening
						sendAudioBuffer = record.read(); // sendAudioBuffer captures audio and returns byte stream 
						DatagramPacket datagramPacket = new DatagramPacket(sendAudioBuffer, sendAudioBuffer.length, remoteAddress, 1243); /* construct datagramPacket, 
						send packets of length of sendAudioBuffer, to IP remoteAddress and port=1243 of remote */  
						datagramSocket.send(datagramPacket); // datagramPacket send 
					}
				} 
				catch (Exception e) { // in case of error
					e.printStackTrace();
				}
			}).start(); // start Thread
			

			new Thread(() -> { // Thread the receive and play audio process
				try {
					byte[] receiveAudioBuffer = new byte[1024]; // receiveAudioBuffer, size=1024 bytes, to capture byte stream from remote 
					while (isCallActive) { // while VoIP call is happening
						DatagramPacket datagramPacket = new DatagramPacket(receiveAudioBuffer, receiveAudioBuffer.length); /* construct datagramPacket,
						receive packets of length of receiveAudioBuffer */  
						datagramSocket.receive(datagramPacket); // datagramPacket received from datagramSocket, blocking method 
						playback.write(datagramPacket.getData()); // call method write from AudioPlayback, write audio data from datagramPacket to sourceLine    
					}
				} 
				catch (Exception e) { // in case of error
					e.printStackTrace();
				}
			}).start(); // start Thread

		} 
		catch (Exception e) { // in case of error
			e.printStackTrace();
		}
	}

	public void stop() { // method stop, stop VoIP call 
		
		isCallActive = false; // set isCallActive to false, change state when "Call" is pressed
		record.close();  // call method close from AudioRecord, close targetLine-stream 
		playback.close();  // call method close from AudioPlayback, close sourceLine-stream 
	}
    
}
