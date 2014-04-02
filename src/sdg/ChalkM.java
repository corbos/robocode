package sdg;

import java.awt.Color;

import cjm.chalk.MoveState;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class ChalkM extends AdvancedRobot {

	cjm.chalk.Evader _evader;
	int _ticksFromLastScan = 1;

	public void run() {

		_evader = new cjm.chalk.Evader(this);

		setColors(Color.WHITE, Color.WHITE, Color.WHITE);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		if (MoveState.BOUNDS == null) {
			MoveState.BOUNDS = new java.awt.geom.Rectangle2D.Double(18d, 18d, getBattleFieldWidth() - 36d,
					getBattleFieldHeight() - 36d);
		}

		while (true) {
			if (_ticksFromLastScan++ > 1) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			_evader.move();
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent sre) {

		_ticksFromLastScan = 0;
		// radar
		setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians() + sre.getBearingRadians()
				- getRadarHeadingRadians()) * 2);

		_evader.onScannedRobot(sre);

	}

	public void onHitByBullet(HitByBulletEvent e) {
		_evader.onHitByBullet(e);
	}

	public void onBulletHit(BulletHitEvent e) {
		_evader.onBulletHit(e);
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		_evader.onBulletHitBullet(e);
	}

	public void onHitRobot(HitRobotEvent e) {
		_evader.onHitRobot(e);
	}
}
