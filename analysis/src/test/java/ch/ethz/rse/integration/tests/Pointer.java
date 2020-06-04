package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;


//expected results:
//TRACK_NON_NEGATIVE SAFE
//TRACK_IN_RANGE SAFE
//NO_CRASH SAFE

public class Pointer {

	public static void pointerM5(int i) {
        TrainStation s = new TrainStation(5);

        for (int j = 0; j < 10; j++) {
            s = new TrainStation(6);
            s.arrive(0);
        }

    }
}
