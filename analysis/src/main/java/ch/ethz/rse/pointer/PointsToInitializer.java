package ch.ethz.rse.pointer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.spark.pag.Node;

public class PointsToInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PointsToInitializer.class);

	/**
	 * Internally used points-to analysis
	 */
	private final PointsToAnalysisWrapper pointsTo;

	/**
	 * class for which we are running points-to
	 */
	private final SootClass c;

	/**
	 * Maps abstract object indices to initializers
	 */
	private final Map<Node, TrainStationInitializer> initializers = new HashMap<Node, TrainStationInitializer>();

	/**
	 * All {@link TrainStationInitializer}s, keyed by method
	 */
	private final Multimap<SootMethod, TrainStationInitializer> perMethod = HashMultimap.create();

	public PointsToInitializer(SootClass c) {
		this.c = c;
		logger.debug("Running points-to analysis on " + c.getName());
		this.pointsTo = new PointsToAnalysisWrapper(c);
		logger.debug("Analyzing initializers in " + c.getName());
		this.analyzeAllInitializers();
	}

	private void analyzeAllInitializers() {
		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			SootHelper.getUnitGraph(method).forEach(unit -> {
				if (unit instanceof JInvokeStmt) {
					JInvokeStmt stmt = (JInvokeStmt) unit;
					if (stmt.getInvokeExpr().getMethod().getName().contains("<init>")) {
						//logger.info("analyzing statement: " + stmt.toString());
						InvokeExpr expr = stmt.getInvokeExpr();
						Value arg = expr.getArg(0);
						Value trainStationVar = (expr.getUseBoxes().get(1)).getValue();
						TrainStationInitializer trainStationInitializer = new TrainStationInitializer(stmt, trainStationVar.hashCode(), ((IntConstant) arg).value, trainStationVar.toString());
						initializers.put(pointsTo.getNodes((Local) trainStationVar).iterator().next(), trainStationInitializer);
						perMethod.put(method, trainStationInitializer);
					}
				}
			});
		}
	}

	public Collection<TrainStationInitializer> getTrainStationsByPointer(Value trainStationVar){
		Local trainStation = (Local) trainStationVar;
		Collection<TrainStationInitializer> trainStationInitializers = new LinkedList<>();
		pointsTo.getNodes(trainStation).forEach(node -> {
			trainStationInitializers.add(initializers.get(node));
		});

		return trainStationInitializers;
	}

	public Collection<TrainStationInitializer> getTrainStations(SootMethod method) {
		return perMethod.get(method);
	}
}
