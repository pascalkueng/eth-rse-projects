package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class A__BinOpSafe {

	public static void m1() {
		int x = 10;
		int y = 10;
		TrainStation s = new TrainStation(20);
		
		x = x - 20; // -10
		x = x * 3; // -30
		x = x + 32; // 2
		
		s.arrive(x);
		
		y = y - 5; // 5
		y = y * 3; // 15
		y = y+2; // 17 
		
		s.arrive(y);
		
	}
}
