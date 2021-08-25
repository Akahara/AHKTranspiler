package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class FunctionSection implements SourceElement {
	
	public final SourceReference sourceRef;
	public final Unit unit;
	
	// set by the unit parser
	public String name;
	public VarType returnType;
	public FunctionArgument[] arguments;
	public DeclarationModifiers modifiers;
	
	// set by the unit parser using the statement parser
	public Statement[] body;
	
	// set by the linker using #makeSignature
	private FunctionPrototype prototype;
	
	public FunctionSection(Unit unit, SourceReference sourceRef, DeclarationModifiers modifiers) {
		this.sourceRef = sourceRef;
		this.unit = unit;
		this.modifiers = modifiers;
	}
	
	public static FunctionSection dummyFunction() {
		return new FunctionSection(Invalids.UNIT, Invalids.SOURCE_REF, DeclarationModifiers.NONE);
	}
	
	@Override
	public String toString() {
		return "func " + returnType + " " + name + "(" + Utils.toString(arguments) + ")";
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
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
