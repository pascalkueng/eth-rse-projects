package ch.ethz.rse.numerical;

import java.util.*;

import apron.*;

import gmp.Mpq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.verify.EnvironmentGenerator;
import soot.ArrayType;
import soot.DoubleType;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;


public class NumericalAnalysis extends ForwardBranchedFlowAnalysis<NumericalStateWrapper> {

	private static final Logger logger = LoggerFactory.getLogger(NumericalAnalysis.class);

	private final SootMethod method;

	public Map<JInvokeStmt, NumericalStateWrapper> numericalStateWrapperMap = new HashMap<JInvokeStmt, NumericalStateWrapper>();

	private final PointsToInitializer pointsTo;

	/**
	 * number of times this loop head was encountered during analysis
	 */
	private HashMap<Unit, IntegerWrapper> loopHeads = new HashMap<Unit, IntegerWrapper>();
	/**
	 * Previously seen abstract state for each loop head
	 */
	private HashMap<Unit, NumericalStateWrapper> loopHeadState = new HashMap<Unit, NumericalStateWrapper>();

	/**
	 * Numerical abstract domain to use for analysis: COnvex polyhedra
	 */
	public final Manager man = new Polka(true);

	public final Environment env;

	/**
	 * We apply widening after updating the state at a given merge point for the
	 * {@link WIDENING_THRESHOLD}th time
	 */
	private static final int WIDENING_THRESHOLD = 6;
	//private Logger ;

