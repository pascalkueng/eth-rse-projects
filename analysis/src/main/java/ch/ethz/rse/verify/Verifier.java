package ch.ethz.rse.verify;

import java.util.*;

import apron.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.JInvokeStmt;

/**
 * Main class handling verification
 *
 */
public class Verifier extends AVerifier {

	private static final Logger logger = LoggerFactory.getLogger(Verifier.class);

	private final SootClass c;
	private final PointsToInitializer pointsTo;
	private final Map<SootMethod, NumericalAnalysis> numericalAnalysis = new HashMap<SootMethod, NumericalAnalysis>();

	/**
	 *
	 * @param c class to verify
	 */
	public Verifier(SootClass c) {
		logger.debug("Analyzing {}", c.getName());

		this.c = c;
		// pointer analysis
		this.pointsTo = new PointsToInitializer(this.c);
		// numerical analysis
		this.runNumericalAnalysis();
	}

	private void runNumericalAnalysis() {
		List<SootMethod> methods = c.getMethods();
		for (SootMethod m : methods) {
			if (m.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}


			numericalAnalysis.put(m, new NumericalAnalysis(m, SootHelper.getUnitGraph(m), pointsTo));
		}
	}

	@Override
	public boolean checkTrackNonNegative() {

		// return true if no methods in test
		if (numericalAnalysis.isEmpty()) {
			return true;
		}

		boolean trackNonNegative = true;

		for (SootMethod method : numericalAnalysis.keySet()) {
			//logger.info("analyzing method: " + method.getName());
			NumericalAnalysis numericalAnalysis1 = numericalAnalysis.get(method);
			Collection<TrainStationInitializer> trainStationInitializers = pointsTo.getTrainStations(method);
			Environment environment = numericalAnalysis1.env;
			Manager manager = numericalAnalysis1.man;

			for (TrainStationInitializer trainStationInitializer :
					trainStationInitializers) {
				//logger.info("analyzing trainstation: " + trainStationInitializer.toString());
				//logger.info("all invokes: " + trainStationInitializer.getInvokes());
				for (JInvokeStmt jInvokeStmt:
						trainStationInitializer.getInvokes()) {
					//logger.info("analyzing invoke statement: " + jInvokeStmt.toString());
					Value arg = jInvokeStmt.getInvokeExpr().getArg(0);

					if (arg instanceof Local) {
						NumericalStateWrapper fallOutWrapper = numericalAnalysis1.numericalStateWrapperMap.get(jInvokeStmt);
						Abstract1 fallOut = fallOutWrapper.get();
						MpqScalar mpqScalar1 = new MpqScalar(-1);
						MpqScalar mpqScalar2 = new MpqScalar(0);
						Linterm1 linterm1 = new Linterm1(((Local) arg).getName(), mpqScalar1);
						Linterm1[] linterm1s = new Linterm1[1];
						linterm1s[0] = linterm1;
						Linexpr1 linexpr1 = new Linexpr1(environment, linterm1s, mpqScalar2);
						Lincons1 lincons1 = new Lincons1(Lincons1.SUP, linexpr1);
						try {
							fallOut.meet(manager, lincons1);
							if (!fallOut.isBottom(manager)) {
								trackNonNegative = false;
							}
						} catch (ApronException e) {
							e.printStackTrace();
						}
					} else if (arg instanceof IntConstant) {
						if (((IntConstant) arg).value < 0) {
							trackNonNegative = false;
						}
					} else {
						throw new UnsupportedOperationException("can't handle this type of argument");
					}
				}
			}
		}
		return trackNonNegative;
	}

