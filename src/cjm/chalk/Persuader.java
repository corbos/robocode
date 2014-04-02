package cjm.chalk;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.*;

public class Persuader {
	
	static GunClusterer _mainGun;
	static GunClusterer _adaptiveGun;
	static int _currentGun;
   
    //gun stats
	static double _shotsFired;
	static double _shotsHit;
	
	AdvancedRobot _robot;
	java.awt.geom.Rectangle2D.Double _field;
	Vector<Scan> _nodeQueue = new Vector<Scan>(100);
	ScannedRobotEvent _lastScan;
	boolean _aiming = false;
	
	double _lastHeading = Double.MAX_VALUE;
	double _lastVelocity = Double.MAX_VALUE;
	double _lastVelocityChange;
	Point2D.Double _enemyPoint = new Point2D.Double();
	
	double _coolingRate;
	
	public Persuader(AdvancedRobot robot){
		_robot = robot;
		_coolingRate = _robot.getGunCoolingRate();
		_field = new java.awt.geom.Rectangle2D.Double(18d, 18d, _robot.getBattleFieldWidth() - 36d, _robot.getBattleFieldHeight() - 36d);
		if(_mainGun == null){
			_mainGun = new GunClusterer(30000, 50, 1d, robot);
		}
		if(_adaptiveGun == null){
			_adaptiveGun = new GunClusterer(5000, 45, 0.2d, robot);
		}
		_mainGun.clear();
		_adaptiveGun.clear();
	}
	
	public void process(){
		
		if(_lastScan == null){
			return;
		}
		
		if(_robot.getEnergy() == 0){
	        boolean hasReal = false;
	        for(int i = _nodeQueue.size(); --i >= 0;){
			    Scan s = (Scan)_nodeQueue.get(i);
			    if(s.IsRealBullet){
			        hasReal = true;
			    }
	        }
	        if(!hasReal){
	            return;
	        }
	    }
		
		double enemyHeading = _robot.getHeadingRadians() + _lastScan.getBearingRadians();
		double relativeHeading = _lastScan.getHeadingRadians() - enemyHeading;
		double heading = _lastScan.getHeadingRadians();
		double x = _robot.getX();
		double y = _robot.getY();
		double distance = _lastScan.getDistance();
		_enemyPoint.setLocation(Math.sin(enemyHeading) * distance + x, Math.cos(enemyHeading) * distance + y);
		long time = _robot.getTime();
		double gunHeading = _robot.getGunHeadingRadians();
			    
			//calculate indexes=====================		
		double velocity = _lastScan.getVelocity();
		double absVelocity = Math.abs(velocity);
		double lateralVelocity = velocity * Math.sin(relativeHeading);
		double advancingVelocity = -Math.cos(relativeHeading) * velocity;
		double direction = lateralVelocity < 0 ? -1 : 1;
		
		double energy = _robot.getEnergy();
		double hitPercent = _shotsHit / Math.max(_shotsFired, 1d);
		double shotPower = 1.90d;
		if(distance < 135d){
			shotPower = 3.0d;
		}
		else if(energy < 10d && _lastScan.getEnergy() > energy){
			shotPower = 1d;
		}
		else if(_robot.getRoundNum() > 5 && hitPercent > 0.16d){
			shotPower = 2.5d;
		}
		else if(distance < 285d){
			shotPower = 2.2d;
		}
		shotPower = Math.min(Math.min(Math.max(_lastScan.getEnergy() / 4d, 0.1d), shotPower), _robot.getEnergy() - 0.100001d);
		
		if(Debug.IS_TC){
		    shotPower = 3d;
		}
		double acceleration = 0;
		if(_lastVelocity != Double.MAX_VALUE){
		    
			if(ChalkUtils.sign(_lastVelocity) == ChalkUtils.sign(velocity)){
				acceleration = Math.abs(velocity) - Math.abs(_lastVelocity);
			}
			else{
				acceleration = Math.abs(velocity - _lastVelocity);
			}
		}
		else{
			acceleration = velocity;
		}
		acceleration = Math.max(Math.min(acceleration, 2d), -2d);
		
		_lastVelocityChange++;
		if(Math.abs(_lastVelocity - velocity) > 0.1){
		    _lastVelocityChange = 0;
		}
		double velocityChangeValue = Math.min(_lastVelocityChange / (distance / 14.3d), 4d);	
		
		//wall distance forward
		double wallTries = getWallTries(enemyHeading, direction, x, y, distance);
		double wallTriesBack = getWallTries(enemyHeading, -direction, x, y, distance);
		
		lateralVelocity = Math.abs(lateralVelocity);
		   
		Scan scan = new Scan();
		scan.Time = time - 1;
		scan.LateralVelocity = lateralVelocity / 8d;
		scan.AdvancingVelocity = advancingVelocity / 16d;
		scan.WallTriesForward = wallTries / 20d;
		scan.WallTriesBack = wallTriesBack / 20d;
		scan.NormalizedDistance = distance / 800d;
		scan.Distance = distance;
		scan.Velocity = absVelocity / 8d;
		scan.Acceleration = acceleration / 4d;
		scan.SinceVelocityChange = velocityChangeValue / 4d;
		scan.Direction = direction;
		scan.EnemyHeading = enemyHeading;
		scan.RX = x;
		scan.RY = y;
		if(shotPower > 0){
		    scan.setBulletVelocity(shotPower);
		}
		else{
		    scan.setBulletVelocity(1.90d);
		}
		
		_nodeQueue.add(scan);
		
		//check for virtual hits
		_mainGun.checkVirtualBullets(time, _enemyPoint.x, _enemyPoint.y);
		_adaptiveGun.checkVirtualBullets(time, _enemyPoint.x, _enemyPoint.y);
		
		for(int i = _nodeQueue.size(); --i >= 0;){
		    Scan s = (Scan)_nodeQueue.get(i);
		    if(s.getDistance(time) > distance - s.BulletVelocity * 0.5d){
		       if(!s.setBearing(_enemyPoint.x, _enemyPoint.y)){
		    	   _mainGun.addScan(s);
		    	   _adaptiveGun.addScan(s);
		       }
		    }
		  
		    if(s.getDistance(time) > (distance + 20d)){
		        _nodeQueue.remove(i);
		        _mainGun.removePassed(s);
		    	_adaptiveGun.removePassed(s);
		    }
		}
		
		if(shotPower > 0 && _robot.getOthers() > 0 && _robot.getGunHeat() / _coolingRate < 2d){
			
			double mainRating = _mainGun.getRatingPercent();
			if(_robot.getRoundNum() > 3 && _adaptiveGun.getRatingPercent() > mainRating + 0.007d){
				_currentGun = 1;
			}
			else{
				_currentGun = 0;
			}
			
			double bearing;
			if(_currentGun == 0){
				bearing = _mainGun.projectBearing(scan, x, y, enemyHeading);
			}
			else{
				bearing = _adaptiveGun.projectBearing(scan, x, y, enemyHeading);
			}
			
			_aiming = true;
		    if(bearing < Double.MAX_VALUE && distance > 80d){
		    	_robot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading + (bearing * scan.MaxAngle) - gunHeading));
		    }
		    else{
		    	 //http://www.robowiki.net/cgi-bin/robowiki?CircularTargeting
				//Derived from robowiki.net & GrubbmGrb
			    //this should only be used in ultra-close-range
			    //or once for an early anti-Musashi Trick shot
				double projectedTime = 0;
				double dX = _enemyPoint.x;
				double dY = _enemyPoint.y;
				double currentHeading = heading;
				double deltaHeading = heading - _lastHeading;
				do{
					projectedTime++;
					dX += Math.sin(currentHeading) * velocity;
					dY += Math.cos(currentHeading) * velocity;
					currentHeading += deltaHeading;
	
				}while(Point2D.distance(x, y, dX, dY) > projectedTime * scan.BulletVelocity && _field.contains(dX, dY));
				
				_robot.setTurnGunRightRadians(Utils.normalRelativeAngle(Math.atan2(dX - x, dY - y) - gunHeading));
		    }
		    
