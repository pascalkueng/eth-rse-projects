package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Loop2test {

	public static void m1(int j) {

	      TrainStation s = new TrainStation(600);

	      for (int i = 0; i < 500; i++) {

	               s.arrive(i);

	      }

	     s.arrive(518);

	}

}
