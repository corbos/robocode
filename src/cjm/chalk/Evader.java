package cjm.chalk;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.*;
import java.util.*;

/*	Evader - Wave Surfing by Corbos
 * 
 *  Surfing without stats buffers.
 * 
 * 	Wave surfing didn't come easy to me.
 * 	Still, it wouldn't have come at all without the insights and shared resources generously made available at robowiki.net.
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
 * 
 * 	Thanks to Voidious for suggestions and pressure :).
 * 
 *  As of 2.5 - thanks to Voidious for the min/max surfing gem
 *  this has been the first significant improvement of my movement in months. Cheers...
 */
public class Evader {
	
	//constants=======================
	static final double ONE_QUARTER = Math.PI / 2d;
	static final double ONE_EIGHTH = Math.PI / 4d;
	static final double FEELER = 145d;
	static final double DISTANCE_SHOT = 200d;

	static double _distanceShotsFired;
	static double _distanceShotHit;
	static double _damageDone;
	static double _direction = 1d;
	
	AdvancedRobot _robot;
	Rectangle2D.Double _field;
	ArrayList<BulletTracker> _buffer = new ArrayList<BulletTracker>(100);
	ArrayList<BulletTracker> _visitBuffer = new ArrayList<BulletTracker>(100);
	double _lastEnemyEnergy = 100;
	double _lastVelocityChange;
	double _lastVelocity = Double.MAX_VALUE;
	double _lastHeading = Double.MAX_VALUE;
	double _acceleration;
	double _lastEnemyX = 400;
	double _lastEnemyY = 300;
	long _lastTime = -1;
	BulletTracker _lastBullet;
	double _currentDistance;
	double _advancingVelocity;
	double _velocity;
	double _heading;
	boolean _flatten = false;
	
	double _lastDistance = 200d;
	
	BulletTracker[] _closestBullets = new BulletTracker[2];
	
	public Evader(AdvancedRobot robot){
	    
		_robot = robot;
		_field = new Rectangle2D.Double(18, 18, _robot.getBattleFieldWidth() - 36, _robot.getBattleFieldHeight() - 36);
		_currentDistance = 550d + (double)((_robot.getRoundNum() % 3) * 50d);
		
		if(_robot.getRoundNum() > 5){
		    if(_distanceShotHit / Math.max(_distanceShotsFired, 1) < 0.060d){
		        _currentDistance = 425d;
		    }
		    if(_distanceShotHit / Math.max(_distanceShotsFired, 1) > 0.09d){
		        _flatten = true;
		    }
		}
	}
	
