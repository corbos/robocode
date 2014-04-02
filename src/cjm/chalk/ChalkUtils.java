package cjm.chalk;

public class ChalkUtils {

    static void setWeight(double[] bounds, double value, double weight){
	    bounds[0] = Math.min( bounds[0], value);
	    bounds[1] = Math.max( bounds[1], value);
	    bounds[2] = (bounds[1] - bounds[0]) / weight;
	    if(bounds[2] == 0){
	        bounds[2] = 1;
	    }
	}
    
    static void setWeight(double[] bounds, double value){
	    bounds[0] = Math.min( bounds[0], value);
	    bounds[1] = Math.max( bounds[1], value);
	    bounds[2] = bounds[1] - bounds[0];
	    if(bounds[2] == 0){
	        bounds[2] = 1;
	    }
	}
    
    public static final double sign(double x){
	    if(x < 0){
	        return -1;
	    }
	    return 1;
	}
    
    static final double getDamage(double power){
    	return (4d * power) + (power > 1d ?  2d * (power - 1d) : 0d);
    }
    
    static final double sqr(double x){
    	return x * x;
    }
    
    static final double distance(double x, double y, double xx, double yy){
    	return Math.sqrt(sqr(x - xx) + sqr(y - yy));
    }
}
