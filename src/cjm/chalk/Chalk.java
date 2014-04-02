package cjm.chalk;

import java.awt.Color;
import java.util.Vector;

import robocode.*;
import robocode.util.Utils;

/*
 * Chalk - by Corbos (corbin@scatterbright.com)
 * 
 * Thanks to everyone at robowiki.net. 
 * 
 * 1.0 Beta Release
 * 	Movement: Rudimentary Wave Surfing
 * 	Gun: Indexed Pattern Matcher
 * 1.01 Bug Fix
 * 	Movement: Smoothing function was only looking at the currently index. Whoops.
 * 1.1 Movement Fix - Wave Surfing Tweeks plus Voidious comments.
 * 1.2 Gun Bug Fix - For some odd reason, the current gun didn't take lateral velocity direction into consideration.
 * 	- Chalk uses Lateral Velocity Segments but he had no concept of left vs. right movement.
 * 2.0 Removed all stats buffers from movement and gun. See how it goes.
 * 2.1.H Gun update - added play-forward pattern matching
 * 2.1.He Gun update - now with forward wall distancing!
 * 2.1.Li Movement update - add gun-like weight system to movement 'features'. Seems like the correct move but probably 
 * 	won't be a big boost in rating. Much more work to do.
 * 2.1.Be Movement update - wall distance feature, small tweaks.
 * 2.1.B Movement Update - Replaced 'best-of' scan selection with weighted sum of all saved scans
 * 2.1.C Movement Update - Projects five movement options versus three
 * 		Gun Update - Lighter touch; no more 2.5 strength bullets
 * 2.2 Movement Update - re-worked movement projection. Removed 'features': straight velocity, wall distance.
 * 	Gun Update - waiting for aim before shooting.
 * 2.3 Movement Update - time for the flattener.
 * 2.3.H Movement Update - Flattener is now harder to activate.
 * 	Gun Update - Error on scans now calculated as the sum of squares.
 * 2.3.He Gun Update - Restructuring. No more play forward. Now uses wave bearing.
 * 2.3.Li Movement Update - Added wall smoothing 'feature'. (Turned out to be a bad idea.)
 * 2.3.Be Movement Update = Removed wall smoothing feature. Tuned the wave impact calculations.
 * 	Gun Update - Added a decision to use the standard gun or a faster learning gun (shorter scan buffer, good for adapting movements).
 * 2.3.B Tweaks Update
 * 2.3.C Gun Update - speed updates, mostly from ABC. Removed fast-learning gun.
 * 2.4 Gun Update - changed bullet timing, tweaked weights
 * 	Movement Update - experimenting with three clusters versus weighting features.
 * 2.5 Gun - speed things up to analyze 27500 scans per shot
 *  Movement - Removed the three cluster nonsense
 *  	Added Voidious's min/max calculation [big thanks!], changed timing
 *  2.5.H Movement Update - tweaked weights.
 *  2.5.He Gun Update - tweaked weights. Added a 1000 scans which may makes things annoyingly slow.
 *  2.5.Li	Gun Update - small tweaks
 *  2.5.Be	Gun Update - moved processing out of event handler so I can handle more scans. ;)
 *  	Movement Update - small, small tweaks
 *  2.5.B	Movement Update - Experiments with wall smoothing - leaves the code very ugly.
 *  2.5.C	Movement Updates - various unproductive noodlings with the current movement.
 *  2.5.N	Gun Update - gun now excludes scans that would leave the opponent out-of-bounds via a
 *  	nice distancedelta trick. No need to play the movie forward ;).
 *  2.5.O	Gun Update - first attempt at virtual guns
 *  2.5.F	Gun Update - made the adaptive gun a little harder to activate
 *  	Movement Update - used more history for the flattener
 *  2.5.Ne	Gun Update - added activation rules for the adaptive gun
 *  	Movement Update - made it harder to activate the flattener. Added a penalty for surfing closer to an enemy.
 *  2.5.Na	Gun Update - made the adaptive gun even harder to activate
 *  	Movement Update - rollback to 2.5.N
 */

public class Chalk extends AdvancedRobot {
	
	cjm.chalk.Persuader _persuader;
	cjm.chalk.Evader _evader;
	int _ticksFromLastScan = 1;
	
	boolean _antiMirror;
	double _fieldWidth;
	double _fieldHeight;

	public void run(){
		
		_persuader = new cjm.chalk.Persuader(this);
		_evader = new cjm.chalk.Evader(this);
		
		setColors(Color.WHITE, Color.WHITE, Color.WHITE);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		if(MoveState.BOUNDS == null){
			MoveState.BOUNDS = new java.awt.geom.Rectangle2D.Double(18d, 18d, getBattleFieldWidth() - 36d, getBattleFieldHeight() - 36d);
		}
		
		_fieldWidth = this.getBattleFieldWidth();
		_fieldHeight = this.getBattleFieldHeight();
		
		while(true){
			if(_ticksFromLastScan++ > 1){
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			if(!Debug.IS_TC){
			    _evader.move();
			}
			_persuader.process();
			execute();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent sre){
		
		_ticksFromLastScan = 0;
		//radar
		setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians() + sre.getBearingRadians() - getRadarHeadingRadians()) * 2);
		
		if(!Debug.IS_MC){
		    _persuader.onScannedRobot(sre);
		}
		
		if(!Debug.IS_TC){
		    _evader.onScannedRobot(sre);
		}
		
		//double enemyHeading = Utils.normalAbsoluteAngle(getHeadingRadians() + sre.getBearingRadians());
		//double distance = sre.getDistance();
		//double eX = Math.sin(enemyHeading) * distance + getX();
		//double eY = Math.cos(enemyHeading) * distance + getY();
		//_mirrorAvg.addValue(ChalkUtils.distance(_fieldWidth - getX(), _fieldHeight - getY(), eX, eY));
		//_antiMirror = _mirrorAvg.getAverage() <= 30d;
	}
	
	public void onHitByBullet(HitByBulletEvent e){
	    if(!Debug.IS_TC){
	        _evader.onHitByBullet(e);
	    }
	}
	
	public void onBulletHit(BulletHitEvent e){
		_evader.onBulletHit(e);
		_persuader.onBulletHit(e);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){
	    _evader.onBulletHitBullet(e);
	    _persuader.onBulletHitBullet(e);
	}
	
	public void onHitRobot(HitRobotEvent e){
		_evader.onHitRobot(e);
	}
	
	public void onWin(WinEvent e){
	    printStats();
	}
	
	public void onDeath(DeathEvent e){
		Vector<Event> v = this.getAllEvents();
		for(int i = v.size() - 1; i >= 0; i--){
			if(v.get(i) instanceof HitByBulletEvent){
				_evader.onHitByBullet((HitByBulletEvent)v.get(i));
			}
		}
	    printStats();
	}
	
	void printStats(){
	    if(Debug.PRINT_STATS){
	        if(!Debug.IS_TC){
	            System.out.println(_evader.getStats());
	        }
	        if(!Debug.IS_MC){
	        	System.out.println(_persuader.getStats());
	        }
	    }
	}
}
