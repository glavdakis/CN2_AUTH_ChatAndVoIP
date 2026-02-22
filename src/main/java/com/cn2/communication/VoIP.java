package com.cn2.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.sound.sampled.LineUnavailableException;

public class VoIP {  
	
    private AudioPlayback playback;   
    private AudioRecord record;   
    private InetAddress remoteAddress;  
    private DatagramSocket datagramSocket;  
    private volatile boolean isCallActive = false; 
    
    public VoIP(DatagramSocket datagramSocket, InetAddress remoteAddress) throws LineUnavailableException { 
        this.playback = new AudioPlayback();
        this.record = new AudioRecord();
        this.remoteAddress  = remoteAddress;
        this.datagramSocket = datagramSocket;
    }
    
    public void start() {  
    	isCallActive = true; 
		try {
			record.open(); 
			playback.open(); 

			new Thread(() -> { // Thread the capture and send audio process
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
			

			new Thread(() -> { // Thread the receive and play audio process
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
