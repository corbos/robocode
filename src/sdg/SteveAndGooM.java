package sdg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class SteveAndGooM extends AdvancedRobot {
	static final double ONE_QUARTER = Math.PI / 2;
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_EIGHTH = Math.PI / 4;

	private double _direction = 1;
	private int _stoppedTicks;
	private RoundRectangle2D.Double _battlefield;

	public void run() {

		_battlefield = new RoundRectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60,
				100, 100);
		this.setColors(Color.BLUE, Color.BLUE, Color.BLUE);

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

		for (int i = 0; i < 2; i++) {
			double a = preferredAngle;
			do {
				nextX = x + Math.sin(enemyHeading + a * _direction) * 65;
				nextY = y + Math.cos(enemyHeading + a * _direction) * 65;
				a -= .01745d;
			} while (!_battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);

			if (_battlefield.contains(nextX, nextY)) {
				break;
			}
			_direction = -_direction;
			_stoppedTicks = -5;
		}

		double angle = Utils.normalRelativeAngle(Math.atan2(nextX - x, nextY - y) - getHeadingRadians());
		setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ? angle > 0 ? angle - Math.PI : angle + Math.PI : angle);
		if (Math.abs(getVelocity()) < 0.01)
			_stoppedTicks++;

		if (_stoppedTicks > 6) {
			setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * Point2D.distance(nextX, nextY, x, y));
			_stoppedTicks = 0;
		}
	}
}
