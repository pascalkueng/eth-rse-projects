package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class NonLin {

	public static void m1(int j){

	    TrainStation s = new TrainStation(4);

	    int i = j;

	    if(i*i < 10){

	        s.arrive(i);

	    }

	}
}
