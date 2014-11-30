package serenity;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;

public class MotorRunner implements Runnable {
	
	private Rest rest = null;
	private Serial serial = SerialFactory.createInstance();
			
	public MotorRunner(Rest r) {
		rest = r;
		serial.addListener(new SerialDataListener() {
		@Override
			public void dataReceived(SerialDataEvent event) {
				// ignore anything motor sends back
        	}            
		});
		try {
			serial.open(Serial.DEFAULT_COM_PORT, 19200);
		} catch( java.lang.UnsatisfiedLinkError e ) {
			serial = null; // no pi libraries--not running on a pi
			Control ctl = rest.getControl();
			ctl.serialPanic = true;
			rest.writeControl(ctl);
		}
	}

	@Override
	public void run() {
		Control ctl = rest.getControl();
		if( ctl.speeds[ctl.motorSpeed] != 0 ) {
			// Somehow write this out to the RS485 interface, and possibly read (and ignore) the response packet.
			if( serial == null ) System.out.println("Motor: "+ ctl.speeds[ctl.motorSpeed]);
			try {
				rest.getLock();
				if( serial != null ) {
					serial.write(haywardPacket(ctl.speeds[ctl.motorSpeed]));
					ctl.serialPanic = false;
				}
			} catch( InterruptedException ix ) {
				// ignore for now
			} catch ( Exception x ) {
				ctl.serialPanic = true;
			} finally {
				rest.writeControl(ctl);
			}
		}
	}

    private byte[] haywardPacket( Integer pct ) {
    	byte[] packet = {0x10, 0x02, 0x0C, 0x01, 0x00, 0x00, 0x00, 0x00, 0x10, 0x03};
    	packet[5] = pct.byteValue();
    	packet[7] = (new Integer(31 + pct)).byteValue(); // checksum
    	return packet;
    }
    
    // !! ATTENTION !!
    // By default, the serial port is configured as a console port 
    // for interacting with the Linux OS shell.  If you want to use 
    // the serial port in a software program, you must disable the 
    // OS from using this port.  Please see this blog article by  
    // Clayton Smith for step-by-step instructions on how to disable 
    // the OS console for this port:
    // http://www.irrational.net/2012/04/19/using-the-raspberry-pis-serial-port/
    
}
