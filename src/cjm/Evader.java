package cjm;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

/*	Evader - Wave Surfing by Corbos
 * 
 * 	Wave surfing didn't come easy to me, even in this rudimentary form.
 * 	Still, it wouldn't have come at all without the insights and shared resources
 * 	generously made available at robowiki.net.
 * 	Those interested might start here: http://robowiki.net/cgi-bin/robowiki?WaveSurfing
 * 
 * 	Specifically, I'm indebted to the following authors and bots :
 * 	PEZ - Pugilist & CassiusClay
 * 	Jamougha - RaikoMX
 * 	Alcatraz - Cyanide
 * 	jim - DarkHallow
 * 	
 * 	Thanks for the peek behind the scene.
 * 
 * 	Also, it's been suggested ABC is the original inventor of this movement.
 * 	Thanks, everyone, for sharing your ideas.
 */
public class Evader {
	
	//constants=======================
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_QUARTER = Math.PI / 2;
	static final double ONE_EIGHTH = Math.PI / 4;
	
	static final int ACCELERATION_SLICES = 3;
	static final int BINS = 33;
	static final int MIDDLE_BIN = (int)(BINS / 2);
	static final double LATERAL_BINS = (double)MIDDLE_BIN;
	
	static final double[] DISTANCE_SLICE = {150, 350, 550};
	static final double[] VELOCITY_SLICE = {0, 2, 4, 6};
	
	static double[][][][][] _longFactors = new double[DISTANCE_SLICE.length + 1][VELOCITY_SLICE.length + 1][ACCELERATION_SLICES][3][BINS];
	static double[][][][] _factors = new double[DISTANCE_SLICE.length + 1][VELOCITY_SLICE.length + 1][ACCELERATION_SLICES][BINS];
	static double[][][] _shortFactors = new double[DISTANCE_SLICE.length + 1][VELOCITY_SLICE.length + 1][BINS];
	static double[][] _velocityFactors = new double[VELOCITY_SLICE.length + 1][BINS];
	
	AdvancedRobot _robot;
	RoundRectangle2D.Double _field;
	ArrayList<EnemyShot> _buffer = new ArrayList<EnemyShot>(100);
	EnemyShot _closestBullet;
	double _lastEnemyEnergy = 100;
	double _direction = 1;
	double _lastEnemyX;
	double _lastEnemyY;
	double _lastDistance;
	double _lastEnemyHeading;
	double _lastChalkVelocity;
	long _lastScan = -1;
	long _lastTime = -1;
	
	public Evader(AdvancedRobot robot){
		_robot = robot;
		_field = new RoundRectangle2D.Double(30, 30, _robot.getBattleFieldWidth() - 60, _robot.getBattleFieldHeight() - 60, 100, 100);
		_lastDistance = Double.POSITIVE_INFINITY;
	}
	
