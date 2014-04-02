package cjm;

public class Util {

	public static final double THREE_QUARTERS = (3 * Math.PI)/2;
	public static final double ONE_QUARTER = Math.PI/2;
	public static final double ONE_EIGHTH = Math.PI/4;
	public static final double CLOSE_ANGLE = Math.PI/16;
	public static final double FOUR_QUARTERS = Math.PI * 2;
	
	public static double getUnitCircleAngle(double angle){
		return -(angle + THREE_QUARTERS);
	}
	
	public static double getHeadingPoints(double originX, double originY, double targetX, double targetY){
		return (Math.atan2(targetX - originX, targetY - originY) + Util.FOUR_QUARTERS) % Util.FOUR_QUARTERS;
	}
	
	public static double getRelativeBearing(double itemHeading, double originX, double originY, double targetX, double targetY){
		double a = ((Math.atan2(targetX - originX, targetY - originY) + Util.FOUR_QUARTERS) % Util.FOUR_QUARTERS) - itemHeading;
		if(Math.abs(a) > Math.PI){
			a += (a < 0 ? Util.FOUR_QUARTERS : -Util.FOUR_QUARTERS);
		}
		return a;
	}
	
	public static double getBulletDamage(double power){
		return (4 * power) + (power > 1 ?  2 * (power - 1) : 0);
	}
}
