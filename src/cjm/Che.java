package cjm;

import robocode.*;
import robocode.util.*;
import java.awt.Color;
import java.awt.geom.*;

/*
 * Che - by Corbos (corbin@scatterbright.com)
 * 		my first mini
 * 
 * Thanks to everyone at robowiki.net. 
 * 
 * 1.0	Movement inspired by Tityus/RaikoMicro-type random oscillation
 * 		Gun == indexed pattern matcher
 * 1.1	Fixed a rammer bug
 * 1.2	Added a 'best of' selection to matched patterns. Too slow.
 */

public class Che extends AdvancedRobot {
	
	static double _radarDegrees = 540;
	static double _shotPower;
	static double _direction = 1;
	static double _x;
	static double _y;
	static RoundRectangle2D.Double _battlefield;
	static double _CTLTHits;
	static boolean _moveMatched;
	//enemy info================
	double _lastHeading = Double.MIN_VALUE;
	static double _lastVelocity;
	static long _lastScan;
	static Point2D.Double _enemyPosition;
	double _lastEnemyDistance;
	double _lastEnemyEnergy = 100;
	double _lastEnemyShotPower;
	//constants=======================
	static final double FOUR_QUARTERS = Math.PI * 2;
	static final double ONE_QUARTER = Math.PI/2;
	static final double ONE_EIGHTH = Math.PI/4;
	
	//pattern matcher===================
	static private PatternNode[] _nodeIndexes = new PatternNode[512];
	static private PatternNode _head;
	static private int _scanCount = 0;
	static final double TEN_DEGREES = 10 * Math.PI / 180;
	static final double TWENTY_DEGREES = TEN_DEGREES * 2;
	static final char BREAK_SCAN = '\uffff';
	static final int DEPTH = 20;
	static final int MAX_DEPTH = 75;
	
	public void run(){
		
		setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.ORANGE, Color.BLACK, Color.ORANGE);
		
        insert(new PatternNode(BREAK_SCAN, _scanCount++, 0, 0));
		
		setTurnRadarRight(_radarDegrees);
		
