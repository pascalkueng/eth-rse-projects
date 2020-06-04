package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class BreakTest {
	public static void m1(int j) {
		
		int tmp = 99;
		TrainStation s = new TrainStation(100);
		
		while(j > 0) {
			if (tmp - j < 0) {
				break;
			}
			
			s.arrive(j);
			tmp = tmp - j;
		}		
	}
}
