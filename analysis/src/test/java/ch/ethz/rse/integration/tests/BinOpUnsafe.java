package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE


public class BinOpUnsafe {
	public static void m1() {
		TrainStation s = new TrainStation(10);
		 int x = 10;
		 
		 x = x + 5; // 15
		 x = x-12; // 3; 
		 x = x * 9; // 27;
		 x -= 30; //-3
		 s.arrive(x);
		 
		 int y = 10;
		 int k = 3;
		 int l = y + k; // 13;
		 int u = l *3; // 39;
		 int res = u + 2; // 41
		 s.arrive(res);
		 
		 
		 
		 int tmp = 3;
		 tmp = tmp*tmp;
		 tmp++;
		 
		s.arrive(tmp);
		
		s.arrive(10);
		 
		  
		  
	}
}
