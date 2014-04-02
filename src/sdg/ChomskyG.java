package sdg;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Vector;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import cjm.Util;
import cjm.ANN.SmallNet;
import cjm.chalk.ChalkUtils;

public class ChomskyG extends AdvancedRobot {

	private double _radarDegrees = 540;
	private double lastEnemyBearing = Double.MIN_VALUE;
	private static SmallNet _net;
	private double _shotPower = 2d;
	private double[] _input = new double[6];
	private double[] _output = new double[1];
	private Vector<VirtualBullet> _bulletQueue = new Vector<VirtualBullet>();
	private double _enemyX;
	private double _enemyY;
	private double _lastVelocity = Double.MAX_VALUE;
	private double _lastOutput;

	public void run() {

		setColors(Color.ORANGE, Color.WHITE, Color.YELLOW);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		if (_net == null) {
			_net = new SmallNet(5, 5, 1, 0.05d);
		}

		setTurnRadarRight(_radarDegrees);

		while (true) {
			fire();
			execute();
		}
	}

	private void aim(ScannedRobotEvent sre) {
		lastEnemyBearing = Util.getUnitCircleAngle(getHeadingRadians() + sre.getBearingRadians());
		long time = getTime();
		double x = getX();
		double y = getY();
		double distance = sre.getDistance();
		_enemyX = x + distance * Math.cos(lastEnemyBearing);
		_enemyY = y + distance * Math.sin(lastEnemyBearing);
		double enemyHeading = Util.getHeadingPoints(x, y, _enemyX, _enemyY);
		double velocity = sre.getVelocity();

		double acceleration = 0;
		if (_lastVelocity != Double.MAX_VALUE) {

			if (ChalkUtils.sign(_lastVelocity) == ChalkUtils.sign(velocity)) {
				acceleration = Math.abs(velocity) - Math.abs(_lastVelocity);
			} else {
				acceleration = Math.abs(velocity - _lastVelocity);
			}
		} else {
			acceleration = velocity;
		}
		acceleration = Math.max(Math.min(acceleration, 2d), -2d);
		_lastVelocity = velocity;

		double relativeHeading = sre.getHeadingRadians() - (getHeadingRadians() + sre.getBearingRadians());
		double lateralVelocity = velocity * Math.sin(relativeHeading);
		double advancingVelocity = -Math.cos(relativeHeading) * velocity;

		// check virtual bullets============================
		double enemyDistance, bulletDistance;
		VirtualBullet vb;
		for (int i = _bulletQueue.size() - 1; i > -1; i--) {
			vb = _bulletQueue.get(i);
			enemyDistance = vb.getEnemyDistance(_enemyX, _enemyY);
			bulletDistance = vb.getBulletDistance(time);
			if (bulletDistance > enemyDistance + 20) {
				_bulletQueue.removeElementAt(i);
			} else if (Math.abs(enemyDistance - bulletDistance) < 18) {
				// train========================================
				_input[0] = vb.Distance / 800d;
				_input[1] = vb.LateralVelocity / 8d;
				_input[2] = vb.AdvancingVelocity / 8d;
				_input[3] = vb.Acceleration / 2d;
				_input[4] = _lastOutput;
				_input[5] = vb.getBearing(_enemyX, _enemyY);
				_net.train(_input);
			}
		}

		VirtualBullet bb = new VirtualBullet(enemyHeading, distance, x, y, time);

		bb.LateralVelocity = lateralVelocity;
		bb.AdvancingVelocity = advancingVelocity;

		bb.setBulletPower(_shotPower);
		_bulletQueue.add(bb);

		_input[0] = bb.Distance / 800d;
		_input[1] = bb.LateralVelocity / 8d;
		_input[2] = bb.AdvancingVelocity / 8d;
		_input[3] = bb.Acceleration / 2d;
		_input[4] = _lastOutput;

		_output = _net.getNext(_input);
		_lastOutput = _output[0];

		double angle = Util.getRelativeBearing(getGunHeadingRadians(), x, y, _enemyX, _enemyY) + _output[0];

		if (Math.abs(angle) > Math.PI) {
			angle += (angle < 0 ? Util.FOUR_QUARTERS : -Util.FOUR_QUARTERS);
		}

		setTurnGunRightRadians(angle);

	}

	private void fire() {
		if (getOthers() > 0) {
			setFire(_shotPower);
		}
	}

	public void onScannedRobot(ScannedRobotEvent sre) {
		setTurnRadarRight(_radarDegrees = -_radarDegrees);
		aim(sre);
	}

	public void onWin(WinEvent e) {
		_bulletQueue.clear();
	}

	class VirtualBullet {

		public double RobotX;
		public double RobotY;
		public double Distance;
		public double EnemyHeading;
		public double BulletPower;
		public long StartingTick;
		public double BulletVelocity;
		public double LateralVelocity;
		public double AdvancingVelocity;
		public double Acceleration;

		public VirtualBullet(double h, double d, double rX, double rY, long t) {
			RobotX = rX;
			RobotY = rY;
			Distance = d;
			StartingTick = t;
			EnemyHeading = h;
		}

		public void setBulletPower(double bulletPower) {
			BulletPower = bulletPower;
			BulletVelocity = 20 - bulletPower * 3;
		}

		public double getEnemyDistance(double eX, double eY) {
			return Point2D.distance(eX, eY, RobotX, RobotY);
		}

		public double getBulletDistance(long t) {
			return (double) (t - StartingTick) * BulletVelocity;
		}

		public double getBearing(double eX, double eY) {
			return Util.getRelativeBearing(EnemyHeading, RobotX, RobotY, eX, eY);
		}
	}

}
