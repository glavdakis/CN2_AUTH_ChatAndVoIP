package com.cn2.communication;

import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;	//for generating key with password

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

//import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;	//for generating key with password
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;	//for generating key with password
import java.util.Arrays;
import java.util.Base64;

public class AESci {
    private SecretKey key;				// the AES key
 // private final variable: its value is constant throughout the execution of the program and can only be accessed within the class where it is defined
    private final int KEY_SIZE = 128;	// the size of the AES key
    private final int T_LEN = 128;		// the length of the tag used in the Galois/Counter Mode (GCM) of operation in the AES encryption algorithm
    									// In GCM, a tag is appended to the ciphertext during encryption, and it is used during decryption to verify the integrity of the ciphertext
    private byte[] IV = new byte[12];	// Initialization Vector: This is the IV (the real IV) our cipher uses, we will call it IV
    									// it is essentially the ephemeral key that changes after every encryption
    private String salt = "potato";		// the salt used to generate the key from our password
    private String encodedIV = "";		// the base64 encoded string version of the IV
    private String ivstr = "";			// the base64 encoded string containing the new IV, before it is assigned to IV
    public String[] bufferedMes = new String[100];	// the buffer to contain messages over 500chars in plaintext
    												// Should hold 100 strings max of 500 chars max (50k chars max total) 
//        public int counter = 0;		// debug, used to validate that the class is callled properly 
    
    // constructor
    public AESci() throws Exception {
    	initFromPassword("VerySecurePassword");	// Sets key
//    	IV = new byte[12]; // Create a byte array of length 10
    	Arrays.fill(IV, (byte) 1);
    	ivstr = encode(IV);
//    	IVgen(); //Generates IV string and assigns it to ivstr variable
//    	setIV(ivstr); //Assigns IV stored in ivstr to the real IV
    }
    


    // Generates key from password
    public void initFromPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
    	// Creates a SecretKeyFactory instance for the PBKDF2WithHmacSHA256 algorithm
    	SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    	// Create a PBEKeySpec object with the password, salt, iteration count, and key size
    	// Password is converted to char array, while salt to byte array
    	// Salt and iteration counts help prevent dictionary attacks and brute force attacks
    	PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
    	// Creates a SecretKeySpec object (our key) with the generated PBEKeySpec object and the AES algorithm
    	key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
    
    // Generates new and random IV (should be called after every encryption, it should be ephemeral)
    // The new IV should be supplied in the beginning of the sent encrypted message, *before* be put in use
    // The receiver should decrypt the message with the old IV, extract the new IV from the message and use that for the next message
    // The base64 IV we generate is 16 chars (should be the first 16 chars of a normal encrypted message)
    public void IVgen() {
    SecureRandom random = new SecureRandom();	// Creates the random seed
    byte[] iv2 = new byte[12];	// Creates a temporary 12-byte array (iv2)
    random.nextBytes(iv2);		// Fills the temporary iv2 array with 12 bytes (nextBytes() is a method of SecureRandom)
    ivstr = encode(iv2); 		// Encodes the iv as a base64 string and assigns it to ivstr variable
//    System.err.println(ivstr);	// Debug, prints ivstr on the console
//    this.IV = decode(ivstr); 	// Unecessary, used to directly assign the byte array to the real IV upon generating a new
    }
    
  // Sets the IV from a base64 string
  public void setIV(String IVnew) {
	  	if (!IVnew.isEmpty()) { // Checks if the string is empty
	  		this.IV = decode(IVnew);	// Decodes the base64 string to a byte array and assigns it to IV
  	}
  }


    // Encryps text
    public String encrypt(String message) throws Exception {
    	byte[] messageInBytes = message.getBytes(); // Encodes and assings string message to a byte array to be encrypted
//        byte[] messageInBytes = message.getBytes(StandardCharsets.UTF_8); // This didn't work somehow
        Cipher encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creates a new AES/GCM/NoPadding Cipher object
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV); // Creates a GCMParameterSpec object with the given tag length (T_LEN) and IV
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key, spec); // Initialize the cipher in *encryption* mode with the AES key and the GCMParameterSpec
        byte[] encryptedBytes = encryptionCipher.doFinal(messageInBytes); // Encrypts the message using the cipher
        return encode(encryptedBytes); // Returns a base64 encoded string of the message
    }

    // Decrypts text
    public String decrypt(String encryptedMessage) throws Exception {
        byte[] messageInBytes = decode(encryptedMessage); // Decodes and assings base64 string message to a byte array to be decrypted
        Cipher decryptionCipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creates a new AES/GCM/NoPadding cipher object for decryption
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV); // Creates a GCMParameterSpec object with the same tag length and IV used for encryption
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec); // Initializes the cipher in decryption mode with the AES key and the GCMParameterSpec object
        byte[] decryptedBytes = decryptionCipher.doFinal(messageInBytes); // Decrypts the message using the cipher
