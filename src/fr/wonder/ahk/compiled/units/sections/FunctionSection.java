package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class FunctionSection extends SourceObject {
	
	private final int declarationStop;
	public final Unit unit;
	
	// set by the unit parser
	public String name;
	public VarType returnType;
	public FunctionArgument[] arguments;
	public DeclarationModifiers modifiers;
	public final DeclarationVisibility visibility = DeclarationVisibility.GLOBAL;
	
	// set by the unit parser using the statement parser
	public Statement[] body;
	
	// set by the linker using #makeSignature
	private FunctionPrototype prototype;
	
	public FunctionSection(Unit unit, int sourceStart, int sourceStop, int declarationStop, DeclarationModifiers modifiers) {
		super(unit.source, sourceStart, sourceStop);
		this.unit = unit;
		this.declarationStop = declarationStop;
		this.modifiers = modifiers;
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
		this.prototype = new FunctionPrototype(signature, getFunctionType(), modifiers);
	}
	
	/**
	 * global scope signature, must not be called before the linker
	 * prelinked the declaring unit of this function
	 */
	public Signature getSignature() {
		return prototype.getSignature();
	}
	
	/**
	 * global scope signature, must not be called before the linker
	 * prelinked the declaring unit of this function
	 */
	public FunctionPrototype getPrototype() {
		return prototype;
	}
	
	public String getName() {
		return name;
	}
	
	public VarType[] getArgumentTypes() {
		return ArrayOperator.map(arguments, VarType[]::new, arg -> arg.type);
	}
	
	/**
	 * Returns the {@link VarFunctionType} associated with this function,
	 * cannot be called before the linker linked types.
	 */
	public VarFunctionType getFunctionType() {
		return new VarFunctionType(returnType, getArgumentTypes());
	}

}
