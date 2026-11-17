package com.cn2.communication;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

// AES + *ci*pher -> AESci: encrypts and decrypts the UDP chat messages with AES-GCM
//
// Every message carries the IV of the next one: the sender generates a new IV, puts it (base64, 16 chars)
// in front of the message, encrypts both with the current IV and then switches to the new IV.
// The receiver decrypts with the current IV and switches to the IV it finds in the message.
public class AESci {
    private SecretKey key;
    private final int T_LEN = 128;		// length of the GCM authentication tag, in bits
    private byte[] IV = new byte[12];	// the IV currently in use
    private String salt = "potato";
    private String ivstr = "";			// base64 of the next IV, before it replaces IV
    public String[] bufferedMes = new String[100];	// chunks of a long message: 100 chunks of 500 chars max (50k chars)

    public AESci() throws Exception {
    	initFromPassword("VerySecurePassword");	// default key, until Set Pass is used
    	Arrays.fill(IV, (byte) 1);				// both peers must start from the same IV
    	ivstr = encode(IV);
    }

    // Derives the AES key from a password (PBKDF2 with HMAC-SHA256)
    public void initFromPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
    	SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    	PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
    	key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    // Generates a random IV for the next message and keeps it in ivstr (it is not used until setIV)
    public void IVgen() {
    	SecureRandom random = new SecureRandom();
    	byte[] iv2 = new byte[12];
    	random.nextBytes(iv2);
    	ivstr = encode(iv2);
    }

    public void setIV(String IVnew) {
    	if (!IVnew.isEmpty()) {
    		this.IV = decode(IVnew);
    	}
    }

    public String encrypt(String message) throws Exception {
    	byte[] messageInBytes = message.getBytes();
        Cipher encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encryptedBytes = encryptionCipher.doFinal(messageInBytes);
        return encode(encryptedBytes);
    }

    public String decrypt(String encryptedMessage) throws Exception {
        byte[] messageInBytes = decode(encryptedMessage);
        Cipher decryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, IV);
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decryptedBytes = decryptionCipher.doFinal(messageInBytes);
        return new String(decryptedBytes);
    }

    private String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    // i == 0: keeps the current IV (used for the chunks of a long message)
    // i == 1: generates a new IV, sends it with the message and switches to it
    public String encryptMessage(String plainMessage, int i) throws Exception {
    	String encryptedMessage = "";

    	if (i == 0) {
    		encryptedMessage = encrypt(ivstr + plainMessage);
    	}
    	else if (i == 1) {
    		IVgen();
        	encryptedMessage = encrypt(ivstr + plainMessage);
        	setIV(ivstr); // only works between two devices: on one machine the app would receive its own message
    	}
    	return encryptedMessage;
    }

    // Returns the text to show for a received message.
    // "[Voice-Call]" commands arrive unencrypted and are returned as they are.
    // "[Part]00..." chunks are stored, and "[Part]FINISHED" returns the assembled long message.
    public String decryptMessage(String encryptedMessage) throws Exception {
    	if (!encryptedMessage.substring(0, 12).equals("[Voice-Call]")) { // an encrypted message is always longer than 12 chars
    		encryptedMessage = decrypt(encryptedMessage);
    		ivstr = encryptedMessage.substring(0, 16); // the first 16 chars are the next IV
    		setIV(ivstr);
    		encryptedMessage = encryptedMessage.substring(16, encryptedMessage.length());

    		if (encryptedMessage.length() > 13) {
	    		if (encryptedMessage.substring(0, 14).equals("[Part]FINISHED")) {
	    			return mesAssembler();
	    		}
	    		else if (encryptedMessage.substring(0, 6).equals("[Part]")) {
	    			return chunkStorer(encryptedMessage);
	    		}
    		}
	    	else if (encryptedMessage.length() > 6) {
	    		if (encryptedMessage.substring(0, 6).equals("[Part]")) {
	    			return chunkStorer(encryptedMessage);
	    		}
	    	}
    	}
    	return encryptedMessage;
    }

    // Joins the stored chunks in order and empties the buffer
    public String mesAssembler() {
		StringBuilder finalMessage = new StringBuilder();

		for (int j = 0; j < bufferedMes.length; j++) {
			if (bufferedMes[j] == null) { // chunks are numbered from 0, so the first empty slot is the end
				break;
			}
			finalMessage.append(bufferedMes[j]);
			bufferedMes[j] = null;
		}
		return finalMessage.toString();
    }

    // Stores a decrypted chunk ("[Part]" + two-digit number + text) in the slot given by its number
    public String chunkStorer(String encryptedMessage) throws Exception {
    	int i = Integer.parseInt(encryptedMessage.substring(6, 8));
    	bufferedMes[i] = encryptedMessage.substring(8, encryptedMessage.length());
		return ("Recieving long message, wait...");
    }
}
