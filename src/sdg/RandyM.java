package sdg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class RandyM extends AdvancedRobot {

	private double _radarDegrees = 540;
	private double _direction = 1;
	private double _x;
	private double _y;
	private RoundRectangle2D.Double _battlefield;
	private double _CTLTHits;
	private boolean _moveMatched;
	// enemy info================
	double _lastHeading = Double.MIN_VALUE;
	private double _lastEnemyDistance;
	private double _lastEnemyEnergy = 100;
	private double _lastEnemyShotPower;
	// constants=======================
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_QUARTER = Math.PI / 2;
	static final double ONE_EIGHTH = Math.PI / 4;
	static final double TEN_DEGREES = 10 * Math.PI / 180;
	static final double TWENTY_DEGREES = TEN_DEGREES * 2;

	public void run() {

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setColors(Color.GREEN, Color.GREEN, Color.GREEN);

		setTurnRadarRight(_radarDegrees);

		_battlefield = new RoundRectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60,
				100, 100);
	}

	public void onScannedRobot(ScannedRobotEvent sre) {
		_x = getX();
		_y = getY();
		_lastEnemyDistance = sre.getDistance();
		_lastEnemyShotPower = _lastEnemyEnergy - (_lastEnemyEnergy = sre.getEnergy());

		setTurnRadarRight(_radarDegrees = -_radarDegrees);
		move(getHeadingRadians() + sre.getBearingRadians());
	}

	public void onHitByBullet(HitByBulletEvent e) {
		if (_lastEnemyDistance > 150 && !_moveMatched) {
			_CTLTHits++;
		}
		_moveMatched = _CTLTHits > (getRoundNum() + 1);
	}

	private void move(double enemyHeading) {

		double nextX = 0, nextY = 0;
		double preferredAngle = ONE_QUARTER
				+ (ONE_QUARTER / (1 + Math.exp((_lastEnemyDistance - 350) / 100)) - ONE_EIGHTH);

		if (_moveMatched) {
			if (Math.random() < 0.08348d) {
				_direction = -_direction;
			}
		} else {
			if (getDistanceRemaining() > 0 || _lastEnemyShotPower < 0.1 || _lastEnemyShotPower > 3.0) {
				return;
			}
		}

		// Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		for (int i = 0; i < 2; i++) {
			double a = preferredAngle;
			do {
				nextX = _x + Math.sin(enemyHeading + a * _direction) * 85;
				nextY = _y + Math.cos(enemyHeading + a * _direction) * 85;
				a -= .01745d;
			} while (!_battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);

			if (_battlefield.contains(nextX, nextY)) {
				break;
			}
			_direction = -_direction;
		}

		double angle = Utils.normalRelativeAngle(Math.atan2(nextX - _x, nextY - _y) - getHeadingRadians());
		setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ? angle > 0 ? angle - Math.PI : angle + Math.PI : angle);
		setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * Point2D.distance(nextX, nextY, _x, _y));
	}
}
