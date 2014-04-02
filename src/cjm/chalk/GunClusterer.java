package cjm.chalk;

import robocode.*;

import java.util.*;

public class GunClusterer {
	
	static final double BAND_WIDTH = 6.0d;
	private int _maxScans;
	private int _topScans;
	private double _nonShotWeight;
	
	Scan[] _scans;
	int _scanIndex;
	
	java.awt.geom.Rectangle2D.Double _walls;
	
	ArrayList<VirtualBullet> _virtualShots = new ArrayList<VirtualBullet>();
	private Averager _averager = new Averager(2500);
	
	public GunClusterer(int maxScans, int topScans, double nonShotWeight, AdvancedRobot robot){
		_maxScans = maxScans;
		_topScans = topScans;
		_nonShotWeight = nonShotWeight;
		_scans = new Scan[_maxScans];
		
		_walls = new java.awt.geom.Rectangle2D.Double(5d, 5d, robot.getBattleFieldWidth() - 10d, robot.getBattleFieldHeight() - 10d);
	}
	
	public void addScan(Scan s){
		_scans[_scanIndex % _maxScans] = s;
	    _scanIndex++;
	}
	
	public void checkVirtualBullets(long time, double x, double y){
		for(int i = _virtualShots.size() - 1; i >= 0; i--){
			VirtualBullet vb = (VirtualBullet)_virtualShots.get(i);
			double d = vb.TheScan.getDistance(time);
			double heading = vb.TheScan.EnemyHeading + vb.TheBearing;
			double nextX = vb.TheScan.RX + Math.sin(heading) * d;
			double nextY = vb.TheScan.RY + Math.cos(heading) * d;
			if(ChalkUtils.distance(x, y, nextX, nextY) <= 20d){
				_averager.addValue(1d);
				_virtualShots.remove(i);
			}
		}
	}
	
	public double getRatingPercent(){
		return _averager.getAverage();
	}
	
	public void takeVirtualShot(Scan s, double bearing){
		_virtualShots.add(new VirtualBullet(s, bearing));
	}
	
	public void removePassed(Scan s){
		for(int i = _virtualShots.size() - 1; i >= 0; i--){
			if(((VirtualBullet)_virtualShots.get(i)).TheScan == s){
				_virtualShots.remove(i);
				_averager.addValue(0d);
				break;
			}
		}
	}
	
	public void clear(){
		_virtualShots.clear();
	}
	
	public double projectBearing(Scan scan, double x, double y, double enemyHeading){
		
		double distance = scan.Distance;
		
		double err = 0;
	    Scan[] bestNodes = new Scan[_topScans];
	    double[] bestErrs = new double[_topScans];
	    Arrays.fill(bestErrs, Double.MAX_VALUE);
		int results = 0;
		
		double d, vc, lv, wt, wb, a;
		
		for(int i = Math.min(_scans.length, _scanIndex); --i >= 0;){
		    
		    Scan s = _scans[i];
		    
		    err = ((d = (scan.NormalizedDistance - s.NormalizedDistance)) * d * 4d) +
		    	((vc = (scan.SinceVelocityChange - s.SinceVelocityChange)) * vc * 4d) +
		    	((lv = (scan.LateralVelocity - s.LateralVelocity)) * lv * 5d) +
		    	((wt = (scan.WallTriesForward - s.WallTriesForward)) * wt * 4d) +
		    	((wb = (scan.WallTriesBack - s.WallTriesBack)) * wb) +
		    	((a = (scan.Acceleration - s.Acceleration)) * a * 2d);
		    
		    int j = _topScans - 1;
		    if(err < bestErrs[j]){
			    while(--j >= 0 && err < bestErrs[j]){
		            bestErrs[j + 1] = bestErrs[j];
		            bestNodes[j + 1] = bestNodes[j];
			    }
			    bestErrs[++j] = err;	
			    bestNodes[j] = s;
		    }
			        
		    results++;
		   
		}
		
		results = Math.min(results, _topScans);
		    
		if(results > 0){
			
			double theBearing;
	
			//remove out-of-bounds scans
			double projectedX, projectedY, projectedDistance;
			int index = 0;
			for(int i = 0; i < results; i++){
				theBearing = bestNodes[i].BearingRadians * scan.MaxAngle;
				projectedDistance = distance + bestNodes[i].DistanceDelta;
				projectedX = x + Math.sin(enemyHeading + theBearing) * projectedDistance;
				projectedY = y + Math.cos(enemyHeading + theBearing) * projectedDistance;
				if(_walls.contains(projectedX, projectedY)){
					if(i != index){
						bestNodes[index] = bestNodes[i];
					}
					index++;
				}
			}
			
			results = index;
			
			if(results > 0){
				//calculate density on observed bearings
				int bestIndex = 0;
				double bestDensity = 0;
				
				for(int i = results; --i >= 0;){
					
					double density = 0;
					double u;
					for(int j = results; --j >= 0;){
						density += Math.exp(
									(
										u = (bestNodes[i].Bearing - bestNodes[j].Bearing) / BAND_WIDTH
									) 
									* u 
									* -0.5d
								)
								* (bestNodes[j].IsRealBullet ? 1d : _nonShotWeight);
					}
					
					density *= (bestNodes[i].IsRealBullet ? 1d : _nonShotWeight);
					
					if(density > bestDensity){
						bestDensity = density;
						bestIndex = i;
					}
				}
				
				return bestNodes[bestIndex].BearingRadians;
			}
		}
		return Double.MAX_VALUE;
	}
	
	class VirtualBullet{
		public Scan TheScan;
		public double TheBearing;
		
		VirtualBullet(Scan s, double bearing){
			TheScan = s;
			TheBearing = bearing;
		}
	}
}
