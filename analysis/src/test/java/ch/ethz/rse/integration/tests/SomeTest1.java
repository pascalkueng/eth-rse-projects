package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE (imprecise)

public class SomeTest1 {

	public static void example(int i) {
        TrainStation s = new TrainStation(10);
        if (0 <= i && i <= 6) {
                if (i != 3) {
                        s.arrive(i);
                        s.arrive(3);
                }
        }
}
}
