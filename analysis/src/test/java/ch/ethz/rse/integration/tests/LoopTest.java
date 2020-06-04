package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class LoopTest {
	public static void m1(int j, int k) {
		
		int tmp = 100;
		TrainStation s = new TrainStation(100);
		
		for (int i = 0; i < j; i++) {
			if(tmp - k >= 0 && k >= 0) {
				tmp = tmp - k;
				s.arrive(tmp);
				s.arrive(k);
			}
		}
	}
}
