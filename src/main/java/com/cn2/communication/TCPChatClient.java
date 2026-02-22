package com.cn2.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.swing.JTextArea;

public class TCPChatClient { // class for the user "client" that sends the socket connection request
	
	private Socket socket; // define socket
	private BufferedReader bufferedReader; // define buffer bufferedReader, contains data sent from remote   
	private BufferedWriter bufferedWriter; // define buffer bufferedWriter, contains data local will send to remote  
	
	public TCPChatClient(Socket socket) { // conctructor TCPChatSender, initialize Socket
		
		try {
			this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); /* put InputStreamReader to bufferedReader, 
            InputStreamReader converts the input byte stream coming from socket to character stream */
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); /* put OutputStreamWriter to bufferedWriter, 
            OutputStreamWriter converts the output character stream sent to socket to byte stream */
		}
		catch (IOException e) { // in case of error
            e.printStackTrace();
            System.out.println("Error initializing");
            closeEverything(); // close streams
        }
	}
	
	public void send(String messageToRemote) { // method send, local sends text messageToRemote 
		
		try {
			bufferedWriter.write(messageToRemote); // messageToRemote to bufferedWriter 
			bufferedWriter.newLine(); // used to create line separator "\n" in buffer so we know when the messageToRemote is finished  
			bufferedWriter.flush(); // flush the stream when messageToRemote is finished 
		}
		catch (IOException e) { // in case of error
			e.printStackTrace();
			closeEverything(); // close streams
        }
	}
	
    public void receive(JTextArea textArea) { // method receive, local receives text messageFromRemote 
		
		new Thread(() -> { // Thread the receive text process 
			while (socket.isConnected()) { // while socket connection is established
				try {
					String messageFromRemote = bufferedReader.readLine(); // messageFromRemote the message remote sends to local
					
					if (messageFromRemote == null) { // check for null, remote closed the app
	                    textArea.append("remote: Disconnected." + "\n"); // inform local
	                    break; // break from loop 
	                }
					textArea.append("remote: " + messageFromRemote + "\n"); // appear messageFromRemote to textArea and change line
				}
				catch (IOException e) { // in case of error
					e.printStackTrace();
					closeEverything(); // close streams
					break; // break from loop 
				}
			}
	    }).start(); // start Thread  	
    }
	
	public void closeEverything() { // checking for null before closing streams to avoid a null pointer exception and closing streams
		
		try {
			if (socket != null && !socket.isClosed()) {  				
				socket.close(); // close socket 
			}	
			if (bufferedReader != null) {
				bufferedReader.close(); // close bufferedReader
			}	
		    if (bufferedWriter != null) {
				bufferedWriter.close(); // close bufferedWriter
		    }	
		 }
		 catch (IOException e) { // in case of error
			 e.printStackTrace();
		 }
		   
	}

}