	public void move(){
		
		long time = _robot.getTime();
		Point2D.Double chalkPosition = new Point2D.Double(_robot.getX(), _robot.getY());
		_velocity = _robot.getVelocity();
		_heading = _robot.getHeadingRadians();
		
		//process visits
		for(int i = _visitBuffer.size(); --i >= 0;){
		    
		    BulletTracker bt = (BulletTracker)_visitBuffer.get(i);
			
			double distance = chalkPosition.distance(bt._scan.EX, bt._scan.EY);
			
			if(bt.distanceTraveled(time) > distance - bt._scan.BulletVelocity / 2d){
			    BulletTracker.addVisit(bt._scan, chalkPosition.x, chalkPosition.y);
			    _visitBuffer.remove(i);
			}
		}
		
		//process passed bullets, may there be many of them
		//find the closest bullet
		double[] lowestDifferences = new double[]{ Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		_closestBullets = new BulletTracker[]{ null, null };
		
		for(int i = _buffer.size(); --i >= 0;){
			
		    BulletTracker bt = (BulletTracker)_buffer.get(i);
			double difference = bt.distanceTraveled(time) - chalkPosition.distance(bt._scan.EX, bt._scan.EY);
			
			if(difference > 20d){
				_buffer.remove(i);
				continue;
			}
			
			bt.calc();
			
			difference = Math.abs(difference);
		
			if(difference < lowestDifferences[1]){
				_closestBullets[1] = bt;
				lowestDifferences[1] = difference;
			}
			
			if(difference < lowestDifferences[0]){
				lowestDifferences[1] = lowestDifferences[0];
				_closestBullets[1] = _closestBullets[0];
				lowestDifferences[0] = difference;
				_closestBullets[0] = bt;
			}
		}
		
		if(_lastTime != time){
		    
		    double pivotX = 0, pivotY = 0;
		    double targetVelocity = 8d;
		    
		    if(_closestBullets[0] == null){		        
		        pivotX = _lastEnemyX;
		        pivotY = _lastEnemyY;
		    }
			else{				
				pivotX = _closestBullets[0]._scan.EX;
				pivotY = _closestBullets[0]._scan.EY;
			}
		    
		    Projection forward = getProjection(chalkPosition.x, chalkPosition.y, pivotX, pivotY, _direction);
		    Projection backward = getProjection(chalkPosition.x, chalkPosition.y, pivotX, pivotY, -_direction);
		    Projection next = forward;
		    
		    if(_closestBullets[0] != null){
		        
			    boolean changeDirection = false;
			    
			    //full speed====================================
			    double danger = projectDanger(_velocity, _heading, _direction, time, chalkPosition.x, chalkPosition.y, 8d, 0);
			    double nextDanger = projectDanger(_velocity, _heading, -_direction, time, chalkPosition.x, chalkPosition.y, 8d, 0);
			    if(nextDanger < danger){
			        changeDirection = true;
			        danger = nextDanger;
			    }
			    
		        nextDanger = projectDanger(_velocity, _heading, _direction, time, chalkPosition.x, chalkPosition.y, 0d, 0);
			    if(nextDanger < danger){
			        changeDirection = false;
			        targetVelocity = 0;
			    }
			   
			    //now change direction if it's appropriate
			    if(changeDirection){
			        next = backward;
			    }
		    }
			    
		    _direction = next.D;
			double angle = Utils.normalRelativeAngle(Math.atan2(next.X - chalkPosition.x, next.Y - chalkPosition.y) - _heading);
			_robot.setMaxVelocity(targetVelocity);
			_robot.setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ?  angle > 0 ? angle - Math.PI : angle + Math.PI: angle);
			_robot.setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * 50d);
			
			if(_lastVelocity != Double.MAX_VALUE){
			    
				if(ChalkUtils.sign(_lastVelocity) == ChalkUtils.sign(_velocity)){
				    _acceleration = Math.abs(_velocity) - Math.abs(_lastVelocity);
				}
				else{
				    _acceleration = Math.abs(_velocity - _lastVelocity);
				}
			}
			else{
			    _acceleration = _velocity;
			}
			_acceleration = Math.max(Math.min(_acceleration, 2d), -2d);
		}
		_lastTime = time;
		_lastVelocity = _velocity;
	}
	
	private double projectDanger(double v, double h, double d, long t, double mX, double mY, double targetV, int index){
		
		if(_closestBullets[index] == null){
			return 0d;
		}
		
		MoveState cursor = new MoveState();
		cursor.RobotX = mX;
		cursor.RobotY = mY;
		cursor.Heading = h;
		cursor.Velocity = v;
		
		Projection next = getProjection(cursor.RobotX, cursor.RobotY, _closestBullets[index]._scan.EX, _closestBullets[index]._scan.EY, d);
		
		while(true){
			
			double desiredHeading = Math.atan2(next.X - cursor.RobotX, next.Y - cursor.RobotY );
			cursor = MoveState.getNextState(cursor, targetV, desiredHeading);
			
			t++;
			
			if(_closestBullets[index].distanceTraveled(t) - Point2D.distance(cursor.RobotX, cursor.RobotY, _closestBullets[index]._scan.EX, _closestBullets[index]._scan.EY) > -18d){
				double pressure = _closestBullets[index].getPressure(cursor.RobotX, cursor.RobotY, t, _flatten);
				if(index == 0){
					double danger = projectDanger(cursor.Velocity, cursor.Heading, next.D, t, cursor.RobotX, cursor.RobotY, 8d, 1);
					double nextDanger = projectDanger(cursor.Velocity, cursor.Heading, -next.D, t, cursor.RobotX, cursor.RobotY, 8d, 1);
				    if(nextDanger < danger){
				        danger = nextDanger;
				    }
			        nextDanger = projectDanger(cursor.Velocity, cursor.Heading, next.D, t, cursor.RobotX, cursor.RobotY, 0d, 1);
				    if(nextDanger < danger){
				    	danger = nextDanger;
				    }
				    return pressure + danger;
				}
				else{
					return pressure;
				}
			}
			
			next = getProjection(cursor.RobotX, cursor.RobotY, _closestBullets[index]._scan.EX, _closestBullets[index]._scan.EY, next.D);
		}
	}
	
	public double getWallTries(double heading, double dir, double x, double y, double distance){

		double wallIncrement = 0.0407d * dir;
		double eHeading = heading;
		double nextX = 0;
		double nextY = 0;
		double wallTries = -1;
		do{
		    eHeading += wallIncrement;
		    nextX = x + Math.sin(eHeading) * distance;
		    nextY = y + Math.cos(eHeading) * distance;
		    wallTries++;
		}while(_field.contains(nextX, nextY) && wallTries < 20);
		
		return wallTries;
	}

	public void onScannedRobot(ScannedRobotEvent sre){
		
		double enemyHeading = Utils.normalAbsoluteAngle(_heading + sre.getBearingRadians());
		double distance = sre.getDistance();
		_lastDistance = distance;
		long time = _robot.getTime();
		double x = _robot.getX();
		double y = _robot.getY();
		double eX = Math.sin(enemyHeading) * distance + x;
		double eY = Math.cos(enemyHeading) * distance + y;
		_advancingVelocity = -Math.cos(sre.getHeadingRadians() - enemyHeading) * sre.getVelocity();
		double enemyShotPower = _lastEnemyEnergy - (_lastEnemyEnergy = sre.getEnergy());
		double lateralVelocity = -(_velocity * Math.sin(sre.getBearingRadians() + Math.PI));
		double rAV = -(_velocity * Math.cos(sre.getBearingRadians() + Math.PI));
		double direction = lateralVelocity < 0 ? -1d : 1d;
		
		_lastVelocityChange++;
		if(Math.abs(_lastVelocity - _velocity) > 0.1d){
		    _lastVelocityChange = 0;
		}
		
		double velocityChangeValue = Math.min(_lastVelocityChange / (distance / 14.3d), 3d);
		
		//track the enemy's shot
		if(enemyShotPower > 0 && enemyShotPower <= 3.01d){
		    
		    if(_lastBullet != null){
		        
		    	_lastBullet.setBulletVelocity(enemyShotPower);
		    	_lastBullet._scan.BulletFired = true;
		        _buffer.add(_lastBullet);
		        
		        if(_lastBullet._scan.Distance > DISTANCE_SHOT){
		            _distanceShotsFired++;
		        }
		    }
		}
		else if(_lastBullet != null){
			_lastBullet.setBulletVelocity(1.9d);
		}
		
		//visits
		if(_lastBullet != null){
		    _visitBuffer.add(_lastBullet);
		}
		
		_lastBullet = new BulletTracker(
		        distance, 
		        Math.abs(_velocity),
		        Math.abs(lateralVelocity), 
		        rAV, 
		        velocityChangeValue, 
		        _acceleration,
		        getWallTries(enemyHeading + Math.PI, direction, eX, eY, distance), 
		        getWallTries(enemyHeading + Math.PI, -direction, eX, eY, distance), 
		        time, 
		        direction,
		        Utils.normalAbsoluteAngle(enemyHeading + Math.PI),
		        eX,
		        eY);
		
		_lastEnemyX = eX;
		_lastEnemyY = eY;
	}
	
	public void onHitByBullet(HitByBulletEvent e){
	    
	    Bullet bullet = e.getBullet();
		_damageDone += ChalkUtils.getDamage(bullet.getPower());
	    _lastEnemyEnergy += bullet.getPower() * 3d;
	    
	    if(_closestBullets[0] != null){
			
			BulletTracker.addHit(_closestBullets[0]._scan, bullet.getX(), bullet.getY());
			_buffer.remove(_closestBullets[0]);
			
			if(_closestBullets[0]._scan.Distance > DISTANCE_SHOT){
			    _distanceShotHit++;
			}
			
			for(int i = 0 ; i <_buffer.size(); i++){
			    ((BulletTracker)_buffer.get(i)).calcHits(true);
			}
	    }
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){
	    Bullet bullet = e.getBullet();
	    EnemyScan best = null;
	    long time = _robot.getTime();
	    double distance;
	    double shortestDistance = Double.MAX_VALUE;
	    for(int i = 0; i < _buffer.size(); i++){
	        BulletTracker bt = (BulletTracker)_buffer.get(i);
	        distance = Math.abs(Point2D.distance(bt._scan.EX, bt._scan.EY, bullet.getX(), bullet.getY()) - bt.distanceTraveled(time));
	        if(distance < shortestDistance){
	            shortestDistance = distance;
	            best = bt._scan;
	        }
	    }
	    if(best != null){
	        BulletTracker.addHit(best, bullet.getX(), bullet.getY());
	        _buffer.remove(best);
	        for(int i = 0 ; i <_buffer.size(); i++){
			    ((BulletTracker)_buffer.get(i)).calcHits(true);
			}
	    }
	}
	
	public void onBulletHit(BulletHitEvent e){
		Bullet bullet = e.getBullet();
		_lastEnemyEnergy -= ChalkUtils.getDamage(bullet.getPower());
	}
	
	public void onHitRobot(HitRobotEvent e){
		_lastEnemyEnergy -= 0.6d;
	}
	
	public String getStats(){
	    return 
	    	"Distance Shots Fired: " + _distanceShotsFired + "\n" +
	    	"Distance Hits: " + _distanceShotHit + "\n" +
	    	"Distance Hit %: " + ((_distanceShotHit / _distanceShotsFired) * 100) + "\n" +
	    	("Damage/Round: " + _damageDone / (double)(_robot.getRoundNum() + 1) + "\n");
	    /*
	    	("D: " + BulletTracker._featureData[0][0] + "\n") +
		    ("LV: " + BulletTracker._featureData[1][0]  + "\n") +
		    ("AV: " + BulletTracker._featureData[2][0]  + "\n") +
		    ("VC: " + BulletTracker._featureData[3][0]  + "\n") +
		    ("A: " + BulletTracker._featureData[4][0]  + "\n") +
		    ("WF: " + BulletTracker._featureData[5][0]  + "\n") +
		    ("WB: " + BulletTracker._featureData[6][0] );
		    */
		   
	}
	
	private double getEvasion(double distance){
		if(_lastDistance < 150d || (_lastDistance < 225d && _advancingVelocity > 4.5d)){
	        return 2.67d;
	    }
	    if(distance < _currentDistance){
	        return ONE_QUARTER + (ONE_QUARTER / (1d + Math.exp((distance - _currentDistance) / 100d)) - ONE_EIGHTH);
	    }
	    return ONE_QUARTER;
	}
	
	private Projection getProjection(double rX, double rY, double eX, double eY, double direction){
	    
	    double nextX = 0;
	    double nextY = 0;
	    double shotHeading = Utils.normalAbsoluteAngle(Math.atan2(eX - rX, eY - rY));
	    double distance = Point2D.distance(rX, rY, eX, eY);
		double preferredAngle = getEvasion(distance);
		
		//Escape Smoothing
		double dir = direction;
		 if(_lastDistance < 150d || (_lastDistance < 225d && _advancingVelocity > 4.5d)){
			for(int i = 0; i < 2; i++){
				double a = preferredAngle;
				do{
					nextX = rX + Math.sin(shotHeading + a * dir) * 110d;
					nextY = rY + Math.cos(shotHeading + a * dir) * 110d;
					a += 0.052d;
				}while(!_field.contains(nextX, nextY) && a < Math.PI);
				
				if(_field.contains(nextX, nextY)){
					return new Projection(nextX, nextY, dir);
				}
				dir = -dir;
			}
		}
		
		//Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		//Normal smoothing
		dir = direction;
		for(int i = 0; i < 2; i++){
			double a = preferredAngle;
			do{
				nextX = rX + Math.sin(shotHeading + a * dir) * FEELER;
				nextY = rY + Math.cos(shotHeading + a * dir) * FEELER;
				a -= 0.052d;
			}while(!_field.contains(nextX, nextY) && a > ONE_EIGHTH);
			
			if(_field.contains(nextX, nextY)){
				return new Projection(nextX, nextY, dir);
			}
			dir = -dir;
		}
		
		dir = direction;
		for(int i = 0; i < 2; i++){
			double a = preferredAngle;
			do{
				nextX = rX + Math.sin(shotHeading + a * dir) * FEELER;
				nextY = rY + Math.cos(shotHeading + a * dir) * FEELER;
				a += 0.052d;
			}while(!_field.contains(nextX, nextY) && a < Math.PI);
			
			if(_field.contains(nextX, nextY)){
				return new Projection(nextX, nextY, dir);
			}
			dir = -dir;
		}
		
		dir = direction;
		for(int i = 0; i < 2; i++){
			double a = ONE_EIGHTH;
			do{
				nextX = rX + Math.sin(shotHeading + a * dir) * FEELER;
				nextY = rY + Math.cos(shotHeading + a * dir) * FEELER;
				a -= 0.052d;
			}while(!_field.contains(nextX, nextY) && a > 0);
			
			if(_field.contains(nextX, nextY)){
				break;
			}
			dir = -dir;
		}
		
		return new Projection(nextX, nextY, dir);
	}
	
	//============================
	class Projection{
	    
	    double X;
	    double Y;
	    double D;
	    
	    Projection(double x, double y, double d){
	        X = x;
	        Y = y;
	        D = d;
	    }
	}
}
