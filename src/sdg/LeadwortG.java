package sdg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class LeadwortG extends AdvancedRobot {

	private double _lastHeading;
	private Rectangle2D.Double _battlefield;

	public void run() {

		_battlefield = new Rectangle2D.Double(18, 18, getBattleFieldWidth() - 36, getBattleFieldHeight() - 36);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		this.setColors(Color.PINK, Color.PINK, Color.PINK);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent e) {

		setTurnRadarLeftRadians(getRadarTurnRemaining());

		double bulletWeight = 2d;
		double bulletPower = Math.min(bulletWeight, getEnergy());
		double x = getX();
		double y = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double velocity = e.getVelocity();
		double heading = e.getHeadingRadians();
		double delta = heading - _lastHeading;
		_lastHeading = heading;

		double tick = 0;
		double predictedX = x + e.getDistance() * Math.sin(absoluteBearing);
		double predictedY = y + e.getDistance() * Math.cos(absoluteBearing);
		while (++tick * (20.0 - 3d * bulletPower) < Point2D.Double.distance(x, y, predictedX, predictedY)) {
			predictedX += Math.sin(heading) * velocity;
			predictedY += Math.cos(heading) * velocity;
			heading += delta;
			if (!_battlefield.contains(predictedX, predictedY)) {
				predictedX = Math.min(Math.max(18.0, predictedX), getBattleFieldWidth() - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY), getBattleFieldHeight() - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - x, predictedY - y));

		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

		fire(bulletWeight);
	}
}
