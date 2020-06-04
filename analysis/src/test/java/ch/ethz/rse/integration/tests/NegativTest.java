package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE


public class NegativTest {
	public static void m1(int i) {
		TrainStation s = new TrainStation(0);
		s.arrive(3);
	}
}
