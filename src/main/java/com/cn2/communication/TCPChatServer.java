package com.cn2.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JTextArea;

public class TCPChatServer { // class for the user that accepts the socket connection request
	
	private ServerSocket serverSocket; // waits for requests for connection on a port and creates socket 
	private Socket socket; 
	private BufferedReader bufferedReader;   
	private BufferedWriter bufferedWriter;   
	
	public TCPChatServer(ServerSocket serverSocket) { 
		
		try {
			this.serverSocket = serverSocket;
            this.socket = serverSocket.accept(); 
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); 
		}
		catch (IOException e) { 
            e.printStackTrace();
            System.out.println("Error initializing");
            closeEverything(); 
        }
	}
	
    public void send(String messageToRemote) {  
    	
    	try {
			bufferedWriter.write(messageToRemote);  
			bufferedWriter.newLine();  
			bufferedWriter.flush(); 
		}
		catch (IOException e) { 
			e.printStackTrace();
			closeEverything(); 
		}
	}
	
	public void receive(JTextArea textArea) {  
		
		new Thread(() -> { 
			while (socket.isConnected()) { 
				try {
					String messageFromRemote = bufferedReader.readLine(); 

					if (messageFromRemote == null) { 
	                    textArea.append("remote: Disconnected." + "\n"); 
	                    break; 
	                }
					textArea.append("remote: " + messageFromRemote + "\n"); 
				}
				catch (IOException e) { 
					e.printStackTrace();
					closeEverything(); 
					break;  
				}
			}
	    }).start();   	
    }
	
	public void closeEverything() { 
		try {
			if (socket != null && !socket.isClosed()) { 				
				socket.close();  
			}
			if (serverSocket != null && !serverSocket.isClosed()) { 				
				serverSocket.close();  
			}
			if (bufferedReader != null) {
				bufferedReader.close(); 
			}	
		    if (bufferedWriter != null) {
				bufferedWriter.close(); 
		    }	
		 }
		 catch (IOException e) { 
			 e.printStackTrace();
		 }
	}	
	
}
