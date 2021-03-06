package ch.ethz.rse.verify;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Iterables;

import apron.Environment;
import ch.ethz.rse.pointer.PointsToInitializer;
import soot.*;

/**
 * Container for environment containing all relevant values
 *
 */
public class EnvironmentGenerator {

	private final SootMethod method;
	private final PointsToInitializer pointsTo;

	/**
	 * List of names for integer variables relevant when analyzing the program
	 */
	private List<String> ints = new LinkedList<String>();


	private final Environment env;

	/**
	 * 
	 * @param method
	 * @param pointsTo
	 */
	public EnvironmentGenerator(SootMethod method, PointsToInitializer pointsTo) {
		this.method = method;
		this.pointsTo = pointsTo;

		// populate this.ints

		method.retrieveActiveBody().getLocals().forEach(local -> {
			this.ints.add(local.getName());
		});

		String ints_arr[] = Iterables.toArray(this.ints, String.class);
		String reals[] = {}; // we are not analyzing real numbers
		this.env = new Environment(ints_arr, reals);
	}

	public Environment getEnvironment() {
		return this.env;
	}


}
