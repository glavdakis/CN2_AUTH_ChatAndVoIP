package com.cn2.communication;

import java.io.*;
import java.net.*;

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

public class App extends Frame implements WindowListener, ActionListener {

	static TextField inputTextField;
	static JTextArea textArea;
	static JFrame frame;
	static JButton sendButton;
	static JTextField meesageTextField;
	public static Color gray;
	final static String newline="\n";
	static JButton callButton;

	private InetAddress remoteAddress;
	private UDPChat chatUDP;
	private VoIP voip;
	private boolean isCallActive = false;

	// To use the TCP chat instead of the UDP one:
	// 1) uncomment the chatTCP field and its constructor (TCPChatClient on one peer, TCPChatServer on the other)
	// 2) uncomment the TCP receive() in main
	// 3) in actionPerformed, comment out chatUDP.send and uncomment chatTCP.send
//	private TCPChatClient chatTCP;
//	private TCPChatServer chatTCP;

	{
	try {
		remoteAddress = InetAddress.getByName("localhost"); // IP address of the other peer
		chatUDP = new UDPChat(new DatagramSocket(1234), remoteAddress);
		voip = new VoIP(new DatagramSocket(1243), remoteAddress);
//		chatTCP = new TCPChatClient(new Socket("192.168.1.14", 2345));
//		chatTCP = new TCPChatServer(new ServerSocket(2345));
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

		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);

		sendButton.addActionListener(this);
		callButton.addActionListener(this);
	}

	public static void main(String[] args) throws LineUnavailableException {
		App app = new App("CN2 - AUTH");
		app.setSize(500,250);
		app.setVisible(true);

		app.chatUDP.receive(textArea);
//		app.chatTCP.receive(textArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == sendButton){

			String messageToSend  = inputTextField.getText();
			if (!messageToSend.isEmpty()) {
				try {
					chatUDP.send(messageToSend);
//					chatTCP.send(messageToSend);
					textArea.append("local: " + messageToSend  + newline);
					inputTextField.setText("");
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		else if (e.getSource() == callButton){

			String textAreaText = textArea.getText();
			if (!isCallActive) {
				if (textAreaText.contains("remote: Calling...Pick up!")) { // the remote is calling: answer
					try {
						String message = ("VoIP call started.");
						chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					String content = textArea.getText();
					content = content.replace("remote: Calling...Pick up!", "VoIP call started.");
					textArea.setText(content);
				}
				else { // start a call
					try {
						String message = ("Calling...Pick up!");
					    chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					textArea.append("Calling..." + newline);
				}
				callButton.setText("End Call");
				voip.start();
				isCallActive = true;
			}

			else {
				if (textAreaText.contains("remote: VoIP call ended.")) { // the remote already ended the call
					String content = textArea.getText();
					content = content.replace("remote: VoIP call ended.", "VoIP call ended.");
					textArea.setText(content);
				}
				else { // end the call and tell the remote
					try {
						String message = ("VoIP call ended.");
						chatUDP.send(message);
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					textArea.append("VoIP call ended."+ newline);
				}
				callButton.setText("Call");
				voip.stop();
				isCallActive = false;

				String twoContent = textArea.getText();
				twoContent = twoContent.replace("Calling...", "");
				textArea.setText(twoContent);
			}
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
//	            chatTCP.closeEverything();
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