	@Override
	public boolean checkTrackInRange() {
		// return true if no methods in test
		if (numericalAnalysis.isEmpty()) {
			return true;
		}

		boolean trackInRange = true;

		for (SootMethod method : numericalAnalysis.keySet()) {
			//logger.info("analyzing method: " + method.getName());
			NumericalAnalysis numericalAnalysis1 = numericalAnalysis.get(method);
			Collection<TrainStationInitializer> trainStationInitializers = pointsTo.getTrainStations(method);
			Environment environment = numericalAnalysis1.env;
			Manager manager = numericalAnalysis1.man;

			for (TrainStationInitializer trainStationInitializer :
					trainStationInitializers) {
				//logger.info("analyzing trainstation: " + trainStationInitializer.toString());
				//logger.info("all invokes: " + trainStationInitializer.getInvokes());
				for (JInvokeStmt jInvokeStmt:
						trainStationInitializer.getInvokes()) {
					//logger.info("analyzing invoke statement: " + jInvokeStmt.toString());
					Value arg = jInvokeStmt.getInvokeExpr().getArg(0);
					//logger.info(arg.toString());


					if (arg instanceof Local) {
						NumericalStateWrapper fallOutWrapper = numericalAnalysis1.numericalStateWrapperMap.get(jInvokeStmt);
						Abstract1 fallOut = fallOutWrapper.get();
						MpqScalar mpqScalar1 = new MpqScalar(1);
						MpqScalar mpqScalar2 = new MpqScalar(-trainStationInitializer.nTracks);
						Linterm1 linterm1 = new Linterm1(((Local) arg).getName(), mpqScalar1);
						Linterm1[] linterm1s = new Linterm1[1];
						linterm1s[0] = linterm1;
						Linexpr1 linexpr1 = new Linexpr1(environment, linterm1s, mpqScalar2);
						Lincons1 lincons1 = new Lincons1(Lincons1.SUPEQ, linexpr1);
						//logger.info(linterm1.toString());
						//logger.info(linexpr1.toString());
						//logger.info("linear constraints: " + lincons1.toString());
						//logger.info("fallout: " + fallOut.toString());
						try {
							fallOut.meet(manager, lincons1);
							if (!fallOut.isBottom(manager)) {
								trackInRange = false;
							}
						} catch (ApronException e) {
							e.printStackTrace();
						}
					} else if (arg instanceof IntConstant) {
						if (((IntConstant) arg).value >= trainStationInitializer.nTracks) {
							trackInRange = false;
						}
					} else {
						throw new UnsupportedOperationException("can't handle this type of argument");
					}
				}
			}
		}
		return trackInRange;
	}

	@Override
	public boolean checkNoCrash() {

		// return true if no methods in test
		if (numericalAnalysis.isEmpty()) {
			return true;
		}

		boolean noCrash = true;

		for (SootMethod method : numericalAnalysis.keySet()) {
			//logger.info("analyzing method: " + method.getName());
			NumericalAnalysis numericalAnalysis1 = numericalAnalysis.get(method);
			Collection<TrainStationInitializer> trainStationInitializers = pointsTo.getTrainStations(method);
			Environment environment = numericalAnalysis1.env;
			Manager manager = numericalAnalysis1.man;

			for (TrainStationInitializer trainStationInitializer :
					trainStationInitializers) {
				//logger.info("analyzing trainstation: " + trainStationInitializer.toString());
				//logger.info("all invokes: " + trainStationInitializer.getInvokes());

				ArrayList<Interval> intervals = new ArrayList<>();

				for (JInvokeStmt jInvokeStmt:
						trainStationInitializer.getInvokes()) {
					//logger.info("analyzing invoke statement: " + jInvokeStmt.toString());
					Value arg = jInvokeStmt.getInvokeExpr().getArg(0);


					//logger.info(arg.toString());
					//logger.info(String.valueOf(arg instanceof Local));
					//logger.info("abstract1: " + abstract1);

					if (arg instanceof IntConstant) {
						intervals.add(new Interval(((IntConstant) arg).value, ((IntConstant) arg).value));
					} else {
						try {
							Abstract1 abstract1 = numericalAnalysis1.numericalStateWrapperMap.get(jInvokeStmt).get();
							Interval bound = abstract1.getBound(manager, arg.toString());
							//logger.info("bound: " + bound);
							intervals.add(bound);
						} catch (ApronException e) {
							e.printStackTrace();
						}
					}
				}

				for (int i = 0; i < intervals.size(); i++) {
					for (int j = i+1; j < intervals.size(); j++) {
						if (i != j && !checkNoOverlap(intervals.get(i), intervals.get(j))) {
							noCrash = false;
						}
					}
				}

				//logger.info("bounds: " + intervals.toString());
				//logger.info("no overlap is: " + noCrash);
			}
		}
		return noCrash;
	}

	private boolean checkNoOverlap(Interval interval1, Interval interval2) {
		Scalar inf1 = interval1.inf;
		Scalar inf2 = interval2.inf;
		Scalar sup1 = interval1.sup;
		Scalar sup2 = interval2.sup;
		return inf1.cmp(sup2) == 1 || inf2.cmp(sup1) == 1;
	}
}
