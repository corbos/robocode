package cjm;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import robocode.*;
import robocode.util.Utils;

/*
 * Charo - by Corbos (corbin@scatterbright.com)
 * 		my first micro, my first guess factor bot
 * 
 * 1.0	Movement: Random oscillation.
 * 		Gun: http://robowiki.net/cgi-bin/robowiki?GuessFactorTargeting for now, segmented on distance, heading and velocity.
 * 1.1	Added a bunch of wiki codesize tweaks, poor Musashi trick and an acceleration segment.
 */

public class Charo extends AdvancedRobot{
	
	//variables=========================
	static double _direction = 1;
	//constants=======================
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_QUARTER = Math.PI / 2;
	static final double ONE_EIGHTH = Math.PI / 4;
	static final double APPROACH_ANGLE = Math.PI / 3 + 0.01745d;
	static final double RETREAT_ANGLE = 2 * Math.PI / 3 + 0.01745d;
	static final double ESCAPE_ANGLE = 0.814339942d;
	//GUN===============================
	static int[][][][][] _factors = new int[6][9][3][20][25];
	static Point2D.Double _enemy;
	static double _lastVelocity;
	static double _robotLastEnergy;
	static int _hits;
	
	public void run(){
		_robotLastEnergy = 100;
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		setColors(Color.ORANGE, null, Color.WHITE);
	}
	
	public void onScannedRobot(ScannedRobotEvent sre){
		
		//move
		RoundRectangle2D.Double _battlefield = new RoundRectangle2D.Double(35, 35, 730, 530, 100, 100);
		double nextX = 0, nextY = 0;
		double distance, enemyHeading, r;
		double x = getX();
		double y = getY();
		_enemy = new Point2D.Double(
				Math.sin(enemyHeading = getHeadingRadians() + sre.getBearingRadians()) * (distance = sre.getDistance()) + x, 
				Math.cos(enemyHeading) * distance + y
				);
		
		if(_robotLastEnergy - (_robotLastEnergy = getEnergy()) > 3){
			_hits++;
		}
		
		if(Math.random() < 0.085d && _hits > getRoundNum()){
			_direction = -_direction;
		}
		
		//Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		for(int i = 0; i < 2; i++){
			double a = RETREAT_ANGLE;
			if(distance >= 400){
				a = APPROACH_ANGLE;
			}
			do{
				nextX = x + Math.sin(r = enemyHeading + (a -= .01745d) * _direction) * 65;
				nextY = y + Math.cos(r) * 65;
			}while(!_battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);
			
			if(_battlefield.contains(nextX, nextY)){
				break;
			}
			_direction = -_direction;
		}
		
		nextX = Utils.normalRelativeAngle(Math.atan2(nextX - x, nextY - y) - getHeadingRadians());
		nextY = 65;
		if(Math.abs(nextX) > ONE_QUARTER){
			nextY = -nextY;
			nextX += nextX > 0 ? -Math.PI : Math.PI;
		}
		setTurnRightRadians(nextX);
		setAhead(nextY);
		
		//add
		r = -(_lastVelocity - (_lastVelocity = Math.abs(nextX = sre.getVelocity())));
		r = r == 0 ? 1 : 1 + r / Math.abs(r);
		int[] factors = _factors[(int)(distance * 0.00625d)]
								 [(int)_lastVelocity]
								  [(int)r]
								   [(int)(Utils.normalAbsoluteAngle(sre.getHeadingRadians() - enemyHeading + (nextX < 0 ? Math.PI : 0)) * 3.023943d)];
		VirtualBullet vb;
		addCustomEvent(vb = new VirtualBullet());
		vb.RX = x;
		vb.RY = y;
		vb.EnemyHeading = enemyHeading;
		vb.Time = getTime();
		vb.Factors = factors;
		
		//shoot
		int bestIndex = 12;
		for(int i = 24; i >= 0; i--){
			if(factors[i] > factors[bestIndex]){
				bestIndex = i;
			}
		}
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - getGunHeadingRadians() + (double)(bestIndex - 12) * 0.067861661d));
		if(getEnergy() > 3){
			setFire(3);
		}
		
		//radar
		setTurnRadarLeftRadians(getRadarTurnRemaining());
	}
	
	/*====================================================
	 * By far, two of the more clever tricks I've seen from robowiki developers:
	 * #1 : Model a VirtualBullet/Wave as a Condition and handle it without a collection.
	 * 		From what I've read, this is PEZ.
	 * #2 : Pre-index the factor array and store it in the Condition. Saves a handleful of bytes.
	 * 		Not sure who deserves credit for this. I've seen it in PEZ, Kawigi and Jamougha micros.
	 * To the creators of both tricks, bravo and thanks.
	 ====================================================*/
	class VirtualBullet extends Condition{
		
		double RX;
		double RY;
		double EnemyHeading;
		long Time;
		int[] Factors;
		
		public boolean test(){
			if((getTime() - Time) * 11 > _enemy.distance(RX, RY) - 18){
				try{
					Factors[12 + (int)(Utils.normalRelativeAngle(Math.atan2(_enemy.x - RX, _enemy.y - RY) - EnemyHeading) * 14.73586076d)]++;
				}
				catch(Exception ex){}
				removeCustomEvent(this);
			}
			return false;
		}
	}
}
