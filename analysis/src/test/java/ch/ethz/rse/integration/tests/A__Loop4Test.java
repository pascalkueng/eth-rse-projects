package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;


// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class A__Loop4Test {

	public static void m1() {
		  TrainStation s = new TrainStation(10);

		  int x = 0;
		  do {
		    s.arrive(x);
		    x++;
		  } while (x < 10);
		}
}
