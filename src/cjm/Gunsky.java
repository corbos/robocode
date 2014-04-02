package cjm;

import robocode.*;
import cjm.ANN.SmallNet;
import java.util.Vector;
import java.awt.geom.*;

public class Gunsky {
	
	private double _shotPower;
	private double[] _input = new double[6];
	private double[] _output = new double[1];
	private AdvancedRobot _robot;
	private SmallNet[] _nets = new SmallNet[4];
	private Vector<VirtualBullet> _bulletQueue = new Vector<VirtualBullet>();
	private double _lastVelocity;
	private double _lastHeading;
	private double _lastTick = -5;
	private double _enemyX;
	private double _enemyY;
	//shot stats================
	private final int NNIndex = 0;
	private final int CTLTIndex = 1;
	private double[][] _gunStats = new double[][]{{1,1,1},{1,1,1}};
	
	public Gunsky(AdvancedRobot robot){
		_robot = robot;
		_nets[0] = new SmallNet(4, 3, 1, 0.15d);
		_nets[1] = new SmallNet(4, 3, 1, 0.15d);
		_nets[2] = new SmallNet(4, 3, 1, 0.15d);
		_nets[3] = new SmallNet(5, 4, 1, 0.15d);
	}
	
	public void clear(){
		_bulletQueue.clear();
	}
	
	public void fire(){
		if(_robot.getOthers() > 0){
			_robot.setFire(_shotPower);
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent sre, double lastEnemyBearing){
		
		long time = _robot.getTime();
		double x = _robot.getX();
		double y = _robot.getY();
		double distance = sre.getDistance();
		double heading = sre.getHeadingRadians();
		_enemyX = x + distance * Math.cos(lastEnemyBearing);
		_enemyY = y + distance * Math.sin(lastEnemyBearing);
		double enemyHeading = Util.getHeadingPoints(x, y, _enemyX, _enemyY);
		double velocity = sre.getVelocity();
		int netIndex = 0;
		double factor = 0.8143399d;
		int distanceIndex;
		
		if(distance < 200){
			_shotPower = 3;
		}
		else if(distance < 400){
			_shotPower = 2;
			netIndex = 1;
			factor = 0.6082456d;
		}
		else{
			_shotPower = 1;
			netIndex = 2;
			factor = 0.4899573d;
		}
		if((sre.getEnergy() / 4) < _shotPower){
			_shotPower = sre.getEnergy() / 4;
			if((_robot.getEnergy() - 0.1) < _shotPower){
				_shotPower = _robot.getEnergy() - 0.1;
			}
			netIndex = 3;
		}
		
		//check virtual bullets============================
		//Decaying average from GrubbmGrb
		double enemyDistance, bulletDistance;
		VirtualBullet vb;
		for(int i = _bulletQueue.size() - 1; i > -1; i--){
			vb = _bulletQueue.get(i);
			enemyDistance = vb.getEnemyDistance(_enemyX, _enemyY);
			bulletDistance = vb.getBulletDistance(time);
			if(bulletDistance > enemyDistance + 30){
				_bulletQueue.removeElementAt(i);
			}
			else if(Math.abs(enemyDistance - bulletDistance) < 20){
				//train========================================
				_input[0] = vb.Distance;
				_input[1] = vb.HX;
				_input[2] = vb.VX;
				_input[3] = vb.AX;
				distanceIndex = getStatIndex(vb.Distance);
				if(vb.NetIndex < 3){
					_input[4] = vb.getBearing(_enemyX, _enemyY) / vb.Factor;
					_output = _nets[vb.NetIndex].train(_input);
					_gunStats[NNIndex][distanceIndex] = (_gunStats[NNIndex][distanceIndex] * 0.99 + (vb.isNNHit(_output[0] * vb.Factor, _enemyX, _enemyY, time) ? 0.01 : 0));
				}
				_input[4] = vb.BulletVelocity;
				_input[5] = vb.getBearing(_enemyX, _enemyY) / vb.Factor;
				_output = _nets[3].train(_input);
				if(vb.NetIndex == 3){
					_gunStats[NNIndex][distanceIndex] = (_gunStats[NNIndex][distanceIndex] * 0.99 + (vb.isNNHit(_output[0] * vb.Factor, _enemyX, _enemyY, time) ? 0.01 : 0));
				}
				
				_gunStats[CTLTIndex][distanceIndex] = (_gunStats[CTLTIndex][distanceIndex] * 0.99 + (vb.isCTLTHit(_enemyX, _enemyY, time) ? 0.01 : 0));
			}
		}
		
		double hX = Math.asin(Math.sin(sre.getHeadingRadians() - sre.getBearingRadians() + _robot.getHeadingRadians()));
		double accel = velocity - _lastVelocity;
			
		if(_lastTick + 1 == time){
			
			VirtualBullet bb = new VirtualBullet(enemyHeading, distance, x, y, time);
			
			bb.HX = hX;
			bb.VX = velocity * 10000d;
			bb.AX = accel * 10000d;
			bb.NetIndex = netIndex;
			bb.Factor = factor;
			//==============
			bb.DeltaHeading = heading - _lastHeading;
				
			bb.setBulletPower(_shotPower);
			_bulletQueue.add(bb);
				
			double angle = 0;
			
			//http://www.robowiki.net/cgi-bin/robowiki?CircularTargeting
			//Derived from robowiki.net & GrubbmGrb
			double projectedTime = 0;
			double dX = _enemyX;
			double dY = _enemyY;
			double currentHeading = heading;
			do{
				projectedTime++;
				dX += Math.sin(currentHeading) * velocity;
				dY += Math.cos(currentHeading) * velocity;
				currentHeading += bb.DeltaHeading;

			}while(Point2D.distance(x, y, dX, dY) > projectedTime * bb.BulletVelocity);
			
			bb.CTLTHeading = Util.getHeadingPoints(x, y, dX, dY);
			
			distanceIndex = getStatIndex(bb.Distance);
			if(_gunStats[CTLTIndex][distanceIndex] < _gunStats[NNIndex][distanceIndex]){
				_input[0] = bb.Distance;
				_input[1] = bb.HX;
				_input[2] = bb.VX;
				_input[3] = bb.AX;
				if(bb.NetIndex == 3){
					_input[4] = bb.BulletVelocity;
				}
				
				_output = _nets[bb.NetIndex].getNext(_input);	
				angle = Util.getRelativeBearing(_robot.getGunHeadingRadians(), x, y, _enemyX, _enemyY) + (_output[0] * bb.Factor);
				if(Math.abs(angle) > Math.PI){
					angle += (angle < 0 ? Util.FOUR_QUARTERS : -Util.FOUR_QUARTERS);
				}
			}
			else{
				angle = Util.getRelativeBearing(_robot.getGunHeadingRadians(), x, y, dX, dY);
			}
			
			_robot.setTurnGunRightRadians(angle);
		}
		_lastVelocity = velocity;
		_lastHeading = heading;
		_lastTick = time;
	}
	
	private int getStatIndex(double distance){
		if(distance < 300){
			return 0;
		}
		else if(distance < 400){
			return 1;
		}
		else{
			return 2;
		}
	}
}
