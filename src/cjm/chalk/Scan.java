package cjm.chalk;

import robocode.util.Utils;

public class Scan {
    
    //members
    public long Time;
    public double RX;
    public double RY;
    public double EnemyHeading;
    public double Bearing;
    public double BearingRadians;
    public double NormalizedDistance;
    public double Distance;
    public double DistanceDelta;
    public double LateralVelocity;
    public double AdvancingVelocity;
    public double Acceleration;
    public double Velocity;
    public double SinceVelocityChange;
    public double WallTriesForward;
    public double WallTriesBack;
    public double Direction;
    public double BulletVelocity;
    public boolean IsRealBullet;
    public double MaxAngle;
    public boolean Set = false;
    public boolean DeltaSet = false;
	
	Scan(){}
	
	void setBulletVelocity(double shotPower){
		BulletVelocity = 20d - 3d * shotPower;
		MaxAngle = Math.asin(8d / BulletVelocity) * Direction;
	}
	
	public double getDistance(long time){
	    return (double)(time - Time) * BulletVelocity;
	}
	
	public boolean setBearing(double x, double y){
		boolean val = Set;
	    if(!Set){
	    	register(x, y);
	    }
	    if(!DeltaSet){
	    	DistanceDelta = Math.sqrt(Math.pow(RX - x, 2) + Math.pow(RY - y, 2)) - Distance;
	    	DeltaSet = true;
	    }
	    return val;
	}
	
	public void registerHit(double x, double y){
		register(x, y);
	}
	
	private void register(double x, double y){
		BearingRadians = Utils.normalRelativeAngle(Math.atan2(x - RX, y - RY) - EnemyHeading) / MaxAngle;
		Bearing = BearingRadians * 100d;
		Set = true;
	}
}