		    if((Math.abs(_robot.getGunTurnRemainingRadians()) < Math.atan(18d / distance) || distance < 125d) && _robot.setFireBullet(shotPower) != null){
			    
				scan.IsRealBullet = true;
				_aiming = false;
				_shotsFired++;
				
				 if(bearing < Double.MAX_VALUE){
					if(_currentGun == 0){
						double br = _adaptiveGun.projectBearing(scan, x, y, enemyHeading);
						if(br != Double.MAX_VALUE){
							_mainGun.takeVirtualShot(scan, bearing * scan.MaxAngle);
							_adaptiveGun.takeVirtualShot(scan, br * scan.MaxAngle);
						}
					}
					else{
						double br = _mainGun.projectBearing(scan, x, y, enemyHeading);
						if(br != Double.MAX_VALUE){
							_adaptiveGun.takeVirtualShot(scan, bearing * scan.MaxAngle);
							_mainGun.takeVirtualShot(scan, br * scan.MaxAngle);
						}
					}
				 }
			}
		}

		if(!_aiming){
		    _robot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - gunHeading));
		}
		
		_lastVelocity = velocity;
		_lastScan = null;
		_lastHeading = heading;
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
		_lastScan = sre;
	}
	
	public String getStats(){
	    return "Total Chalk Hit %: " + ((_shotsHit / _shotsFired) * 100) + "\n" +
	    	"Total Shots: " + _shotsFired + "\n" +
		    "Main Gun %: " + _mainGun.getRatingPercent() + "\n" +
	    	"Adaptive Gun% Rating: " + _adaptiveGun.getRatingPercent() + "\n";
	}
	
	public void onBulletHit(BulletHitEvent e){
		
		double bulletX = e.getBullet().getX();
		double bulletY = e.getBullet().getY();
		long time = _robot.getTime();
		double greatestDistance = Double.MAX_VALUE;
		Scan best = null;
		for(int i = _nodeQueue.size(); --i >= 0;){
			Scan s = (Scan)_nodeQueue.get(i);
			double d = Math.abs(s.getDistance(time) - Point2D.distance(s.RX, s.RY, bulletX, bulletY));
			if(d < greatestDistance && d < 30d){
				greatestDistance = d;
				best = s;
			}
		}
		
		if(best != null){
			best.registerHit(bulletX, bulletY);
		}
		
		_shotsHit++;
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){

		double bulletX = e.getBullet().getX();
		double bulletY = e.getBullet().getY();
		long time = _robot.getTime();
		double greatestDistance = Double.MAX_VALUE;
		Scan best = null;
		for(int i = _nodeQueue.size(); --i >= 0;){
			Scan s = (Scan)_nodeQueue.get(i);
			double d = Math.abs(s.getDistance(time) - Point2D.distance(s.RX, s.RY, bulletX, bulletY));
			if(d < greatestDistance && d < 30d){
				greatestDistance = d;
				best = s;
			}
		}
		
		if(best != null){
			best.registerHit(bulletX, bulletY);
		}
	}
}
