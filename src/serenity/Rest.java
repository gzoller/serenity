package serenity;

import static spark.Spark.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.text.SimpleDateFormat;

/*
 *  get(SERENITY+"/control")
 *  post(SERENITY+"/reset")
 *  put(SERENITY+"/control/schedule") -- payload: 24 ints between 0 and 7 inclusive
 *  put(SERENITY+"/control/speeds") -- payload: 8 ints (speeds) between 0 and 100 inclusive
 *  put(SERENITY+"/motor/start?speed=<Int0to7>") 
 *  put(SERENITY+"/motor/stop")
 *  put(SERENITY+"/resume")
 *  put(SERENITY+"/location?zip=<zip>")
 */

public class Rest {
	private ObjectMapper mapper = new ObjectMapper();	
	private Control ctl = null;
	
	private static final String CTL_FILE        = "serenity.json";
	private static final String SERENITY        = "/serenity";
	private static final long   ONE_SECOND      = 1000;
	private static final long   FIVE_MINUTES    = ONE_SECOND * 60 * 5;
	private static final long   TWO_HOURS       = ONE_SECOND * 60 * 60 * 2;
	private static final long   MOTOR_PERIOD_MS = ONE_SECOND/2;  
	private static final String NWS_URL         = "http://graphical.weather.gov/xml/sample_products/browser_interface/ndfdXMLclient.php?zipCodeList=%s&product=time-series&begin=%s&end=%s&temp=temp";
	private SimpleDateFormat DATE_FORMAT        = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private final Semaphore ctlwrite            = new Semaphore(5,true);
	
	Control getControl() { return ctl; }
	
	public Rest() {
    	// Load or create the Control object
    	try {
    		ctl = mapper.readValue(new File(CTL_FILE), Control.class);
    	} catch ( Exception e ) {
    		reset(); // Something bad happened... reset and re-write control file
    	}
    	checkFreeze();
   	
    	// Start the threaded services
    	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    	scheduler.scheduleAtFixedRate(new MotorRunner(this),   ONE_SECOND, MOTOR_PERIOD_MS, TimeUnit.MILLISECONDS);
    	scheduler.scheduleAtFixedRate(new MotorChecker(this),  ONE_SECOND, FIVE_MINUTES,    TimeUnit.MILLISECONDS);
    	scheduler.scheduleAtFixedRate(new FreezeChecker(this), ONE_SECOND, TWO_HOURS*6,     TimeUnit.MILLISECONDS);
    	 
    	// RESTful service
    	//----------------
    	get(SERENITY+"/control", (req, res) -> {
	    	try { 
	    		res.type("application/json");
	    		return mapper.writeValueAsString(ctl);
	    	} catch(JsonProcessingException je ) {
	    		reset();
	    		throw new IllegalArgumentException(je);
	    	}
    	});
    	
    	post(SERENITY+"/reset", (req, res) -> {
    		reset();
    		return "OK";
    	});
    	
    	//------------------
    	//  PUT
    	//------------------
    	put(SERENITY+"/control/schedule", (req, res) -> {
    		try {
    			int[] newSched = mapper.readValue(req.body(), int[].class);
    			if( newSched.length != 24 ) throw new IOException("boom");
    			for( int ns : newSched ) {
    				if( ns < 0 || ns > 7 ) throw new IOException("scope");
    			}
    			ctlwrite.acquire();
    			ctl.schedule = newSched;
    			writeControl(ctl);
    			return "OK";
    		} catch( IOException je ) {
    			throw new IllegalArgumentException(je);
    		} catch( InterruptedException ie ) {
    			throw new IllegalArgumentException(ie);
    		}
    	});

    	put(SERENITY+"/control/speeds", (req, res) -> {
    		try {
    			int[] newSpeeds = mapper.readValue(req.body(), int[].class);
    			if( newSpeeds.length != 8 ) throw new IOException("boom");
    			for( int ns : newSpeeds ) {
    				if( ns < 0 || ns > 100 ) throw new IOException("scope");
    			}
    			ctlwrite.acquire();
    			ctl.speeds = newSpeeds;
    			ctl.speeds[0] = 0; // 0th speed is always 0/off
    			writeControl(ctl);
    			return "OK";
    		} catch( IOException je ) {
    			throw new IllegalArgumentException(je);
			} catch( InterruptedException ie ) {
				throw new IllegalArgumentException(ie);
			}
    	});
    	
    	put(SERENITY+"/motor/start", (req, res) -> {
    		try {
    			int speed = Integer.parseInt( req.queryParams("speed") );
    			if( speed < 0 || speed > 7 ) throw new NumberFormatException("boom");
    			ctlwrite.acquire();
    			ctl.motorSpeed = speed;
    			ctl.isManual = true;
    			ctl.manualTime = System.currentTimeMillis();
    			writeControl(ctl);
    			return "OK";
    		} catch( NumberFormatException nfe ) {
    			nfe.printStackTrace();
    			throw new IllegalArgumentException(nfe);
			} catch( InterruptedException ie ) {
				throw new IllegalArgumentException(ie);
			}
    	});
 
       	put(SERENITY+"/motor/stop", (req, res) -> {
       		try {
    			ctlwrite.acquire();
				ctl.motorSpeed = 0;
				ctl.isManual = true;
				ctl.manualTime = System.currentTimeMillis();
				writeControl(ctl);
				return "OK";
			} catch( InterruptedException ie ) {
				throw new IllegalArgumentException(ie);
			}
       	});
        
       	put(SERENITY+"/resume", (req, res) -> {
       		resume();
			return "OK";
       	});

    	put(SERENITY+"/location", (req, res) -> {
    		try {
    			String zip = req.queryParams("zip");
    			ctlwrite.acquire();
    			ctl.zip = zip;
    			writeControl(ctl);
    			checkFreeze();
    			return "OK";
			} catch( InterruptedException ie ) {
				throw new IllegalArgumentException(ie);
			}
    	});

    	exception(IllegalArgumentException.class, (e, request, response) -> {
    	    response.status(500);
    	    response.body("Application server error");
    	});
    }
	
