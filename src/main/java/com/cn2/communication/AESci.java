package com.cn2.communication;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;	//for generating key with password

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;	//for generating key with password
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;	//for generating key with password
import java.util.Arrays;
import java.util.Base64;

// AES + *ci*pher -> AESci: encrypts and decrypts the UDP chat messages with AES-GCM
public class AESci {
    private SecretKey key;				// the AES key
    private final int T_LEN = 128;		// the length of the tag used in the Galois/Counter Mode (GCM) of operation in the AES encryption algorithm
    									// In GCM, a tag is appended to the ciphertext during encryption, and it is used during decryption to verify the integrity of the ciphertext
    private byte[] IV = new byte[12];	// Initialization Vector: This is the IV (the real IV) our cipher uses, we will call it IV
    									// it is essentially the ephemeral key that changes after every encryption
    private String salt = "potato";		// the salt used to generate the key from our password
    private String ivstr = "";			// the base64 encoded string containing the new IV, before it is assigned to IV
    public String[] bufferedMes = new String[100];	// the buffer to contain messages over 500chars in plaintext
    												// Should hold 100 strings max of 500 chars max (50k chars max total)

    // constructor
    public AESci() throws Exception {
    	initFromPassword("VerySecurePassword");	// Sets the default key
    	Arrays.fill(IV, (byte) 1);				// Both peers start from the same IV
    	ivstr = encode(IV);
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
    	random.nextBytes(iv2);		// Fills the temporary iv2 array with 12 random bytes
    	ivstr = encode(iv2); 		// Encodes the iv as a base64 string and assigns it to ivstr variable
    }

    // Sets the IV from a base64 string
    public void setIV(String IVnew) {
    	if (!IVnew.isEmpty()) { // Checks if the string is empty
    		this.IV = decode(IVnew);	// Decodes the base64 string to a byte array and assigns it to IV
    	}
    }

    // Encrypts text
    public String encrypt(String message) throws Exception {
    	byte[] messageInBytes = message.getBytes(); // Encodes and assigns string message to a byte array to be encrypted
        Cipher encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creates a new AES/GCM/NoPadding Cipher object
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV); // Creates a GCMParameterSpec object with the given tag length (T_LEN) and IV
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key, spec); // Initialize the cipher in *encryption* mode with the AES key and the GCMParameterSpec
        byte[] encryptedBytes = encryptionCipher.doFinal(messageInBytes); // Encrypts the message using the cipher
        return encode(encryptedBytes); // Returns a base64 encoded string of the message
    }

    // Decrypts text
    public String decrypt(String encryptedMessage) throws Exception {
        byte[] messageInBytes = decode(encryptedMessage); // Decodes and assigns base64 string message to a byte array to be decrypted
        Cipher decryptionCipher = Cipher.getInstance("AES/GCM/NoPadding"); // Creates a new AES/GCM/NoPadding cipher object for decryption
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV); // Creates a GCMParameterSpec object with the same tag length and IV used for encryption
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec); // Initializes the cipher in decryption mode with the AES key and the GCMParameterSpec object
        byte[] decryptedBytes = decryptionCipher.doFinal(messageInBytes); // Decrypts the message using the cipher
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

    // Encrypts the message when we press the send button
    // i == 0: keeps the current IV (used for the chunks of a long message)
    // i == 1: generates a new IV, prepends it to the message and switches to it after encrypting
    public String encryptMessage(String plainMessage, int i) throws Exception {
    	String encryptedMessage = ""; // The string where the encrypted message will be stored

    	if (i == 0) { // Does not create new IV, uses the old (until all chunks are sent)
    		encryptedMessage = encrypt(ivstr + plainMessage); // Encrypts the message **without** a new IV in front
    	}
    	else if (i == 1) { // Generates a new IV
    		IVgen(); // Generates new IV to add in the message before sending it (saved in ivstr)
        	encryptedMessage = encrypt(ivstr + plainMessage); // Encrypts the message after prepending the new IV
        	setIV(ivstr); // Sets the new IV which was previously generated //ONLY WORKS FOR SEPARATE DEVICES
    	}
    	return encryptedMessage; // Returns encrypted message to the caller (to be sent)
    }

    // Decrypts the message when we receive one (it is called by the UDPChat receive method)
    public String decryptMessage(String encryptedMessage) throws Exception {
    	// We send some commands in plain text:
    	// "[Voice-Call]" is for Voice-call related commands and comes **un**encrypted
    	// "[Part]" is for messages that come in multiple chunks and comes **en**crypted
    	if (!encryptedMessage.substring(0, 12).equals("[Voice-Call]")) { // No need for out of bounds check, the IV is 16 (>12) chars already
    		encryptedMessage = decrypt(encryptedMessage); // Decrypts encryptedMessage
    		ivstr = encryptedMessage.substring(0, 16);	// The first 16 chars are the new IV which remote uses by now
    		setIV(ivstr);  // Sets the new IV //ONLY WORKS FOR SEPARATE DEVICES
    		encryptedMessage = encryptedMessage.substring(16, encryptedMessage.length()); // Removes IV string from message

    		// First we check if the "[Part]FINISHED" command was sent
        	// If true, this means remote has finished sending messages in chunks and we are ready to return the assembled message
    		if (encryptedMessage.length() > 13) { // At least 14 chars, out of bounds check needed
	    		if (encryptedMessage.substring(0, 14).equals("[Part]FINISHED")) {
	    			return mesAssembler();
	    		}
	    		else if (encryptedMessage.substring(0, 6).equals("[Part]")) { // In case we receive a chunk with over 14 chars
	    			return chunkStorer(encryptedMessage);
	    		}
    		}
    		// Chunks of a composite message start with the "[Part]" tag, followed by their two-digit number
	    	else if (encryptedMessage.length() > 6) { // At least 7 chars, out of bounds check needed
	    		if (encryptedMessage.substring(0, 6).equals("[Part]")) {
	    			return chunkStorer(encryptedMessage);
	    		}
	    	}
    	}
    	return encryptedMessage; // Returns the decrypted message (or the unencrypted voice call command)
    }

    // This is called upon receiving the command "[Part]FINISHED" to assemble and return the composite message
    public String mesAssembler() {
		StringBuilder finalMessage = new StringBuilder(); // We use StringBuilder class to append all parts to a single variable

		// It iterates through the whole buffer that contains the **decrypted** chunks
		for (int j = 0; j < bufferedMes.length; j++) {
			if (bufferedMes[j] == null) { // If it finds a null object, it means all parts have been appended, the rest should be empty
				break;
			}
			finalMessage.append(bufferedMes[j]); // Appends each chunk to the variable
			bufferedMes[j] = null; // Gradually flushes the array per object to be used again in the future
		}
		return finalMessage.toString(); // Returns the composite message to be printed on the chat
    }

    // This is called upon receiving a chunk of a composite message and stores the (already decrypted) chunk in the local buffer
    public String chunkStorer(String encryptedMessage) throws Exception {
    	int i = Integer.parseInt(encryptedMessage.substring(6, 8));	// Extract the numbering of the part (Format: [Part]00)
    	bufferedMes[i] = encryptedMessage.substring(8, encryptedMessage.length()); // Removes the "[Part]00" tag and stores the chunk
		return ("Recieving long message, wait..."); // We ask the user to wait as the system collects each chunk
    }
}
