package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class BinOpVarSafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int x = j * 2; // 2j
		int y = x + 12; // 2j+12
		int res = y - 2*j-6;  //6
		
		s.arrive(res);
	}
}
