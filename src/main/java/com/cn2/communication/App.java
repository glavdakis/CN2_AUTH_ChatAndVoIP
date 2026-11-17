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

	static TextField inputTextField;
	static JTextArea textArea;
	static JFrame frame;
	static JButton sendButton;
	static JTextField meesageTextField;
	public static Color gray;
	final static String newline="\n";
	static JButton callButton;
	static JButton passButton;
	static JButton clearButton;

	private InetAddress remoteAddress;
	private UDPChat chatUDP;
	private VoIP voip;
	private boolean isCallActive = false;

	// To use the TCP chat instead of the UDP one:
	// 1) uncomment the chatTCP field and its constructor (TCPChatClient on one peer, TCPChatServer on the other)
	// 2) uncomment the TCP receive() in main
	// 3) comment out the UDP SEND area and uncomment the TCP SEND area
//	private TCPChatClient chatTCP;
//	private TCPChatServer chatTCP;

	public AESci aesci;

	{
	try {
		remoteAddress = InetAddress.getByName("localhost"); // IP address of the other peer
		chatUDP = new UDPChat(new DatagramSocket(1234), remoteAddress);
		voip = new VoIP(new DatagramSocket(1243), remoteAddress);
//		chatTCP = new TCPChatClient(new Socket(remoteAddress, 2345));
//		chatTCP = new TCPChatServer(new ServerSocket(2345));
	}
	catch (Exception e) {
		e.printStackTrace();
		System.exit(1);
	}

	try {
		aesci = new AESci();
	}
	catch (Exception e) {
		e.printStackTrace();
		System.exit(1);
	}
	}

	public App(String title) {
		super(title);
		gray = new Color(254, 254, 254);
		setBackground(gray);
		setLayout(new FlowLayout());
		addWindowListener(this);

		inputTextField = new TextField();
		inputTextField.setColumns(20);

		textArea = new JTextArea(10,40);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		sendButton = new JButton("Send");
		callButton = new JButton("Call");
		passButton = new JButton("Set Pass");
		clearButton = new JButton("Clear Chat");

		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(passButton);
		add(clearButton);

		sendButton.addActionListener(this);
		callButton.addActionListener(this);
		passButton.addActionListener(this);
		clearButton.addActionListener(this);
	}

	public static void main(String[] args) throws LineUnavailableException {
		App app = new App("CN2 - AUTH");
		app.setSize(500,250);
		app.setVisible(true);

		app.chatUDP.receive(textArea, app.aesci);
//		app.chatTCP.receive(textArea); // TCP messages are not encrypted
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == sendButton){

			String messageToSend  = inputTextField.getText();
			//UDP SEND [START]
				if (!messageToSend.isEmpty()) {
					try {
						String plainMessage = messageToSend;

						if (messageToSend.length() < 501) { // up to 500 chars: a single packet
							messageToSend = aesci.encryptMessage(messageToSend,1);
							chatUDP.send(messageToSend);
						}
						else if (messageToSend.length() < 50001) { // longer messages are sent in chunks of 500 chars
							int chunkSize = 500;
							int j = 0;
							for (int i = 0; i < messageToSend.length(); i += chunkSize) {
								int endIndex = Math.min(i + chunkSize, messageToSend.length());

							    // Each chunk starts with a two-digit "[Part]00" tag, so the receiver can put them back in order.
							    // All chunks keep the same IV (", 0"), so chunks that arrive out of order can still be decrypted.
							    String number = (j < 10 ? "0" : "") + j;
							    String part = aesci.encryptMessage("[Part]" + number + messageToSend.substring(i, endIndex), 0);
							    chatUDP.send(part);
							    j++;
								}
							Thread.sleep(700); // give the chunks time to arrive before the FINISHED command

							// Tells the remote that all chunks were sent, so it can assemble the message
							chatUDP.send(aesci.encryptMessage("[Part]FINISHED", 1) );
						}
						else if (messageToSend.length() > 50000) {
							textArea.append("Message to big. Please send 50000 chars max.\nYour message was not send.");
						}

						textArea.append("local: " + plainMessage  + newline);
						inputTextField.setText("");
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			//UDP SEND [END]

			//TCP SEND [START]
//				try {
//					String plainMessage = messageToSend;
//					if (!messageToSend.isEmpty() || (messageToSend.length() > 50000) ) {
//							chatTCP.send(messageToSend);
//					}
//					else {
//						textArea.append("Message to big. Please send 50000 chars max.\nYour message was not send.");
//					}
//				textArea.append("local: " + plainMessage  + newline);
//				inputTextField.setText("");
//				}
//				catch (Exception ex) {
//					ex.printStackTrace();
//				}
			//TCP SEND [END]
		}

		else if (e.getSource() == callButton){

			// Call commands start with "[Voice-Call]" and are sent unencrypted
			String textAreaText = textArea.getText();
			if (!isCallActive) {
				if (textAreaText.contains("remote: [Voice-Call] Calling...Pick up!")) { // the remote is calling: answer
					try {
						String message = ("[Voice-Call] VoIP call started.");
						chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					String content = textArea.getText();
					content = content.replace("remote: [Voice-Call] Calling...Pick up!","[Voice-Call] VoIP call started.");
					textArea.setText(content);
				}
				else { // start a call
					try {
						String message = ("[Voice-Call] Calling...Pick up!");
					    chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					textArea.append("[Voice-Call] Calling..." + newline);
				}
				callButton.setText("End Call");
				voip.start();
				isCallActive = true;
			}

			else {
				if (textAreaText.contains("remote: [Voice-Call] VoIP call ended.")) { // the remote already ended the call
					String content = textArea.getText();
					content = content.replace("remote: [Voice-Call] VoIP call ended.", "[Voice-Call] VoIP call ended.");
					textArea.setText(content);
				}
				else { // end the call and tell the remote
					try {
						String message = ("[Voice-Call] VoIP call ended.");
						chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					textArea.append("[Voice-Call] VoIP call ended."+ newline);
				}
				callButton.setText("Call");
				voip.stop();
				isCallActive = false;

				String twoContent = textArea.getText();
				twoContent = twoContent.replace("[Voice-Call] Calling...", "");
				textArea.setText(twoContent);
			}
		}

		else if (e.getSource() == passButton){ // both peers must set the same password
			String pass  = inputTextField.getText();
			if (!pass.isEmpty() && !(pass.length() < 6)) {
				try {
					aesci.initFromPassword(pass);
				} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
					e1.printStackTrace();
				}
				textArea.append("Password changed to : " + pass.substring(0, 1) + "***" + pass.substring((pass.length()-1), pass.length())  + newline);
			}
			else {
				textArea.append("Please put a password (over 5 chars) in the input Field before pressing the button" + newline);
			}
		}

		else if (e.getSource() == clearButton){
			textArea.setText("Text Cleared :)" + newline);
			inputTextField.setText("");
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
//		try {
//	        if (chatTCP != null) {
//	        	chatTCP.closeEverything();
//	        }
//	    }
//		catch (Exception ex) {
//	        ex.printStackTrace();
//	    }
//		finally {
	        dispose();
	        System.exit(0);
//	    }
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}
}
