package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Def_Tests {
	public static void m1(int j) {
		TrainStation s = new TrainStation(20);
		TrainStation s1 = new TrainStation(20);
		TrainStation s2;

		if (j==0) {
			s2 = s;
		} else if (j==1) {
			s2 = s1;
		} else {
			return;
		}
		s.arrive(1);
		s2.arrive(1);
	}

}
