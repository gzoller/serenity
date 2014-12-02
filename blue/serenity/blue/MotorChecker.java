package serenity.blue;

public class MotorChecker implements Runnable {

	private Rest rest = null;
	
	public MotorChecker( Rest r ) { rest = r; }
	
	@Override
	public void run() {
		rest.checkMotor();
	}

}