    public static void main(String[] args) {
    	new Rest();
    }
    
    private void reset() {
		ctl = new Control();
		writeControl(ctl);
    }
    
    public void getLock() throws InterruptedException {
    	ctlwrite.acquire();
    }
    
    public void writeControl( Control c ) {
    	try {
    		mapper.writeValue(new File(CTL_FILE), c);
    	} catch ( Exception e ) {
    	} finally {
			ctlwrite.release();
    	}
    }
    
    private void resume() {
    	try {
			ctlwrite.acquire();
			ctl.isManual = false;
			ctl.manualTime = 0L;
			checkMotor();
			writeControl(ctl);
		} catch( InterruptedException ie ) {
		}
    }
    
	void checkMotor() {
		if( ctl.isManual && System.currentTimeMillis() - ctl.manualTime > TWO_HOURS ) 
			resume();
		else {
			Calendar cal = Calendar.getInstance();
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int schedSpeed = ctl.schedule[hour];
			if( ctl.forecastTemps[hour] <= ctl.freezeTemp1F ) 
				schedSpeed = ctl.overrideSpeed1;
			if( ctl.forecastTemps[hour] <= ctl.freezeTemp2F ) 
				schedSpeed = ctl.overrideSpeed2;
			try {
				ctlwrite.acquire();
				ctl.motorSpeed = schedSpeed;
				Calendar c = Calendar.getInstance();
				c.setTime( new Date() );
				ctl.currentHour = c.get(Calendar.HOUR_OF_DAY);
				writeControl(ctl);
			} catch( InterruptedException ie ) {
			}
		}
	}
	
	void checkFreeze() {
		String fromTime = DATE_FORMAT.format( new Date() );
		Calendar c = Calendar.getInstance();
		c.setTime( new Date() );
		c.add(Calendar.HOUR, 23);
		String toTime = DATE_FORMAT.format(c.getTime());
		String zip = ctl.zip;
		if( zip == "" ) zip = "75019"; // defualt zip -- hasn't been set yet
		String url = String.format(NWS_URL, zip, fromTime, toTime);
		TimeTemps tt = Util.parseNWSTemps(url);
		try {
			ctlwrite.acquire();
			for( int i=0; i<tt.times.length; i++ ) {
				ctl.forecastTemps[tt.times[i]] = tt.temps[i];
				ctl.forecastTemps[tt.times[i]+1] = tt.temps[i];
				ctl.forecastTemps[tt.times[i]+2] = tt.temps[i];
			}
			// TODO: current hour's forecastTemp should be min of what NWS reports and what we read from sensor
			ctl.lastNWS = DATE_FORMAT.format( new Date() );
			writeControl(ctl);
		} catch( InterruptedException ie ) {
		}
	}
}
