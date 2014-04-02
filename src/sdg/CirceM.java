package sdg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class CirceM extends AdvancedRobot {

	static final double ONE_QUARTER = Math.PI / 2;
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_EIGHTH = Math.PI / 4;

	private double _direction = 1;
	private RoundRectangle2D.Double _battlefield;

	public void run() {

		_battlefield = new RoundRectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60,
				100, 100);
		this.setColors(Color.CYAN, Color.CYAN, Color.CYAN);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent sre) {

		move(sre);

		setTurnRadarLeftRadians(getRadarTurnRemaining());

		double enemyHeading = getHeadingRadians() + sre.getBearingRadians();
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - getGunHeadingRadians()));
	}

	private void move(ScannedRobotEvent sre) {

		double distance = sre.getDistance();
		double enemyHeading = getHeadingRadians() + sre.getBearingRadians();
		double x = getX();
		double y = getY();

		double nextX = 0, nextY = 0;
		double preferredAngle = ONE_QUARTER + (ONE_QUARTER / (1 + Math.exp((distance - 350) / 100)) - ONE_EIGHTH);

		// Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		for (int i = 0; i < 2; i++) {
			double a = preferredAngle;
			do {
				nextX = x + Math.sin(enemyHeading + a * _direction) * 85;
				nextY = y + Math.cos(enemyHeading + a * _direction) * 85;
				a -= .01745d;
			} while (!_battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);

			if (_battlefield.contains(nextX, nextY)) {
				break;
			}
			_direction = -_direction;
		}

		double angle = Utils.normalRelativeAngle(Math.atan2(nextX - x, nextY - y) - getHeadingRadians());
		setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ? angle > 0 ? angle - Math.PI : angle + Math.PI : angle);
		setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * Point2D.distance(nextX, nextY, x, y));
	}
}
