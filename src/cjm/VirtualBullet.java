package cjm;

import java.awt.geom.Point2D;

public class VirtualBullet {
	
	public double VX;
	public double VY;
	public double AX;
	public double RobotX;
	public double RobotY;
	public double Distance;
	public double EnemyHeading;
	public double BulletPower;
	public long StartingTick;
	public double BulletVelocity;
	public double HX;
	public int NetIndex;
	public double Factor;
	//CT-LT====================
	public double DeltaHeading;
	public double CTLTHeading;
	
	public VirtualBullet(double h, double d, double rX, double rY, long t){
		RobotX = rX;
		RobotY = rY;
		Distance = d;
		StartingTick = t;
		EnemyHeading = h;
	}

	public void setBulletPower(double bulletPower){
		BulletPower = bulletPower;
		BulletVelocity = 20 - bulletPower * 3;
	}
	
	public double getEnemyDistance(double eX, double eY){
		return Point2D.distance(eX, eY, RobotX, RobotY);
	}
	
	public double getBulletDistance(long t){
		return (double)(t - StartingTick) * BulletVelocity;
	}
	
	public double getBearing(double eX, double eY){
		return Util.getRelativeBearing(EnemyHeading, RobotX, RobotY, eX, eY);
	}
	
	public boolean isNNHit(double bearing, double eX, double eY, long t){
		double adjustedHeading = EnemyHeading + bearing;
		return Point2D.distance(eX, eY, RobotX + getBulletDistance(t) * Math.sin(adjustedHeading), RobotY + getBulletDistance(t)  * Math.cos(adjustedHeading)) <= 20;
	}
	
	public boolean isCTLTHit(double eX, double eY, long t){
		return Point2D.distance(eX, eY, RobotX + getBulletDistance(t) * Math.sin(CTLTHeading), RobotY + getBulletDistance(t)  * Math.cos(CTLTHeading)) <= 20;
	}
}
