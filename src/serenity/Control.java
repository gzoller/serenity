package serenity;

public class Control {
	public int[]	schedule		= new int[24];
	public int		overrideSpeed1	= 1;
	public int		overrideSpeed2	= 2;
	public int		freezeTemp1F	= 32;
	public int		freezeTemp2F	= 23;
	public int 		motorSpeed 		= 0;
	public int      currentHour     = 0;
	public boolean	isManual 		= false;
	public long  	manualTime		= 0L;
	public String	zip      		= "";
	public String	lastNWS         = "";
	public boolean	serialPanic     = false;
	public int[] 	forecastTemps	= {45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45,45};
	public int[]    speeds          = {0,15,35,50,60,70,80,90};  // as pct of motor speed
}
