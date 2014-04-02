package cjm.chalk;

import robocode.util.Utils;

public class EnemyScan {
     
    double EX;
	double EY;
	long Time;
	double Distance;
	double Velocity;
	double Acceleration;
	double LateralVelocity;
	double AdvancingVelocity;
	double VelocityChange;
	double ShotPower;
	double BulletVelocity;
	double RelativeHeading;
	double Direction;
	double Bearing;
	double WallForward;
	double WallBackward;
	double VisitBearing;
	double MaxAngle;
	boolean BulletFired = false;
	
	void setBearing(double x, double y){
	    Bearing = Utils.normalRelativeAngle(Math.atan2(x - EX, y - EY) - RelativeHeading) / MaxAngle * 100d;
	}
	
	void setVisitBearing(double x, double y){
	    VisitBearing = Utils.normalRelativeAngle(Math.atan2(x - EX, y - EY) - RelativeHeading) / MaxAngle * 100d;
	}
}
