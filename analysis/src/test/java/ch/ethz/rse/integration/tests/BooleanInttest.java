package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class BooleanInttest {

	public static void m2(int i, int j) {
		
		TrainStation s = new TrainStation(10);
		
		int a = 1; 
		int b = 0;
		
		if(i == j) {
			a = 0;
		} else {
			b =1;
		}
		
		if ( j == 1) {
			a =j;
		}
		
		if ( a == b) {
			s.arrive(-1);
			s.arrive(10);
		} else {
			s.arrive(2);
			s.arrive(3);
		}
		
		
	}
}
