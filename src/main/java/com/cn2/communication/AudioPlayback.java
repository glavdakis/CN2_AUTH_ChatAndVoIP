package com.cn2.communication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

public class AudioPlayback { 
    private final SourceDataLine sourceLine; 
    private final AudioFormat audioFormat; 
    private final DataLine.Info dataInfo; 
    
	public AudioPlayback() throws LineUnavailableException { 
		this.audioFormat = new AudioFormat(8000, 8, 1, true, false); 
		this.dataInfo = new DataLine.Info(SourceDataLine.class, audioFormat); 
		this.sourceLine = (SourceDataLine) AudioSystem.getLine(dataInfo);  
	}
	
	public void open() throws LineUnavailableException {  
        sourceLine.open(audioFormat); 
        sourceLine.start(); 
	}
	
	public void write(byte[] buffer) {   
		sourceLine.write(buffer, 0, buffer.length); 
	}
	
	public void close() {  
		sourceLine.drain(); 
		sourceLine.stop(); 
		sourceLine.close(); 
	}
	
}
