package serenity.blue;

public class FreezeChecker implements Runnable {

	private Rest rest = null;
	
	public FreezeChecker( Rest r ) { rest = r; }
	
	@Override
	public void run() {
		rest.checkFreeze();
	}
}
