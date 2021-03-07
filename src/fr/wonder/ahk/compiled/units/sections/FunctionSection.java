package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.utils.Utils;

public class FunctionSection extends SourceObject implements ValueDeclaration {
	
	public final int declarationStop;
	
	// set by the unit parser
	public String name;
	public VarType returnType;
	public FunctionArgument[] arguments;
	public VarType[] argumentTypes;
	public DeclarationModifiers modifiers;
	
	// set by the unit parser using the statement parser
	public Statement[] body;
	
	// set by the linker using #makeSignature
	private Signature signature;
	
	public FunctionSection(UnitSource source, int sourceStart, int sourceStop, int declarationStop) {
		super(source, sourceStart, sourceStop);
		this.declarationStop = declarationStop;
	}
	
	@Override
	public String toString() {
		return "func " + returnType + " " + name + "(" + Utils.toString(arguments) + ")";
	}
	
	@Override
	public String getErr() {
		return getSource().getErr(getSourceStart(), declarationStop);
	}

	/** Called by the linker after the function argument types where computed */
	public void setSignature(Signature signature) {
		this.signature = signature;
	}
	
	/**
	 * global scope signature (declaring unit full base + unit scope signature),
	 * must not be called before the linker prelinked the declaring unit of this
	 * function
	 */
	public Signature getSignature() {
		return signature;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	/** Overrides {@link ValueDeclaration#getType()} */
	@Override
	public VarType getType() {
		return getFunctionType();
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
	/**
	 * Returns the {@link VarFunctionType} associated with this function,
	 * cannot be called before the linker linked types.
	 */
	public VarFunctionType getFunctionType() {
		return new VarFunctionType(returnType, argumentTypes);
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.GLOBAL; // TODO read function visibility
	}
}
