package cjm;

import robocode.*;
//import java.util.Vector;
import java.awt.Color;

/*
 * Chomsky - by Corbos (corbin@scatterbright.com)
 * 
 * Thanks to everyone at robowiki.net. 
 * 
 * 1.0 	Movement: Semi-Random Oscillator or Mirror depending on last round's result
 *     	Gun: Extrapolated enemy deltaX and deltaY with neural net through many turns.
 * 1.1 	Flattened Gun to key off Lateral Movement/Lateral Accelation.
 * 1.2 	Added StopNGo movement, tweaked oscillator and scaled gun inputs.
 * 1.25	Tuned Gun to Heading/Velocity/Acceleration, fixed premature Ram bug
 * 1.3	Segmented Neural Gun and added CT/LT Gun
 * 1.4	Movement Update - lessons learned from Che and expirements.
 * 1.5	Movement Update - Wave Surfing, see full description in Evader
 */

public class Chomsky extends AdvancedRobot{

	private double _radarDegrees = 540;
	private Evader _evader;
	private static Gunsky _gun;
	private double lastEnemyBearing = Double.MIN_VALUE;
	
	public void run(){
	
		setColors(Color.ORANGE, Color.WHITE, Color.YELLOW);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		_evader = new Evader(this);
		
		if(_gun == null){
			_gun = new Gunsky(this);
		}
		
		setTurnRadarRight(_radarDegrees);
		
		while(true) {
		    _evader.move();
			_gun.fire();
			execute();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent sre){
		lastEnemyBearing = Util.getUnitCircleAngle(getHeadingRadians() + sre.getBearingRadians());
		_gun.onScannedRobot(sre, lastEnemyBearing);
		setTurnRadarRight(_radarDegrees = -_radarDegrees);
		_evader.onScannedRobot(sre);
	}
	
	public void onHitByBullet(HitByBulletEvent e){
		_evader.onHitByBullet(e);
	}
	
	public void onBulletHit(BulletHitEvent e){
		_evader.onBulletHit(e);
	}
	
	public void onHitRobot(HitRobotEvent e){
		_evader.onHitRobot(e);
	}
	
	public void onWin(WinEvent e){
		_gun.clear();
	}
}
