package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class InfinityLoopBreak {

	public static void m1(int j) {
		
		int tmp = 99;
		TrainStation s = new TrainStation(100);
		
		while(true) {
			if(tmp - j < 0) {
				break;
			}
			s.arrive(tmp);
			tmp = tmp - j;
		}
	}
	
}