//        return new String(decryptedBytes, StandardCharsets.UTF_8); // This didn't work somehow
        return new String(decryptedBytes); // Returns decrypted string of the message
    }

    // Converts byte array to Base64 string (to be readable and printed)
    private String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    
    // Converts Base64 string to byte array (to be used in cipher mainly)
    private byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    // Prints Key and IV encoded in base64 on the console
    public void exportKeys() {
    	System.err.println("Key (base64 encoded): " + encode(key.getEncoded()));
    	System.err.println("IV (base64 encoded): " + encode(IV));
//        System.err.println(key.getEncoded()); // bare byte array
//        System.err.println(IV); 				// bare byte array
    }
    
    // Prints IV encoded in base64 on the console
    public void exportIV() {
    	System.err.println("IV (base64 encoded): " + encode(IV));
    }
    
    // Prints Key encoded in base64 on the console
    public void exportKey() {
    	System.err.println("Key (base64 encoded): " + encode(key.getEncoded()));
    }
    
    // Encrypts the message when we press the send button
    public String encryptMessage(String plainMessage, int i) throws Exception {
    	System.err.println("Plain message to be sent: " + plainMessage); // Debug, prints plainMessage on the console
    	String encryptedMessage=""; // Generates the string where the encrypted message will be stored
//    	exportIV(); // Debug, prints current (old) IV on the console
    	
    	if(i==0) { // Does not create new IV, uses the old (until all chunks are sent)
    		encryptedMessage = encrypt(ivstr +plainMessage); // Generates encrypted message **without** prepending new IV
    	}
    	else if(i==1) { // i>=1 It **does** generate a new IV
    		IVgen(); //generates new IV to add in the message before sending it (saved in ivstr)
        	encryptedMessage = encrypt(ivstr + plainMessage); // Generates encrypted message after prepending the new IV
        	setIV(ivstr); //sets the new IV which was previously generated //ONLY WORKS FOR SEPARATE DEVICES
    	}
    	
//    	exportIV(); // Debug, prints current (new (or old, depending on the if actions)) IV on the console
    	System.err.println("Message encrypted and sent: " + encryptedMessage); // Debug, prints encryptedMessage on the console
    	return (encryptedMessage); // Returns encrypted message to the caller (to be sent)
    }

    // Encrypts the message when we receive one (it is called by the UDP/TCPchat receive methods)
    public String decryptMessage(String encryptedMessage) throws Exception {
    	// This if checks if it's related to voice call or it is part of a big message
    	// We send some commands in plain text (some times prepended to encrypted text)
    	// "[Voice-Call]" is for Voice-call related commands and comes **un**encrypted
    	// "[Part]" is for messages that come in multiple chunks (useful with UDP) and comes **en**crypted
    	if( (!encryptedMessage.substring(0, 12).equals("[Voice-Call]")) )	{ // No need for out of bounds check, the IV is 16 (>12) chars already
//        	exportIV(); // Debug, prints current (old) IV on the console
    		System.err.println("Message received: " + encryptedMessage); // Debug, prints (received) encrypted message on the console
	 		
    		encryptedMessage = decrypt(encryptedMessage); // Decrypts encryptedMessage and assigns it to encryptedMessage
    		ivstr = encryptedMessage.substring(0, 16);	// Assigns the first 16 chars of the message to IVnew
																// (It should be the new IV which remote uses by now)
    		setIV(ivstr);  //Sets the new IV from IVnew //ONLY WORKS FOR SEPARATE DEVICES
    		encryptedMessage = encryptedMessage.substring(16, encryptedMessage.length()); //Removes IV string from message
    		
    		// First we check if the "[Part]FINISHED" command was sent
        	// If true, this means remote has finished sending messages in chunks and we are ready to return the assembled message
    		if (encryptedMessage.length() > 13) { // At least 14 chars, out of bounds check needed
	    		if(encryptedMessage.substring(0, 14).equals("[Part]FINISHED")) { 
	    			return mesAssembler();
	    		}
	    		else if(encryptedMessage.substring(0, 6).equals("[Part]")){ // In case we receive a chunk with over 14 chars
	    			return chunkStorer(encryptedMessage);
	    		}
    		}
    		// It checks if the chunks of a composite message are being received
        	// They should start with "[Part]" tag, followed by their increment number with a single leading zero
        	// The should be 100 max (though we could configure the system to hold more than that
	    	else if(encryptedMessage.length() > 6)	{ // At least 14 chars, out of bounds check needed
	    		if(encryptedMessage.substring(0, 6).equals("[Part]")){
	    			return chunkStorer(encryptedMessage);
	    		}
	    	}
	 		
	 		System.err.println("Received message decrypted: " + encryptedMessage); // Debug, prints (received) decrypted message on the console
	 		}
    	return encryptedMessage; // Ruturns the decrypted message (this is called only in the first if statement)
    }
    
    // This is called upon recieving the command "[Part]FINISHED" to assemble and return the composite message
    public String mesAssembler() {
		StringBuilder finalMessage = new StringBuilder(); // We use StringBuiler class to append all parts to a single variable
		
		// It iterates through the whole buffer that contains the **decrypted** chunks
		for (int j = 0; j < bufferedMes.length; j++) {
		if (bufferedMes[j] == null) { // If it find a null object, it means all parts have been appended, the rest should be empty
		break;
		}
		// Debug, prints each (non-null) chunk from the buffer on the console
		System.err.println("Buffered Message Part " + j + " : \n" + bufferedMes[j]);
		
		finalMessage.append(bufferedMes[j]); // Appends each chunk to the variable
		bufferedMes[j] = null; // Gradually flushes the array per object to be used again in the future
		}
		String finalString = finalMessage.toString(); // Converts variable to string to be printed
		System.err.println("PART_FINISHED: \n" + finalString); // Debug, prints the composite message
		return finalString; // Returns the composite message to be printed on the chat
    }
    
    // This is called upon recieving a chunk of a composite message and stores the message in the local buffer
    public String chunkStorer(String encryptedMessage) throws Exception {
    	int i = Integer.parseInt(encryptedMessage.substring(6, 8));	// Extract the numbering of the part (Format: [Part]00)
																	// It should be the first 2 chars
		// Removes the "[Part]00" tag from the message and **decrypts** the message
//		bufferedMes[i] = decrypt(encryptedMessage.substring(8, encryptedMessage.length()));
    	// It comes decrypted to this method
    	bufferedMes[i] = encryptedMessage.substring(8, encryptedMessage.length());
//NOPE		String IVnew = bufferedMes[i].substring(0, 16); // Assigns the first 16 chars of the message to IVnew
		// (It should be the new IV which remote uses by now)
		//setIV(IVnew);  //Sets the new IV //ONLY WORKS FOR SEPARATE DEVICES
//NOPE		bufferedMes[i] = bufferedMes[i].substring(16, bufferedMes[i].length()); //Removes IV string from message and saves it to buffer in plain text
		// Debug, prints message which is now stored in the buffer on the console
		System.err.println("Buffered Message Part " + i + " : \n" + bufferedMes[i]);
		return ("Recieving long message, wait..."); // We ask the user to wait as the system collects each chunk
    }
    
    
    
    
// Unecessary or test methods:

// Debug, used to validate that the class is callled properly    
//  public void ct() {
//  	counter++;
//  }
//Debug, used to validate that the class is callled properly
//  public void prt() {
//  	System.err.println("Counter: " + counter);
//  }
  
//Unecessary, used to initiallize the key with a random sequence (we initiallize it with a password instead)
//  public void init() throws Exception {
//      KeyGenerator generator = KeyGenerator.getInstance("AES");
//      generator.init(KEY_SIZE);
//      key = generator.generateKey();
//  }

//Unecessary, used to initiallize the key and the IV with a random sequence
//  private void initFromStrings(String secretKey, String IV) {
//      key = new SecretKeySpec(decode(secretKey), "AES");
//      this.IV = decode(IV);
//  }

// Unecessary
//  public void IVset() {
//  	if (!ivstr.isEmpty()) {
//  		this.IV = decode(ivstr);
//  	}
//  }
    
// Unecessary
//  public String getIV() {
//      return encode(IV);
//  }
   
 // Unecessary
//  public String IVForMessage() {
//  	return encode(IV);
//  }
}
