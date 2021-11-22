package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

/**
 * Lambda closure arguments are variables used inside of lambdas that
 * were declared outside. When the lambda is created all of its closure
 * arguments are computed and stored in the lambda closure, they can
 * latter be used by the lambda.
 */
public class LambdaClosureArgument {
	
	public final VarAccess originalAccess;
	
	public LambdaClosureArgument(VarAccess access) {
		this.originalAccess = access;
	}
	
}
