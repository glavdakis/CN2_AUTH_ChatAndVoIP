package com.cn2.communication;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;			
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;		
	
	static JButton passButton;	//button to change AES key
//	static JButton ipButton;	//button to change remote IP
	static JButton clearButton;	//button to clear chat
	
	// define network variables 
	private InetAddress remoteAddress; // define IP address remoteAddress, to set it as IP of remote 
	private UDPChat chatUDP; // define UDPChat object for UDP Chat 
	private VoIP voip; // define VoIP object for VoIP 
	private boolean isCallActive = false; // VoIP call not happening

	// A note for tcp
	// If you plan to use it:
	// 1) uncomment the appropriate object
	// 2) uncomment the appropriate constructor
	// 3) uncomment the TCP receive()
	// 4) comment the UDP SEND area
	// 5) uncomment the TCP SEND area
	
//	private TCPChatClient chatTCP; // define TCPChat object for TCP Chat, if local is the "client"
//	private TCPChatServer chatTCP; // define TCPChat object for TCP Chat, if local is the "server"
	
	// define AES variable	(AES + *ci*pher -> AESci)
	public AESci aesci;
	
	{ // initialize network variables using non-static initialization block
	
	try {
		remoteAddress = InetAddress.getByName("localhost"); // initialize remoteAddress, IP of remote
		chatUDP = new UDPChat(new DatagramSocket(1234), remoteAddress); /* initialize chatUDP,
//		pass DatagramSocket from port 1234 and remoteAddress to constructor UDPChat */ 
		voip = new VoIP(new DatagramSocket(1243), remoteAddress); /* initialize voip,
		pass DatagramSocket from port 1243 and remoteAddress to constructor VoIP */
		
		// initialize chatTCP, pass Socket from port 2345 and IP of remote to constructor TCPChatSender 
//		chatTCP = new TCPChatClient(new Socket(remoteAddress, 2345)); 
		// initialize chatTCP, pass ServerSocket from port 2345 to constructor TCPChatReceiver 
//		chatTCP = new TCPChatServer(new ServerSocket(2345));
	}
	catch (Exception e) { // in case of error
		e.printStackTrace();
		System.exit(1);
	}
	
	// initialize AES variable
	try {
		aesci = new AESci();
	}
	catch (Exception e) { // in case of error
		e.printStackTrace();
		System.exit(1);
	}
	
	
	}

	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);								
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");
		passButton = new JButton("Set Pass");
