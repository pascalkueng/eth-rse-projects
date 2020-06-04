package ch.ethz.rse.integration.tests;
import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE     

public class Condition1 {
    public void m3(int j, int i) {
        TrainStation s = new TrainStation(10);
        if (i > j) {
            if (i-j < 8) {
                s.arrive(i-j);
            }
        }
        else {
            if (j-i < 8) {
                s.arrive(j-i);
            }
        }
    }
}