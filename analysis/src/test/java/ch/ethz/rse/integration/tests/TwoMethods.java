package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class TwoMethods {

	public static void m1(int j) {
		TrainStation s = new TrainStation(20);
		TrainStation s1 = new TrainStation(20);
		TrainStation s2; 
		if(j==10) {
			s2 = s;
		}else {
			s2 = s1;
		}
		
		if(0 < j && j < 10) {
			s.arrive(j);
			s2.arrive(j);
		}
		
	}
	
	public static void m2(int j) {
		TrainStation s = new TrainStation(20);
		TrainStation s1 = new TrainStation(20);
		TrainStation s2;
		if(j == 10) {
			s2 = s;
		} else {
			s2 = s1;
		}
		
		if(0 < j && j < 10) {
			s.arrive(j);
			s2.arrive(j);
		}
	}
}
