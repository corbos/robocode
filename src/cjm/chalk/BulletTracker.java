package cjm.chalk;

import robocode.util.Utils;

public class BulletTracker {

    static final double BAND_WIDTH = 6.0d;
    static final int BUFFER_COUNT = 5000;
    static final int VISIT_COUNT = 2200;
    static final int BEST_VISITS = 50;
    static EnemyScan[] _hits = new EnemyScan[BUFFER_COUNT];
	static int _hitIndex = 0;
	static EnemyScan[] _visits = new EnemyScan[VISIT_COUNT];
	static int _visitIndex = 0;
	
	static double[] _bD = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 4d};
	static double[] _bLV = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 5d};
	static double[] _bVC = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 3d};
	static double[] _bA = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 3d};
	static double[] _bWF = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 4d};
	static double[] _bWB = {Double.MAX_VALUE, Double.MIN_VALUE, 0, 1d};
	
	private EnemyScan[] _visitBearings;
	private boolean _visitCalculated;
	private EnemyScan[] _hitAll;
	private boolean _hitCalculated;
	private int _thisHitIndex;
	public EnemyScan _scan;
	
	BulletTracker(
	        double d, 
	        double v, 
	        double lv, 
	        double av, 
	        double vc, 
	        double a, 
	        double wf, 
	        double wb, 
	        long t, 
	        double dr,
	        double rh,
	        double ex,
	        double ey
	){
	    _visitBearings = new EnemyScan[BEST_VISITS];
	    _thisHitIndex = Math.min(Math.max(7, (int)((double)_hitIndex / 5d)), 50);
	    _hitAll = new EnemyScan[_thisHitIndex]; 
	    
	    ChalkUtils.setWeight(_bD, d);
	    ChalkUtils.setWeight(_bLV, lv);
	    ChalkUtils.setWeight(_bVC, vc);
	    ChalkUtils.setWeight(_bA, a);
	    ChalkUtils.setWeight(_bWF, wf);
	    ChalkUtils.setWeight(_bWB, wb);
	    
	    _scan = new EnemyScan();
	    _scan.Time = t;
	    _scan.Distance = d;
	    _scan.Velocity = v;
	    _scan.LateralVelocity = lv;
	    _scan.AdvancingVelocity = av;
	    _scan.VelocityChange = vc;
	    _scan.Acceleration = a;
	    _scan.WallForward = wf;
	    _scan.WallBackward = wb;
	    _scan.Direction = dr;
	    _scan.RelativeHeading = rh;
	    _scan.EX = ex;
	    _scan.EY = ey;
	}
	
	void calc(){
	    if(!_visitCalculated){
	        
	        double[] errs = new double[BEST_VISITS];
		    java.util.Arrays.fill(errs, Double.MAX_VALUE);
		    
		    double d, a, lv, vc, wf, wb;
		    
		    for(int i = Math.min(_visitIndex, VISIT_COUNT); --i >= 0;){
		        
		        double err = ((d = (_visits[i].Distance - _scan.Distance) / _bD[2]) * d * _bD[3]) +
		        	((a = (_visits[i].Acceleration - _scan.Acceleration) / _bA[2]) * a * _bA[3]) +
					((lv = (_visits[i].LateralVelocity - _scan.LateralVelocity) / _bLV[2]) * lv * _bLV[3]) +
					((vc = (_visits[i].VelocityChange - _scan.VelocityChange) / _bVC[2]) * vc * _bVC[3]) +
					((wf = (_visits[i].WallForward - _scan.WallForward) / _bWF[2]) * wf * _bWF[3]) +
					((wb = (_visits[i].WallBackward - _scan.WallBackward) / _bWB[2]) * wb * _bWB[3]);
		        
		        int j = BEST_VISITS - 1;
		        if(err < errs[j]){
				    while(--j >= 0 && err < errs[j]){
			            errs[j + 1] = errs[j];
			            _visitBearings[j + 1] = _visitBearings[j];
				    }
				    j++;
				    errs[j] = err;
				    _visitBearings[j] = _visits[i];
		        }
		    }
	        _visitCalculated = true;
	    }
	    calcHits(false);
	}
	
	void calcHits(boolean forceRecalc){
	    if(!_hitCalculated || forceRecalc){
	        
		    double[] errAll = new double[_thisHitIndex];
		    java.util.Arrays.fill(errAll, Double.MAX_VALUE);
		    
		    double d, a, lv, vc, wf, wb;
		    
		    for(int i = Math.min(_hitIndex, BUFFER_COUNT); --i >= 0;){
		        
		        double err = ((d = (_hits[i].Distance - _scan.Distance) / _bD[2]) * d * _bD[3]) +
	        	((a = (_hits[i].Acceleration - _scan.Acceleration) / _bA[2]) * a * _bA[3]) +
				((lv = (_hits[i].LateralVelocity - _scan.LateralVelocity) / _bLV[2]) * lv * _bLV[3]) +
				((vc = (_hits[i].VelocityChange - _scan.VelocityChange) / _bVC[2]) * vc * _bVC[3]) +
				((wf = (_hits[i].WallForward - _scan.WallForward) / _bWF[2]) * wf * _bWF[3]) +
				((wb = (_hits[i].WallBackward - _scan.WallBackward) / _bWB[2]) * wb * _bWB[3]);
		        
		        int j = _thisHitIndex - 1;
		        if(err < errAll[j]){
				    while(--j >= 0 && err < errAll[j]){
				        errAll[j + 1] = errAll[j];
			            _hitAll[j + 1] = _hitAll[j];
				    }
				    j++;
				    errAll[j] = err;
				    _hitAll[j] = _hits[i];
		        }
		    }
		    
	        _hitCalculated = true;
	    }
	}
	
	static void addHit(EnemyScan scan, double x, double y){
		
	    scan.setBearing(x, y);
	    
	    _hits[_hitIndex % BUFFER_COUNT] = scan;
		_hitIndex++;
	}
	
	static void addVisit(EnemyScan scan, double x, double y){
		scan.setVisitBearing(x, y);
	    _visits[_visitIndex % VISIT_COUNT] = scan;
	    _visitIndex++;
	}
	
	double getPressure(double x, double y, long time, boolean flatten){
	    
	   double currentBearing = Utils.normalRelativeAngle(Math.atan2(x - _scan.EX, y - _scan.EY) - _scan.RelativeHeading) / _scan.MaxAngle * 100d;
	   double pressure = 0;
	    
	    for(int i = 0; i < _hitAll.length && _hitAll[i] != null; i++){
	        double u = (currentBearing - _hitAll[i].Bearing) / BAND_WIDTH;
			pressure += Math.exp(u * u * -0.5d);
	    }
	    
	    if(flatten){
	        double visitPressure = 0;
		    for(int i = 0; i < _visitBearings.length && _visitBearings[i] != null; i++){
		        double u = (currentBearing - _visitBearings[i].VisitBearing) / BAND_WIDTH;
		        visitPressure += (Math.exp(u * u * -0.5d));
		    }
		    pressure += ((visitPressure * (double)_thisHitIndex / (double)BEST_VISITS));
	    }
	    
	    return pressure;
	}
	
	double distanceTraveled(long t){
		return ((double)(t - _scan.Time) * _scan.BulletVelocity);
	}
	
	void setBulletVelocity(double shotPower){
	    _scan.ShotPower = shotPower;
	    _scan.BulletVelocity = 20d - 3d * shotPower;
	    _scan.MaxAngle = Math.asin(8d / _scan.BulletVelocity) * _scan.Direction;
	}
}
