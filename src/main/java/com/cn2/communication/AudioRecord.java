package com.cn2.communication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;

public class AudioRecord { 
	
	private final TargetDataLine targetLine; 
    private final AudioFormat audioFormat; 
    private final DataLine.Info dataInfo;  
    private byte[] buffer = new byte[1024]; 
    
    public AudioRecord() throws LineUnavailableException { 
    	this.audioFormat = new AudioFormat(8000, 8, 1, true, false); 
        this.dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat); 

        if (!AudioSystem.isLineSupported(dataInfo)) {  
             System.out.println("Not supported");
        }
        this.targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);  
    }

    public void open() throws LineUnavailableException {   
        this.targetLine.open(audioFormat);  
        this.targetLine.start();  
    }
    
    public byte[] read() { 
    	targetLine.read(buffer, 0, buffer.length); 
        return buffer;  
	}
	
	public void close() {  
		targetLine.stop();  
		targetLine.close(); 
	}
    
}    

    
    

