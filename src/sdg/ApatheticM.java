package sdg;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class ApatheticM extends AdvancedRobot {

	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		this.setColors(Color.YELLOW, Color.YELLOW, Color.YELLOW);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent sre) {

		setTurnRadarLeftRadians(getRadarTurnRemaining());

		double enemyHeading = getHeadingRadians() + sre.getBearingRadians();
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - getGunHeadingRadians()));
	}
}