	public void move(){
		
		long time = _robot.getTime();
		double nextX = 0, nextY = 0;
		Point2D.Double chalkPosition = new Point2D.Double(_robot.getX(), _robot.getY());
		double chalkVelocity = _robot.getVelocity();
		double chalkHeading = _robot.getHeadingRadians();
		
		//process passed bullets, may there be many of them
		//find the closest bullet
		double lowestDifference = Double.POSITIVE_INFINITY;
		double distanceFromShot = 0;
		
		for(int i = _buffer.size() - 1; i >= 0; i--){
			
			EnemyShot es = (EnemyShot)_buffer.get(i);
			
			double distance = chalkPosition.distance(es.EX, es.EY);
			double difference = es.distanceTraveled(time) - distance;
			
			if(difference > 25){
				_buffer.remove(i);
				continue;
			}
			
			difference = Math.abs(difference);
			
			if(difference < lowestDifference){
				lowestDifference = difference;
				_closestBullet = es;
				distanceFromShot = distance;
			}
		}
		
		if(_lastTime != time){
		    
		    double targetVelocity = 8;
		    
		    if(_lastDistance < 120){
		        if(Math.random() < 0.084d){
					_direction = -_direction;
				}
		        
		        distanceFromShot = _lastDistance;
		        nextX = _lastEnemyX;
		        nextY = _lastEnemyY;
		    }
			else if(_closestBullet != null){
				
				double forwardDanger = projectDanger(chalkVelocity, chalkHeading, _direction, time, chalkPosition, 8d);
				double stopDanger = projectDanger(chalkVelocity, chalkHeading, _direction, time, chalkPosition, 0d);
				double backwardDanger = projectDanger(chalkVelocity, chalkHeading, -_direction, time, chalkPosition, 8d);
				
				if(stopDanger < forwardDanger && stopDanger < backwardDanger){
				    targetVelocity = 0;
				}
				else if(backwardDanger < forwardDanger){
					_direction = -_direction;
				}
				
				nextX = _closestBullet.EX;
			    nextY = _closestBullet.EY;
			}
			
			if(nextX != 0){
			    
			    double shotHeading = Utils.normalAbsoluteAngle(Math.atan2(nextX - chalkPosition.x, nextY - chalkPosition.y));
				
					double preferredAngle = ONE_QUARTER + (ONE_QUARTER / (1 + Math.exp((distanceFromShot - 475)/100)) - ONE_EIGHTH);
					for(int i = 0; i < 2; i++){
						double a = preferredAngle;
						do{
							nextX = chalkPosition.x + Math.sin(shotHeading + a * _direction) * 125;
							nextY = chalkPosition.y + Math.cos(shotHeading + a * _direction) * 125;
							a -= .01745d;
						}while(!_field.contains(nextX, nextY) && a > Util.ONE_EIGHTH);
						
						if(_field.contains(nextX, nextY)){
							break;
						}
						_direction = -_direction;
					}
					
					double angle = Utils.normalRelativeAngle(Math.atan2(nextX - chalkPosition.x, nextY - chalkPosition.y) - chalkHeading);
					_robot.setMaxVelocity(targetVelocity);
					_robot.setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ?  angle > 0 ? angle - Math.PI : angle + Math.PI: angle);
					_robot.setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * 100);
			}
		}
		
		_lastTime = time;
	}
	
	private double projectDanger(double v, double h, double d, long t, Point2D.Double position, double targetV){
		
		double distanceFromShot = position.distance(_closestBullet.EX, _closestBullet.EY);
		double nextX = 0, nextY = 0;
		double shotHeading = Utils.normalAbsoluteAngle(Math.atan2(_closestBullet.EX - position.x, _closestBullet.EY - position.y));
		
		double preferredAngle = ONE_QUARTER + (ONE_QUARTER / (1 + Math.exp((distanceFromShot - 475)/100)) - ONE_EIGHTH);
		//Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		for(int i = 0; i < 2; i++){
			double a = preferredAngle;
			do{
				nextX = position.x + Math.sin(shotHeading + a * d) * 125;
				nextY = position.y + Math.cos(shotHeading + a * d) * 125;
				a -= .01745d;
			}while(!_field.contains(nextX, nextY) && a > Util.ONE_EIGHTH);
			
			if(_field.contains(nextX, nextY)){
				break;
			}
			d = -d;
		}
		
		double angle = Utils.normalRelativeAngle(Math.atan2(nextX - position.x, nextY - position.y) - h);
		double headingDelta = Math.abs(angle) > ONE_QUARTER ?  angle > 0 ? angle - Math.PI : angle + Math.PI: angle;
		double maxHeadingDelta = Math.toRadians(10 - .75 * Math.abs(v));
		h += Math.max(Math.min(headingDelta, maxHeadingDelta), -maxHeadingDelta);
		
		if(targetV == 0d){
		    if(v > 0){
		        v = Math.max(v - 2, 0);
		    }
		    else{
		        v = Math.min(v + 2, 0);
		    }
		}
		else{
			if(Math.abs(angle) > ONE_QUARTER){
				v = Math.max(v - (v < 0 ? 1 : 2), -targetV);
			}
			else{
				v = Math.min(v + (v > 0 ? 1 : 2), targetV);
			}
		}
		
		t++;
	
		nextX = position.x + Math.sin(h) * v;
		nextY = position.y + Math.cos(h) * v;
	
		if(_closestBullet.distanceTraveled(t) - Point2D.distance(nextX, nextY, _closestBullet.EX, _closestBullet.EY) > -20){
		    
		    int index = _closestBullet.getIndex(nextX, nextY);
			
			double pressure = 0;
			
			for(int i = 0; i < BINS; i++){
				pressure += ((
				        _longFactors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][_closestBullet.AcclerationIndex][_closestBullet.WallIndex][index] +
				        _factors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][_closestBullet.AcclerationIndex][index] +
						_shortFactors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][index] +
						_velocityFactors[_closestBullet.VelocityIndex][index]
				)
				/ Math.pow((double)Math.abs(i - index) + 1, 0.7d));
			}
			return pressure;
		}
		return projectDanger(v, h, d, t, new Point2D.Double(nextX, nextY), targetV);
	}

	public void onScannedRobot(ScannedRobotEvent sre){
		
		_lastEnemyHeading = _robot.getHeadingRadians() + sre.getBearingRadians();
		
		long time = _robot.getTime();
		_lastDistance = sre.getDistance();
		double x = _robot.getX();
		double y = _robot.getY();
		double eX = Math.sin(_lastEnemyHeading) * _lastDistance + x;
		double eY = Math.cos(_lastEnemyHeading) * _lastDistance + y;
		double enemyShotPower = _lastEnemyEnergy - (_lastEnemyEnergy = sre.getEnergy());
		double velocity = sre.getVelocity();
		
		//track the enemy's shot
		if(enemyShotPower >= 0.09 && enemyShotPower <= 3.01){
			
			EnemyShot eShot = new EnemyShot();
			eShot.Time = time - 1;
			eShot.setBulletVelocity(enemyShotPower);
			
			double chalkLateralVelocity = -(_robot.getVelocity() * Math.sin(sre.getBearingRadians() + Math.PI));
			double chalkAbsLateralVelocity = Math.abs(chalkLateralVelocity);
			
			int velocityIndex = VELOCITY_SLICE.length;
			for(int i = 0; i < VELOCITY_SLICE.length; i++){
				if(chalkAbsLateralVelocity <= VELOCITY_SLICE[i]){
					velocityIndex = i;
					break;
				}
			}
			int distanceIndex = DISTANCE_SLICE.length;
			for(int i = 0; i < DISTANCE_SLICE.length; i++){
				if(_lastDistance <= DISTANCE_SLICE[i]){
					distanceIndex = i;
					break;
				}
			}
			
			double accel = chalkAbsLateralVelocity - _lastChalkVelocity;
			int accelIndex = 0;
			if(accel != 0){
				accelIndex = accel < 0 ? 1 : 2;
			}
			
			int wallIndex = 0;
			if(x <  70 || x > 730){
				wallIndex++;
			}
			if(y < 70 || y > 530){
				wallIndex++;
			}
			
			eShot.VelocityIndex = velocityIndex;
			eShot.DistanceIndex = distanceIndex;
			eShot.AcclerationIndex = accelIndex;
			eShot.WallIndex = wallIndex;
			eShot.RelativeHeading = Utils.normalAbsoluteAngle(_lastEnemyHeading + Math.PI);
			eShot.Direction = chalkLateralVelocity < 0 ? -1 : 1;
			if(time - _lastScan == 1){
				eShot.EX = _lastEnemyX;
				eShot.EY = _lastEnemyY;
			}
			else{
				eShot.EX = eX - Math.sin(_lastEnemyHeading) * velocity;
				eShot.EY = eY - Math.cos(_lastEnemyHeading) * velocity;;
			}
			_buffer.add(eShot);
			_lastChalkVelocity = chalkAbsLateralVelocity;
		}
		
		_lastEnemyX = eX;
		_lastEnemyY = eY;
		_lastScan = time;
	}
	
	public void onHitByBullet(HitByBulletEvent e){
		
		Bullet bullet = e.getBullet();
		
		//double power = bullet.getPower();
		//double damage = ((4 * power) + (power > 1 ?  2 * (power - 1) : 0));
		_lastEnemyEnergy += bullet.getPower();
		
		int index = _closestBullet.getIndex(bullet.getX(), bullet.getY());
		
		_longFactors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][_closestBullet.AcclerationIndex][_closestBullet.WallIndex][index]++;
		_factors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][_closestBullet.AcclerationIndex][index]++;
		_shortFactors[_closestBullet.DistanceIndex][_closestBullet.VelocityIndex][index]++;
		_velocityFactors[_closestBullet.VelocityIndex][index]++;
	}
	
	public void onBulletHit(BulletHitEvent e){
		Bullet bullet = e.getBullet();
		double power = bullet.getPower();
		_lastEnemyEnergy -= ((4 * power) + (power > 1 ?  2 * (power - 1) : 0));
	}
	
	public void onHitRobot(HitRobotEvent e){
		_lastEnemyEnergy -= 0.6d;
	}
	
	class EnemyShot{
		
		double EX;
		double EY;
		long Time;
		double BulletVelocity;
		double MaxAngle;
		int VelocityIndex;
		int AcclerationIndex;
		int DistanceIndex;
		int WallIndex;
		double RelativeHeading;
		double Direction;
		
		void setBulletVelocity(double shotPower){
			BulletVelocity = 20 - 3 * shotPower;
			MaxAngle = Math.asin(8 / BulletVelocity);
		}
		
		double distanceTraveled(long t){
			return (double)(t - Time) * BulletVelocity;
		}
		
		int getIndex(double x, double y){
			double bearing = Utils.normalRelativeAngle(Math.atan2(x - EX, y - EY) - RelativeHeading);
			int index = (int)(Math.min(Math.round((bearing / MaxAngle) * LATERAL_BINS), LATERAL_BINS) * Direction);
			return index = Math.max(Math.min(MIDDLE_BIN + index, BINS - 1), 0);
		}
	}
}