		_battlefield = new RoundRectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60, 100, 100);
		
		while(true) {
			if(_shotPower > 0 && getOthers() > 0){
				setFire(_shotPower);
			}
			execute();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent sre){
		
		double heading = sre.getHeadingRadians();
		double velocity = sre.getVelocity();
		_x = getX();
		_y = getY();
		_lastEnemyDistance = sre.getDistance();
		_lastEnemyShotPower =_lastEnemyEnergy - (_lastEnemyEnergy = sre.getEnergy());
		double enemyHeading = getHeadingRadians() + sre.getBearingRadians();
		_enemyPosition = new Point2D.Double(Math.sin(enemyHeading) * _lastEnemyDistance + _x, Math.cos(enemyHeading) * _lastEnemyDistance + _y);
		
		if(_lastHeading != Double.MIN_VALUE){
			
			double ticks = (double)(getTime() - _lastScan);
			double headingDelta = Utils.normalRelativeAngle(heading - _lastHeading) / ticks;
			double velocityDelta = (velocity - _lastVelocity) / ticks;
			
			for(double i = 1; i < ticks + 1; i++){
				record(_lastVelocity + velocityDelta * i, headingDelta);
			}
		}
		
		//==============
		_lastScan = getTime();
		_lastHeading = heading;
		_lastVelocity = velocity;
		
		_shotPower = 1;
		if(_lastEnemyDistance < 125){
			_shotPower = 3;
		}
		else if(_lastEnemyDistance < 400){
			_shotPower = 2;
		}
	
		_shotPower = Math.min(Math.min(sre.getEnergy() + 0.1 / 4, _shotPower), getEnergy() - 0.1);
		
		//_shotPower = 0.5;
		
		if(_lastEnemyDistance > 100){
			Point2D.Double nextLocation = project(sre.getHeadingRadians(), 20 - 3 * _shotPower);
			if(nextLocation != null){
				_enemyPosition = nextLocation;
			}
			else{
				_shotPower = 0;
			}
		}
		setTurnGunRightRadians(Utils.normalRelativeAngle(Math.atan2(_enemyPosition.x - _x, _enemyPosition.y - _y) - getGunHeadingRadians()));
		
		setTurnRadarRight(_radarDegrees = -_radarDegrees);
		move(enemyHeading);
	}
	
	 public void onHitByBullet(HitByBulletEvent e){
	 	//simplified Tityus/Musashi mode switch
	 	if(_lastEnemyDistance > 150 && !_moveMatched){
	 		_CTLTHits++;
	 	}
	 	_moveMatched = _CTLTHits > (getRoundNum() + 1);
	 }
	
	private void move(double enemyHeading){
			
		double nextX = 0, nextY = 0;
		double preferredAngle = ONE_QUARTER + (ONE_QUARTER / (1 + Math.exp((_lastEnemyDistance - 350)/100)) - ONE_EIGHTH);
		
		if(_moveMatched){
			if(Math.random() < 0.08348d){
				_direction = -_direction;
			}
		}
		else{
			if(getDistanceRemaining() > 0 || _lastEnemyShotPower < 0.1 || _lastEnemyShotPower > 3.0){
				return;
			}
		}
		
		//Ideas from http://www.robowiki.net/cgi-bin/robowiki?WallSmoothing
		for(int i = 0; i < 2; i++){
			double a = preferredAngle;
			do{
				nextX = _x + Math.sin(enemyHeading + a * _direction) * 85;
				nextY = _y + Math.cos(enemyHeading + a * _direction) * 85;
				a -= .01745d;
			}while(!_battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);
			
			if(_battlefield.contains(nextX, nextY)){
				break;
			}
			_direction = -_direction;
		}
		
		double angle = Utils.normalRelativeAngle(Math.atan2(nextX - _x, nextY - _y) - getHeadingRadians());
		setTurnRightRadians(Math.abs(angle) > ONE_QUARTER ?  angle > 0 ? angle - Math.PI : angle + Math.PI: angle);
		setAhead((Math.abs(angle) > ONE_QUARTER ? -1 : 1) * Point2D.distance(nextX, nextY, _x, _y));
	}
	
	/*=================================================================================
	 * Pattern matcher - 512 character alphabet
	 * Basically a doubly-linked sequence with linked indexes, pretty fast.
	 * Lots of work to do.
	 =================================================================================*/
	public void record(double velocity, double headingDelta){
		int index = (((int)((velocity + 8) * 15 / 16) << 5) | ((int)((headingDelta + TEN_DEGREES) * 31 / TWENTY_DEGREES)));
		PatternNode node = new PatternNode((char)index, _scanCount++, velocity, headingDelta);
		insert(node);
		node.IndexRef = _nodeIndexes[index];
		_nodeIndexes[index] = node;
	}
	
	private void insert(PatternNode node){
		node.SequenceRef = _head;
		_head = node;
		if(node.SequenceRef != null){
			node.SequenceRef.ReverseSequenceRef = node;
		}
	}
	
	public Point2D.Double project(double currentHeading, double bulletVelocity){
		
		PatternNode cursor;
		PatternNode currentSymbol = _head;
		PatternNode t, p;
		PatternNode indexSymbol = _head.IndexRef;
		PatternNode[] bestNodes = new PatternNode[DEPTH];
		int depth;
		
		while(indexSymbol != null && currentSymbol.Index - indexSymbol.Index < 50000){
				
			cursor = indexSymbol;
			depth = 0;
			
			while(currentSymbol.Index - indexSymbol.Index > 25 && currentSymbol != null && cursor != null && currentSymbol.Symbol == cursor.Symbol && depth < MAX_DEPTH){
				currentSymbol = currentSymbol.SequenceRef;
				cursor = cursor.SequenceRef;
				depth++;
			}
			
			//crappy bubble sort deal that needs to go.
			//expedient, i guess
			if(depth > 0){
				p = indexSymbol;
				p.Depth = depth;
				for(int i = 0; i < DEPTH; i++){
					if(bestNodes[i] == null){
						bestNodes[i] = p;
						break;
					}
					if(p.Depth > bestNodes[i].Depth){
						t = bestNodes[i];
						bestNodes[i] = p;
						p = t;
					}
				}
			}
			
			currentSymbol = _head;
			indexSymbol = indexSymbol.IndexRef;
		}
		
		for(int i = 0; i < DEPTH && bestNodes[i] != null; i++){
			
			double projectedTime = 0;
			double enemyX = _enemyPosition.x;
			double enemyY = _enemyPosition.y;
			double ch = currentHeading;
			
			bestNodes[i] = bestNodes[i].ReverseSequenceRef;
			while(bestNodes[i] != null && Point2D.distance(_x, _y, enemyX, enemyY) > projectedTime * bulletVelocity && bestNodes[i].Symbol != BREAK_SCAN){
				ch += bestNodes[i].HeadingDelta;
				enemyX += Math.sin(ch) * bestNodes[i].Velocity;
				enemyY += Math.cos(ch) * bestNodes[i].Velocity;
				projectedTime++;
				bestNodes[i] = bestNodes[i].ReverseSequenceRef;
			}
			if(bestNodes[i] != null && bestNodes[i].Symbol != BREAK_SCAN){
				return new Point2D.Double(enemyX, enemyY);
			}
		}
		return null;
	}
	
	class PatternNode{
		
		public PatternNode IndexRef;
		public PatternNode SequenceRef;
		public PatternNode ReverseSequenceRef;
		public char Symbol;
		public double Velocity;
		public double HeadingDelta;
		public int Index;
		public int Depth;
		
		PatternNode(char s, int index, double v, double hd){
			Symbol = s;
			Index = index;
			Velocity = v;
			HeadingDelta = hd;
		}
	}
}
