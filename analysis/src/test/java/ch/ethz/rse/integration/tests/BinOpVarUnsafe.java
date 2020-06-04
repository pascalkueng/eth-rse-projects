package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class BinOpVarUnsafe {
	public static void m1(int j, int k, int l) {
		
		TrainStation s = new TrainStation(23);
		int x = j + k;
		x = x-2;
		x = x * 2;
		if(x > 0 && l > 0 ) {
			s.arrive(x);
			s.arrive(l);
		}
	}
}
