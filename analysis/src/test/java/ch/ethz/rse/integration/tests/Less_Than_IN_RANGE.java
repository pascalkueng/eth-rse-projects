package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Less_Than_IN_RANGE {
	public void m2(int j) {
		TrainStation y = new TrainStation(10);
		if (0 <= j && j < 12) {
			// -1<=j<=10
			y.arrive(j);
		}
	}
}