	/**
	 *
	 * @param method   method to analyze
	 * @param g        control flow graph of the method
	 * @param pointsTo result of points-to analysis
	 */
	public NumericalAnalysis(SootMethod method, UnitGraph g, PointsToInitializer pointsTo) {
		super(g);
		this.method = method;
		this.pointsTo = pointsTo;

		this.env = new EnvironmentGenerator(method, this.pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class
		logger.info("Analyzing {} in {}", method.getName(), method.getDeclaringClass().getName());
		doAnalysis();

	}

	// Use this constructor for tests....
	public NumericalAnalysis(SootMethod method, UnitGraph g, PointsToInitializer pointsTo, int i) {
		super(g);

		this.method = method;
		this.pointsTo = pointsTo;

		this.env = new EnvironmentGenerator(method, this.pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class

	}

	/**
	 * Report unhandled instructions, types, cases, etc.
	 *
	 * @param task description of current task
	 * @param what
	 */
	public static void unhandled(String task, Object what, boolean raiseException) {
		String description = task + ": Can't handle " + what.toString() + " of type " + what.getClass().getName();

		if (raiseException) {
			throw new UnsupportedOperationException(description);
		} else {
			logger.error(description);

			// print stack trace
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stackTrace.length; i++) {
				logger.error(stackTrace[i].toString());
			}
		}
	}

	@Override
	protected void copy(NumericalStateWrapper source, NumericalStateWrapper dest) {
		source.copyInto(dest);
	}

	@Override
	protected NumericalStateWrapper newInitialFlow() {
		// should be bottom (only entry flows are not bottom originally)
		return NumericalStateWrapper.bottom(man, env);
	}

	@Override
	protected NumericalStateWrapper entryInitialFlow() {
		// state of entry points into function
		NumericalStateWrapper ret = NumericalStateWrapper.top(man, env);

		return ret;
	}

	@Override
	protected void merge(Unit succNode, NumericalStateWrapper w1, NumericalStateWrapper w2, NumericalStateWrapper w3) {
		logger.debug("in merge: " + succNode);

		IntegerWrapper counter = loopHeads.get(succNode);

		try {
			if (counter == null) {
				w3.set(w1.join(w2).get());
			} else {
				counter.value++;
				if (counter.value >= WIDENING_THRESHOLD) {
					w3.set(w1.widen(w2).get());
				} else {
					w3.set(w1.join(w2).get());
					loopHeadState.put(succNode, w3);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}


	@Override
	protected void merge(NumericalStateWrapper src1, NumericalStateWrapper src2, NumericalStateWrapper trg) {
		// this method is never called, we are using the other merge instead
		throw new UnsupportedOperationException();
	}

	@Override
	protected void flowThrough(NumericalStateWrapper inWrapper, Unit op, List<NumericalStateWrapper> fallOutWrappers,
							   List<NumericalStateWrapper> branchOutWrappers) {
		logger.debug(inWrapper + " " + op + " => ?");
		Stmt s = (Stmt) op;
		// wrapper for state after running op, assuming we move to the next statement
		assert fallOutWrappers.size() <= 1;
		NumericalStateWrapper fallOutWrapper = null;
		if (fallOutWrappers.size() == 1) {
			fallOutWrapper = fallOutWrappers.get(0);
			inWrapper.copyInto(fallOutWrapper);
		}

		// wrapper for state after running op, assuming we follow a jump
		assert branchOutWrappers.size() <= 1;
		NumericalStateWrapper branchOutWrapper = null;
		if (branchOutWrappers.size() == 1) {
			branchOutWrapper = branchOutWrappers.get(0);
			inWrapper.copyInto(branchOutWrapper);
		}

		try {

			if (s instanceof DefinitionStmt) {
				// handle assignment
				DefinitionStmt sd = (DefinitionStmt) s;
				Value left = sd.getLeftOp();
				Value right = sd.getRightOp();

				// We are not handling these cases:
				if (!(left instanceof JimpleLocal)) {
					unhandled("Assignment to non-local variable", left, true);
				} else if (left instanceof JArrayRef) {
					unhandled("Assignment to a non-local array variable", left, true);
				} else if (left.getType() instanceof ArrayType) {
					unhandled("Assignment to Array", left, true);
				} else if (left.getType() instanceof DoubleType) {
					unhandled("Assignment to double", left, true);
				} else if (left instanceof JInstanceFieldRef) {
					unhandled("Assignment to field", left, true);
				}

				if (left.getType() instanceof RefType) {
					// assignments to references are handled by pointer analysis
					// no action necessary
				} else {
					// handle assignment
					handleDef(fallOutWrapper, left, right);
				}

			} else if (s instanceof JIfStmt) {
				// handle if

				Lincons1 linconsBranchOut = null;
				Lincons1 linconsFallOut = null;

				ConditionExpr expr = (ConditionExpr) ((JIfStmt) s).getCondition();

				if (expr instanceof JLeExpr) {
					logger.info("LE If Statement");
					linconsBranchOut = getLincons(expr, "LE");
					linconsFallOut = getLincons(expr, "GT");
				} else if (expr instanceof JLtExpr) {
					logger.info("LT If Statement");
					linconsBranchOut = getLincons(expr, "LT");
					linconsFallOut = getLincons(expr, "GE");
				} else if (expr instanceof JGtExpr) {
					logger.info("GT If Statement");
					linconsBranchOut = getLincons(expr, "GT");
					linconsFallOut = getLincons(expr, "LE");
				} else if (expr instanceof JGeExpr) {
					logger.info("GE If Statement");
					linconsBranchOut = getLincons(expr, "GE");
					linconsFallOut = getLincons(expr, "LT");
				} else if (expr instanceof JEqExpr) {
					logger.info("EQ If Statement");
					linconsBranchOut = getLincons(expr, "EQ");
					linconsFallOut = getLincons(expr, "NEQ");
				} else if (expr instanceof JNeExpr) {
					logger.info("NE If Statement");
					linconsBranchOut = getLincons(expr, "NEQ");
					linconsFallOut = getLincons(expr, "EQ");
				} else {
					logger.info("cant handle if statement");
				}

				branchOutWrapper.set(inWrapper.get().meetCopy(man, linconsBranchOut));
				fallOutWrapper.set(inWrapper.get().meetCopy(man, linconsFallOut));

				logger.info("fallout: " + fallOutWrapper.toString());
				logger.info("branchout: " + branchOutWrapper.toString());

			} else if (s instanceof JInvokeStmt && ((JInvokeStmt) s).getInvokeExpr() instanceof JVirtualInvokeExpr) {
				// handle invocations
				JInvokeStmt jInvStmt = (JInvokeStmt) s;
				handleInvoke(jInvStmt, fallOutWrapper);
			}
			// log outcome
			if (fallOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[fallout] " + fallOutWrapper);
			}
			if (branchOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[branchout] " + branchOutWrapper);
			}

		} catch (ApronException e) {
			throw new RuntimeException(e);
		}
	}

	public Lincons1 getLincons(BinopExpr expression, String Op) {
		Value operand1 = expression.getOp1();
		Value operand2 = expression.getOp2();

		Linexpr1 linexpr1 = new Linexpr1(env);
		Lincons1 lincons1 = null;

		if (Op.equals("LE")) {
			addOperatorToLinearExpression(linexpr1, operand1, "neg");
			addOperatorToLinearExpression(linexpr1, operand2, "pos");
			lincons1 = new Lincons1(Lincons1.SUPEQ, linexpr1);
		} else if (Op.equals("LT")) {
			addOperatorToLinearExpression(linexpr1, operand1, "neg");
			addOperatorToLinearExpression(linexpr1, operand2, "pos");
			lincons1 = new Lincons1(Lincons1.SUP, linexpr1);
		} else if (Op.equals("GT")) {
			addOperatorToLinearExpression(linexpr1, operand1, "pos");
			addOperatorToLinearExpression(linexpr1, operand2, "neg");
			lincons1 = new Lincons1(Lincons1.SUP, linexpr1);
		} else if (Op.equals("GE")) {
			addOperatorToLinearExpression(linexpr1, operand1, "pos");
			addOperatorToLinearExpression(linexpr1, operand2, "neg");
			lincons1 = new Lincons1(Lincons1.SUPEQ, linexpr1);
		} else if (Op.equals("EQ")) {
			addOperatorToLinearExpression(linexpr1, operand1, "pos");
			addOperatorToLinearExpression(linexpr1, operand2, "neg");
			lincons1 = new Lincons1(Lincons1.EQ, linexpr1);
		} else if (Op.equals("NEQ")) {
			addOperatorToLinearExpression(linexpr1, operand1, "pos");
			addOperatorToLinearExpression(linexpr1, operand2, "neg");
			lincons1 = new Lincons1(Lincons1.DISEQ, linexpr1);
		}
		return lincons1;
	}

	private void addOperatorToLinearExpression(Linexpr1 linexpr1, Value value, String sign) {
		if (sign.equals("pos")) {
			if (value instanceof JimpleLocal) {
				linexpr1.setCoeff(new StringVar(((JimpleLocal) value).getName()), new MpqScalar(1));
			} else if (value instanceof IntConstant) {
				linexpr1.setCst(new MpqScalar(((IntConstant) value).value));
			}
		} else if (sign.equals("neg")) {
			if (value instanceof JimpleLocal) {
				linexpr1.setCoeff(new StringVar(((JimpleLocal) value).getName()), new MpqScalar(-1));
			} else if (value instanceof IntConstant) {
				linexpr1.setCst(new MpqScalar(-((IntConstant) value).value));
			}
		}
	}

	public void handleInvoke(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		logger.info("putting invoke statement " + jInvStmt + " and falloutwrapper " + fallOutWrapper.toString() + " into map");

		numericalStateWrapperMap.put(jInvStmt, fallOutWrapper);

		Abstract1 fallout = fallOutWrapper.get();
		InvokeExpr expr = jInvStmt.getInvokeExpr();
		Value arg = expr.getArg(0);
		Value trainStationVar = (expr.getUseBoxes().get(1)).getValue();

		logger.info("trainstationvar: " + trainStationVar.toString());

		pointsTo.getTrainStationsByPointer(trainStationVar).forEach(trainStationInitializer -> {
			logger.info("current station: " + trainStationInitializer);
			Interval intervalArg = null;
			Interval intervalStation = null;
			if (arg instanceof JimpleLocal) {
				try {
					intervalArg = fallout.getBound(man, arg.toString());
				} catch (ApronException e) {
					e.printStackTrace();
				}
			} else if (arg instanceof IntConstant) {
				intervalArg = new Interval(((IntConstant) arg).value, ((IntConstant) arg).value);
			}

			try {
				intervalStation = fallout.getBound(man, trainStationInitializer.getVar());
			} catch (ApronException e) {
				e.printStackTrace();
			}

			Interval intervalCombined = new Interval();

			if (intervalArg.inf().cmp(intervalStation.inf()) == 1) {
				intervalCombined.setInf(intervalStation.inf());
			} else {
				intervalCombined.setInf(intervalArg.inf());
			}
			if (intervalArg.sup().cmp(intervalStation.sup()) == 1) {
				intervalCombined.setSup(intervalArg.sup());
			} else {
				intervalCombined.setSup(intervalStation.sup());
			}

			logger.info("arg interval: " + intervalArg);
			logger.info("station interval: " + intervalStation);
			logger.info("combined interval: " + intervalCombined);

			Lincons1 lincons1 = new Lincons1(Lincons1.EQ, new Linexpr1(env, new Linterm1[]{new Linterm1(trainStationInitializer.getVar(), new MpqScalar(-1))}, intervalCombined));
			try {
				fallout.forget(man, trainStationInitializer.getVar(), false);
				fallout.meet(man, lincons1);
			} catch (ApronException e) {
				e.printStackTrace();
			}
			fallOutWrapper.set(fallout);
			trainStationInitializer.addInvoke(jInvStmt);
		});

		logger.info("falloutwrapper after invoke handling: " + fallOutWrapper);
	}

	/**
	 *
	 * handle assignment
	 *
	 * @param in
	 * @param left
	 * @param right
	 * @return state of in after assignment
	 */
	public void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		logger.info("Def");
		Abstract1 out = outWrapper.get();

		if (right instanceof IntConstant) {
			logger.info("IntConstant");
			Linexpr1 linexpr = new Linexpr1(env);
			linexpr.setCoeff(left.toString(), new MpqScalar(1));
			linexpr.setCst(new MpqScalar(-((IntConstant) right).value));
			Lincons1 lincons = new Lincons1(Lincons1.EQ, linexpr);
			logger.info("abstract 1 before meet: " + out.toString());
			out.forget(man, left.toString(), false);
			out.meet(man, lincons);
			logger.info("abstract 1 after meet: " + out.toString());
			outWrapper.set(out);
		} else if (right instanceof JimpleLocal) {
			String rightVar = ((JimpleLocal) right).getName();
			StringVar leftVar = new StringVar(left.toString());
			Interval rightInterval = out.getBound(man, rightVar);
			Abstract1 rightAbstract = new Abstract1(man, env, new Var[]{leftVar}, new Interval[]{rightInterval});
			logger.info("abstract 1 before meet: " + out.toString());
			out.forget(man, leftVar, false);
			out.meet(man, rightAbstract);
			logger.info("abstract 1 after meet: " + out.toString());
			outWrapper.set(out);
		} else if (right instanceof JAddExpr) {
			logger.info("JAddExpr");
			handleBinopExpr(outWrapper, left, (JAddExpr) right);
		} else if (right instanceof JSubExpr) {
			logger.info("JSubExpr");
			handleBinopExpr(outWrapper, left, (JSubExpr) right);
		} else if (right instanceof JMulExpr) {
			logger.info("JMulExpr");
			handleBinopExpr(outWrapper, left, (JMulExpr) right);
		} else {
			Interval[] intervals = new Interval[1];
			Interval interval = new Interval();
			interval.setTop();
			intervals[0] = interval;
			outWrapper.set(new Abstract1(man, env, new String[] { ((Local) left).getName() }, intervals));
		}
	}

	private void handleBinopExpr(NumericalStateWrapper outWrapper, Value left, BinopExpr right) {

		Abstract1 out = outWrapper.get();

		Interval interval1 = getInterval(right.getOp1(), outWrapper.get());
		Interval interval2 = getInterval(right.getOp2(), outWrapper.get());
		Interval intervalresult = new Interval();
		logger.info("Interval1: " + interval1);
		logger.info("Interval2: " + interval2);
		logger.info("operation: " + right.toString());

		if(right instanceof JAddExpr) {
			assert interval1 != null;
			if (interval1.isTop()) {
				intervalresult.setTop();;
			} else {
				assert interval2 != null;
				if (interval2.isTop()) {
					intervalresult.setTop();
				} else {
					MpqScalar resultSupremum = new MpqScalar();
					MpqScalar resultInfimum = new MpqScalar();
					if (interval1.sup().isInfty() !=0) {
						resultSupremum.setInfty(interval1.sup().isInfty());
					} else if (interval2.sup().isInfty() != 0) {
						resultSupremum.setInfty(interval2.sup().isInfty());
					}

					if (interval1.inf().isInfty() != 0) {
						resultInfimum.setInfty(interval1.inf().isInfty());
					} else if (interval2.inf().isInfty() != 0) {
						resultInfimum.setInfty(interval2.inf().isInfty());
					}

					if (interval1.inf().isInfty() == 0 && interval2.inf().isInfty() == 0) {
						Mpq interval1Inf = new Mpq();
						Mpq interval2Inf = new Mpq();

						interval1.inf().toMpq(interval1Inf, 0);
						interval2.inf().toMpq(interval2Inf, 0);

						interval1Inf.add(interval2Inf);
						resultInfimum.set(interval1Inf);
					}
					if (interval1.sup().isInfty() == 0 && interval2.sup().isInfty() == 0) {
						Mpq interval1Supremum = new Mpq();
						Mpq interval2Supremum = new Mpq();

						interval1.sup().toMpq(interval1Supremum, 0);
						interval2.sup().toMpq(interval2Supremum, 0);

						interval1Supremum.add(interval2Supremum);
						resultSupremum.set(interval1Supremum);
					}
					intervalresult.setInf(resultInfimum);
					intervalresult.setSup(resultSupremum);
				}
			}

			logger.info("result interval is: " + intervalresult);

			StringVar targetVar = new StringVar(left.toString());
			Abstract1 sourceAbs = null;
			try {
				sourceAbs = new Abstract1(man, env,
						new Var[]{targetVar}, new Interval[]{intervalresult});
				out.forget(man, new StringVar(left.toString()), false);
				out.meet(man, sourceAbs);
			} catch (ApronException e) {
				e.printStackTrace();
			}

			outWrapper.set(out);

			logger.info("abstract1 out: " + out.toString());
			logger.info("outwrapper: " + outWrapper.toString());

		} else if (right instanceof JSubExpr) {
			Interval interval2new = new Interval(interval2);
			interval2new.sup().neg();
			interval2new.inf().neg();
			Scalar sup = interval2new.sup();
			interval2new.setSup(interval2new.inf());
			interval2new.setInf(sup);

			interval2 = interval2new;

			assert interval1 != null;
			if (interval1.isTop()) {
				intervalresult.setTop();;
			} else {
				assert interval2 != null;
				if (interval2.isTop()) {
					intervalresult.setTop();
				} else {
					MpqScalar resultSupremum = new MpqScalar();
					MpqScalar resultInfimum = new MpqScalar();
					if (interval1.sup().isInfty() !=0) {
						resultSupremum.setInfty(interval1.sup().isInfty());
					} else if (interval2.sup().isInfty() != 0) {
						resultSupremum.setInfty(interval2.sup().isInfty());
					}

					if (interval1.inf().isInfty() != 0) {
						resultInfimum.setInfty(interval1.inf().isInfty());
					} else if (interval2.inf().isInfty() != 0) {
						resultInfimum.setInfty(interval2.inf().isInfty());
					}

					if (interval1.inf().isInfty() == 0 && interval2.inf().isInfty() == 0) {
						Mpq interval1Inf = new Mpq();
						Mpq interval2Inf = new Mpq();

						interval1.inf().toMpq(interval1Inf, 0);
						interval2.inf().toMpq(interval2Inf, 0);

						interval1Inf.add(interval2Inf);
						resultInfimum.set(interval1Inf);
					}
					if (interval1.sup().isInfty() == 0 && interval2.sup().isInfty() == 0) {
						Mpq interval1Supremum = new Mpq();
						Mpq interval2Supremum = new Mpq();

						interval1.sup().toMpq(interval1Supremum, 0);
						interval2.sup().toMpq(interval2Supremum, 0);

						interval1Supremum.add(interval2Supremum);
						resultSupremum.set(interval1Supremum);
					}
					intervalresult.setInf(resultInfimum);
					intervalresult.setSup(resultSupremum);
				}
			}

			logger.info("result interval is: " + intervalresult);

			StringVar targetVar = new StringVar(left.toString());
			Abstract1 sourceAbs = null;
			try {
				sourceAbs = new Abstract1(man, env,
						new Var[]{targetVar}, new Interval[]{intervalresult});
				out.forget(man, new StringVar(left.toString()), false);
				out.meet(man, sourceAbs);
			} catch (ApronException e) {
				e.printStackTrace();
			}

			outWrapper.set(out);

			logger.info("outwrapper: " + outWrapper.toString());

		} else if (right instanceof JMulExpr) {
			Scalar[] scalars = new Scalar[4];
			scalars[0] = multiply(interval1.sup(), interval2.sup());
			scalars[1] = multiply(interval1.inf(), interval2.sup());
			scalars[2] = multiply(interval1.sup(), interval2.inf());
			scalars[3] = multiply(interval1.inf(), interval2.inf());

			Scalar min = scalars[0];
			Scalar max = scalars[0];

			for (int i = 1; i<4; i++) {
				if (scalars[i].cmp(min) == -1) {
					min = scalars[i];
				}
			}
			intervalresult.setInf(min);

			for (int i = 1; i<4; i++) {
				if (scalars[i].cmp(max) == 1) {
					max = scalars[i];
				}
			}
			intervalresult.setSup(max);

			logger.info("result interval is: " + intervalresult);

			StringVar targetVar = new StringVar(left.toString());
			Abstract1 sourceAbs = null;
			try {
				sourceAbs = new Abstract1(man, env,
						new Var[]{targetVar}, new Interval[]{intervalresult});
				out.forget(man, new StringVar(left.toString()), false);
				out.meet(man, sourceAbs);
			} catch (ApronException e) {
				e.printStackTrace();
			}

			outWrapper.set(out);
			logger.info("outwrapper: " + outWrapper.toString());
		}
	}

	private Scalar multiply(Scalar scalar1, Scalar scalar2) {
		MpqScalar result = new MpqScalar();

		Mpq mpq1 = new Mpq();
		Mpq mpq2 = new Mpq();
		Mpq mpqZero = new Mpq(0);
		scalar1.toMpq(mpq1, 0);
		scalar2.toMpq(mpq2, 0);

		boolean scalar1Pos;
		boolean scalar2Pos;

		if (mpq1.cmp(mpqZero) == 1 || mpq1.cmp(mpqZero) == 0) {
			scalar1Pos = true;
		} else {
			scalar1Pos = false;
		}
		if (mpq2.cmp(mpqZero) == 1 || mpq2.cmp(mpqZero) == 0) {
			scalar2Pos = true;
		} else {
			scalar2Pos = false;
		}

		if (scalar1.isInfty() == 0 && scalar2.isInfty() == 0){
			mpq1.mul(mpq2);
			result.set(mpq1);
		} else if (scalar1.isInfty() == 1 && scalar2Pos) {
			result.setInfty(1);
		} else if (scalar1.isInfty() == 1 && !scalar2Pos) {
			result.setInfty(-1);
		} else if (scalar1.isInfty() == -1 && scalar2Pos) {
			result.setInfty(-1);
		} else if (scalar1.isInfty() == -1 && !scalar2Pos) {
			result.setInfty(1);
		} else if (scalar2.isInfty() == 1 && scalar1Pos) {
			result.setInfty(1);
		} else if (scalar2.isInfty() == 1 && !scalar1Pos) {
			result.setInfty(-1);
		} else if (scalar2.isInfty() == -1 && scalar1Pos) {
			result.setInfty(-1);
		} else if (scalar2.isInfty() == -1 && !scalar1Pos) {
			result.setInfty(1);
		}
		return result;
	}

	private Interval getInterval(Value value, Abstract1 abstract1) {
		try {
			if (value instanceof IntConstant) {
				return new Interval(((IntConstant) value).value, ((IntConstant) value).value);
			} else if (value instanceof JimpleLocal) {
				return abstract1.getBound(man, new StringVar(((JimpleLocal) value).getName()));
			} else {
				return null;
			}
		} catch (ApronException e) {
			throw new RuntimeException();
		}
	}
}
