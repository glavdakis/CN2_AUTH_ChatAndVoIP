package com.cn2.communication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;


public class AudioPlayback { // class for playing sound
	
    private final SourceDataLine sourceLine; // define sourceLine for playing audio 
    private final AudioFormat audioFormat; // define audio format 
    private final DataLine.Info dataInfo; // define info for the audio 
    
	public AudioPlayback() throws LineUnavailableException { // constructor AudioPlay, initialize variables
		
		this.audioFormat = new AudioFormat(8000, 8, 1, true, false); /* audio format:sampleRate=8000 samples/sec,
	    sampleSize=8 bits,  1 channel, signed (true) PCM, littleEndian (false) */
		this.dataInfo = new DataLine.Info(SourceDataLine.class, audioFormat); /* object dataInfo, contains information
		on what type of audio format sourceLine must have */
		this.sourceLine = (SourceDataLine) AudioSystem.getLine(dataInfo); // get sourceLine 
	}
	
	public void open() throws LineUnavailableException {  
		
        sourceLine.open(audioFormat); // open sourceLine 
        sourceLine.start(); // speaker open, sourceLine starts playing audio from speaker 
	}
	
	public void write(byte[] buffer) {   
		
		sourceLine.write(buffer, 0, buffer.length); // write the received audio data from buffer to sourceLine, offset=0 for real time usage 
	}
	
	public void close() {  
		
		sourceLine.drain(); // ensure all data is played 
		sourceLine.stop(); // stop the sourceLine but retains its resources 
		sourceLine.close(); // close the sourceLine and releases resources
	}
	
}
