package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class If_Tests {
	public static void m1(int a) {

		if (0 > a) {
		}
		if (0 <= a) {
		}
		TrainStation s = new TrainStation(10);
	}
}
