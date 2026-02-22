package com.cn2.communication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;

public class AudioRecord { // class for recording sound
	
	private final TargetDataLine targetLine; // define targetLine for capturing audio 
    private final AudioFormat audioFormat; // define audio format 
    private final DataLine.Info dataInfo; // define info for the audio. 
    private byte[] buffer = new byte[1024]; // define buffer to store stream in 
    
    public AudioRecord() throws LineUnavailableException { // constructor AudioCapture, initialize variables
    
    	this.audioFormat = new AudioFormat(8000, 8, 1, true, false); /* audio format:sampleRate=8000 samples/sec,
	    sampleSize=8 bits,  1 channel, signed (true) PCM, littleEndian (false) */
        this.dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat); /* object dataInfo, contains information
		on what type of audio format targetLine must have */

        if (!AudioSystem.isLineSupported(dataInfo)) { // check if audio is supported 
             System.out.println("Not supported");
        }
         
        this.targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo); // get targetLine 
    }

    public void open() throws LineUnavailableException {   
    	
        this.targetLine.open(audioFormat); // open targetLine 
        this.targetLine.start(); // microphone open, targetLine starts capturing data from microphone 
    }
    
    public byte[] read() { 
    	
    	targetLine.read(buffer, 0, buffer.length); // read the recorded audio data from targetLine into buffer, offset=0 for real time usage 
        return buffer; // return bytes of data from buffer  
	}
	
	public void close() {  
		
		targetLine.stop(); // stop the targetLine but retains its resources 
		targetLine.close(); // close the targetLine and releases resources 
	}
    
}    

    
    

