package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class BreakForLoop {
	public static void m1(int j) {
		
		TrainStation s = new TrainStation(100);
		
		for(int i = 0; i < 600; i++) {
			if(i >= 100) {
				break;
			}
			s.arrive(i);
		}
	}
}
