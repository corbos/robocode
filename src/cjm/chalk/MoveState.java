package cjm.chalk;

import robocode.util.*;
import java.awt.geom.*;

public class MoveState {
	
	static final double ONE_QUARTER = Math.PI / 2d;
	public static Rectangle2D.Double BOUNDS;

	public double RobotX;
	public double RobotY;
	public double Heading;
	public double Velocity;
	public boolean HitWall = false;
	
	public MoveState() {}
	
	static MoveState getNextState(MoveState state, double desiredVelocity, double desiredHeading){
		
		MoveState next = new MoveState();
		
		boolean backingUp = false;
		double workingHeading = state.Heading;
		if(state.Velocity < 0){
			backingUp = true;
			workingHeading = Utils.normalAbsoluteAngle(state.Heading + Math.PI);
		}
		
		double bearing = Utils.normalRelativeAngle(desiredHeading - workingHeading);
		
		if(Math.abs(bearing) > ONE_QUARTER){
			backingUp = !backingUp;
			workingHeading = Utils.normalAbsoluteAngle(workingHeading + Math.PI);
			bearing = Utils.normalRelativeAngle(desiredHeading - workingHeading);
		
		}
		
		double maxHeadingDelta = Math.toRadians(10d - 0.75d * Math.abs(state.Velocity));
		double headingDelta = Math.max(Math.min(bearing, maxHeadingDelta), -maxHeadingDelta);	
		next.Heading = state.Heading + headingDelta;
	
		if(backingUp){
		    if(state.Velocity < -desiredVelocity){
		        next.Velocity = Math.min(-desiredVelocity, state.Velocity + 2d);
		    }
		    else{
		    	next.Velocity  = Math.max(state.Velocity - (state.Velocity <= 0 ? 1d : 2d), -desiredVelocity);
		    }
		}
		else{
		    if(state.Velocity > desiredVelocity){
		    	next.Velocity  = Math.max(desiredVelocity, state.Velocity - 2d);
		    }
		    else{
		    	next.Velocity  = Math.min(state.Velocity + (state.Velocity >= 0 ? 1d : 2d), desiredVelocity);
		    }
		}
		
		next.RobotX = state.RobotX + Math.sin(next.Heading) * next.Velocity;
		next.RobotY = state.RobotY + Math.cos(next.Heading) * next.Velocity;
		
		if(!BOUNDS.contains(next.RobotX, next.RobotY)){
			next.Velocity = 0;
			next.RobotX = Math.max(BOUNDS.x, Math.min(BOUNDS.width + BOUNDS.x, next.RobotX));
			next.RobotY = Math.max(BOUNDS.y, Math.min(BOUNDS.height + BOUNDS.y, next.RobotY));
			next.HitWall = true;
		}
		
		return next;
	}
}
