package com.cn2.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JTextArea;

public class UDPChat { 
	
	 private InetAddress remoteAddress; 
	 private DatagramSocket datagramSocket;  
	 private byte[] sendBuffer = new byte[1024];     
	 private byte[] receiveBuffer = new byte[1024];
	 
	 public UDPChat(DatagramSocket datagramSocket, InetAddress remoteAddress) throws LineUnavailableException {
		 this.remoteAddress  = remoteAddress;
	     this.datagramSocket = datagramSocket;     
	 }
	 
	 public void send(String messageToRemote) throws LineUnavailableException { 
		 	 try {
		 		 sendBuffer = messageToRemote.getBytes(); 
		 		 DatagramPacket datagramPacket = new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress, 1234); 
		 		 datagramSocket.send(datagramPacket); 
		 	 }
		 	 catch (IOException e) {
		 		 e.printStackTrace();
		 	 }
	 
	 }
	 
	 public void receive(JTextArea textArea) throws LineUnavailableException { 
	 	 
	 	 new Thread(() -> { 
	 	 	 while (true) { 
	 	 	 	 try {
	 	 	 		 DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length); 
     	 	 		 datagramSocket.receive(datagramPacket);   
     	 	 		 String messageFromRemote = new String(datagramPacket.getData(), 0, datagramPacket.getLength()); 
     	 	 	     textArea.append("remote: " + messageFromRemote + "\n"); 
	 	 	 	 }
	     	 	 catch (IOException e) { 
	     	 		 e.printStackTrace();
	     	 	 } 
	 	 	 	 catch (Exception e) {
					e.printStackTrace();
				}	
	 	 	 }
	 	 }).start(); 
	 }
}
