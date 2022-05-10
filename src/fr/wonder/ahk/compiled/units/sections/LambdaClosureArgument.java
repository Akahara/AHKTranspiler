package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.linker.Signatures;

/**
 * Lambda closure arguments are variables used inside of lambdas that
 * were declared outside. When the lambda is created all of its closure
 * arguments are computed and stored in the lambda closure, they can
 * latter be used by the lambda.
 */
public class LambdaClosureArgument implements VarAccess, SourceElement {
	
	public static final LambdaClosureArgument[] NO_ARGUMENTS = new LambdaClosureArgument[0];
	
	private final SourceReference sourceRef;
	private final String varName;
	private VarAccess originalAccess;
	private Signature proxySignature;
	
	public LambdaClosureArgument(SourceReference sourceRef, String varName) {
		this.sourceRef = sourceRef;
		this.varName = varName;
	}
	
	public String getVarName() {
		return varName;
	}
	
	public void setOriginalVariable(VarAccess variable) {
		this.originalAccess = variable;
		this.proxySignature = Signatures.ofClosureArgument(variable);
	}

	public VarAccess getOriginalVariable() {
		return originalAccess;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}

	@Override
	public Signature getSignature() {
		return proxySignature;
	}

	@Override
	public VarType getType() {
		return originalAccess.getType();
	}
	
	@Override
	public String toString() {
		return (originalAccess == null ? "unlinked" : getType()) + " " + varName;
	}
	
}