//		ipButton = new JButton("Set Remote IP");
		clearButton = new JButton("Clear Chat");
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);							
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(passButton);
//		add(ipButton);
		add(clearButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		passButton.addActionListener(this);
//		ipButton.addActionListener(this);
		clearButton.addActionListener(this);
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 * @throws LineUnavailableException  
	 */
	public static void main(String[] args) throws LineUnavailableException {
		
		/*
		 * 1. Create the app's window
		 */
		App app = new App("CN2 - AUTH");
		app.setSize(500,250);				  
		app.setVisible(true);			  

		/*
		 * 2. Start receiving Chat messages
		 */
		app.chatUDP.receive(textArea, app.aesci);  // call method receive from chatUDP, receive text data
		
		// TCP isn't encrypted
		// Keep it commented if TCP isn't used
//		app.chatTCP.receive(textArea); // call method receive from TCPChatSender or TCPChatRceiver, receive text data
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		/*
		 * Check which button was clicked.
		 */
		
		if (e.getSource() == sendButton){ // The "Send" button was clicked
			
			String messageToSend  = inputTextField.getText(); // get string messageToSend from TextField inputTextField 
		// All of this if should be commented out if we plan to use TCP (TCP uses ~64Kb max buffer, we don't need chunks)
			//UDP SEND [START]
				if (!messageToSend.isEmpty()) { // if there is a messageToSend 
					try {
						String plainMessage = messageToSend; //stores the message in plaintext temporarily
						
						if (messageToSend.length() < 501) {	// if message is under 500 chars it leaves as a single packet
							aesci.exportKeys();	// debug, prints the key and IV of the cipher we will use on the console
							
							messageToSend = aesci.encryptMessage(messageToSend,1); //encrypts the message to be send
							chatUDP.send(messageToSend); // call method send from chatUDP, send text data
							
							aesci.exportIV();	// debug, prints the new IV we created on the console
						}
						else if (messageToSend.length() < 50001) {	// texts over 500 chars will be split and sent in chunks
							int chunkSize = 500; // Sets the maximum chunk size
							// Debug, calculates the number of iterations needed to process the entire string:
	//						int numIterations = messageToSend.length() / chunkSize;
	//						if (messageToSend.length() % chunkSize > 0) {
	//						    numIterations++;
	//						}
							int j = 0; // Chunk counter
							String part;
							part = "[Part]"; // Identification tag for the decryption method
							// Iterate over the string in chunks of a specified size
							for (int i = 0; i < messageToSend.length(); i += chunkSize) {
								
								// Calculate the end index of the current chunk
								int endIndex = i + chunkSize;	// 0, 500, 1000, 1500...
								// If the end index is greater than the length of the string, set it to the length of the string
							    if (endIndex > messageToSend.length()) {
							        endIndex = messageToSend.length();
							    }
							    
							    // Extract and *encrypt* the string chunk from the original string
							    // Note: for each encryption, it does NOT generate a new IV, hence the ", 0"
							    if (j<10) {
	//						    	part = ("[Part]0" + j + aesci.encryptMessage(messageToSend.substring(i, endIndex)) );
							    	part = (aesci.encryptMessage("[Part]0" + j + messageToSend.substring(i, endIndex) , 0) );
							    	aesci.exportIV();	// debug, prints the new IV we created on the console
	//						    	Thread.sleep(150); // Waits 150ms for each packet to be sent before sending the next
							    }
							    else {
							    	part = (aesci.encryptMessage("[Part]" + j + messageToSend.substring(i, endIndex) , 0) );
							    	aesci.exportIV();	// debug, prints the new IV we created on the console
	//						    	Thread.sleep(150); // Waits 150ms for each packet to be sent before sending the next
							    }
							    // Result format is as such: <IV:16chars>[Part]00<encrypted-text>
							    // Alternative, needs locale library to set locale and be consistent:
								// part = (aesci.encryptMessage("[Part]" + String.format("%02d", j) + messageToSend.substring(i, endIndex) , 0) );
								
							    System.err.println("Chunk[" + j + "]: " + part + "\n"); // debug, prints each chunk on the console
							    chatUDP.send(part); // sends the chunk
							    j++; // Adds +1 to the chunk counter
								}
							Thread.sleep(700); // Waits 700ms for all the packets to be sent before sending the next command
							
							// Sends the command to the remote confirming that local finished sending chunks
							// Forces the remote to combine the received chunks and print them on their screen
							chatUDP.send(aesci.encryptMessage("[Part]FINISHED", 1) ); 
						}
						else if (messageToSend.length() > 50000) {	// edge case where user sends >50k char message
							textArea.append("Message to big. Please send 50000 chars max.\nYour message was not send.");
						}
	
						textArea.append("local: " + plainMessage  + newline); // appear plainMessage to textArea and change line
						inputTextField.setText(""); // erase messageTosend from inputTextField					
					}
					catch (Exception ex) { // in case of error
						ex.printStackTrace();
					}
				}
			//UDP SEND [END]
			
//			//TCP SEND [START]
//				try {
//					String plainMessage = messageToSend;
//				// Checks needed for TCP, this whole if should be commented if we plan to use UDP
//				// TCP can send ~64Kb packets easily, a lot bigger than what we normally need for this app
//					if (!messageToSend.isEmpty() || (messageToSend.length() > 50000) ) { // if there is a messageToSend 
//							chatTCP.send(messageToSend); // call method send from chatTCP, send text data
//					}
//					else {
//						textArea.append("Message to big. Please send 50000 chars max.\nYour message was not send.");
//					}
//				textArea.append("local: " + plainMessage  + newline); // appear plainMessage to textArea and change line
//				inputTextField.setText(""); // erase messageTosend from inputTextField					
//				}
//				catch (Exception ex) { // in case of error
//					ex.printStackTrace();
//				} 
//			//TCP SEND [END]
			
			
		}
		
		else if (e.getSource() == callButton){ // The "Call" button was clicked
			
			// the [Voice-Call] tag is recognised by the UDPChat.receive method
			String textAreaText = textArea.getText(); // get the text from textArea
			if (!isCallActive) { // VoIP call happening 
				if (textAreaText.contains("remote: [Voice-Call] Calling...Pick up!")) { // if remote starts call
					try {
						String message = ("[Voice-Call] VoIP call started."); // inform remote local has picked up, call started
						chatUDP.send(message); // by sending message
					} 
					catch (Exception ex) { // in case of error
						ex.printStackTrace();
					}
					String content = textArea.getText(); // get the text from textArea
					content = content.replace("remote: [Voice-Call] Calling...Pick up!","[Voice-Call] VoIP call started."); // replace the specific text
					textArea.setText(content); 
				}
				else { // if local starts call
					try {
						String message = ("[Voice-Call] Calling...Pick up!"); // inform remote local is calling
					    chatUDP.send(message); // by sending message
					} 
					catch (Exception ex) { // in case of error
						ex.printStackTrace();
					}
					textArea.append("[Voice-Call] Calling..." + newline); // appear "Calling..." to textArea and change line
				}
				callButton.setText("End Call"); // change button to End Call
				voip.start(); // call method start from VoIP and start VoIP call
				isCallActive = true; // change state when "End Call" is pressed 
			} 
			
			else { // VoIP call not happening
				if (textAreaText.contains("remote: [Voice-Call] VoIP call ended.")) { // if remote ended call
					String content = textArea.getText(); // get the text from textArea
					content = content.replace("remote: [Voice-Call] VoIP call ended.", "[Voice-Call] VoIP call ended."); // replace the specific text
					textArea.setText(content);
				} 
				else { // if local ended call
					try {
						String message = ("[Voice-Call] VoIP call ended."); // inform remote local has stopped the call
						chatUDP.send(message); // by sending message
					} 
					catch (Exception ex) { // in case of error
						ex.printStackTrace();
					}
					textArea.append("[Voice-Call] VoIP call ended."+ newline); // appear "VoIP call ended." to textArea and change line
				}
				callButton.setText("Call"); // change button to Call
				voip.stop(); // call method stop from VoIP and stop VoIP call
				isCallActive = false; // change state when "Call" is pressed
				
				String twoContent = textArea.getText(); // get the text from textArea
				twoContent = twoContent.replace("[Voice-Call] Calling...", ""); // remove the specific text
				textArea.setText(twoContent);							
			}
		}	
		
		else if (e.getSource() == passButton){	// if user wants to change password-AES key (both should have the same)
			String pass  = inputTextField.getText();
			if (!pass.isEmpty() && !(pass.length() < 6)) {	// checks if password is empty or under 6 chars
				aesci.exportKeys(); // debug, prints key and IV on the console
				
				try {
					aesci.initFromPassword(pass);
				} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				aesci.exportKeys(); // debug, prints key and IV on the console
				textArea.append("Password changed to : " + pass.substring(0, 1) + "***" + pass.substring((pass.length()-1), pass.length())  + newline);
//				inputTextField.getText(""); //for extra protection we can remove the password from the textfield
			}
			else {
				textArea.append("Please put a password (over 5 chars) in the input Field before pressing the button" + newline);
			}
		}
		
		else if (e.getSource() == clearButton){ // Clears the chat area
			textArea.setText("Text Cleared :)" + newline);
			inputTextField.setText("");
			
		}
		
//		// experimental
//		else if (e.getSource() == ipButton){ // Used to change the IP of the remote (should be used mainly with UDP)
//			String newIP  = inputTextField.getText();
//			if (!newIP.isEmpty()) { // checks if IP is empty
////				try {
//////					UDPChat.setIP(InetAddress.getByName(newIP));
//////					remoteAddress = InetAddress.getByName(newIP);
//////					setIP(newIP); // sets new remote IP to the contents of the inputTextField
////				} catch (UnknownHostException e1) {
////					// TODO Auto-generated catch block
////					e1.printStackTrace();
////				}
//				textArea.append("New remote IP set to: " + newIP + newline); 
//			}
//			else {
//				textArea.append("Can't set IP to blank address" + newline); 
//			}
//		}
		
		
	
//		ABANDONED: WOULD BE USED TO DYNAMICALLY CHANGE THE TYPE OF THE CONNECTION (too complicated)
//		else if (e.getSource() == protocolButton){
//			String prot  = inputTextField.getText();
//			try {
//				setProtocol(prot);
//			} catch (SocketException | LineUnavailableException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
	}
	
//	@Override
	// Sets new remote IP (called when ipButton is pressed)
//	public void setIP(String IPaddress) throws UnknownHostException {
//		remoteAddress = InetAddress.getByName(IPaddress);
//	}
//  ABANDONED (SAME USE AND REASON AS ABOVE)	
//	public void setProtocol(String prot) throws SocketException, LineUnavailableException{
//		switch(prot) {
//		  case "UDP":
//			  chatUDP = new UDPChat(new DatagramSocket(1234), remoteAddress); /* initialize chatUDP,
//				pass DatagramSocket from port 1234 and remoteAddress to constructor UDPChat */ 
//		    break;
//		  case "TCPClient":
//		    // code block
//		    break;
//		  case "TCPServer":
//			    // code block
//			    break;
//		  default:
//			  textArea.append("To set a protocol either type UDP or TCPClient or TCPServer" + newline);
//		}
//	}
	
	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) { // close the app
//		try {
//	        if (chatTCP != null) {
//	        	chatTCP.closeEverything(); // close streams
//	        }
//	    }
//		catch (Exception ex) { // in case of error
//	        ex.printStackTrace();
//	    } 
//		finally { // always executed
	        dispose();
	        System.exit(0);
//	    }
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
	
	
}




