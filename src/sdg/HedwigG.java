package sdg;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class HedwigG extends AdvancedRobot {

	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		this.setColors(Color.PINK, Color.PINK, Color.PINK);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent sre) {

		setTurnRadarLeftRadians(getRadarTurnRemaining());

		double enemyHeading = getHeadingRadians() + sre.getBearingRadians();
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - getGunHeadingRadians()));

		setFire(2d);
	}
}
