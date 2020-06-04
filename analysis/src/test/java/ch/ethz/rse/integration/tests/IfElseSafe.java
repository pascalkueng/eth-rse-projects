package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class IfElseSafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(50);
		TrainStation s1 = new TrainStation(50);
		TrainStation s2;
		
		if(j == 15) {
			s2 = s;
		} else {
			s2 = s1;
		}
		
		if ( 0 < j && j < 15) {
			s.arrive(j);
			s2.arrive(j);
		}
	}
}
