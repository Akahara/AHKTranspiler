package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class FunctionSection extends SourceObject implements ValueDeclaration {
	
	public final int declarationStop;
	
	// set by the unit parser
	public String name;
	public VarType returnType;
	public FunctionArgument[] arguments;
	public DeclarationModifiers modifiers;
	
	// set by the unit parser using the statement parser
	public Statement[] body;
	
	// set by the linker using #makeSignature
	private FunctionPrototype prototype;
	
	public FunctionSection(UnitSource source, int sourceStart, int sourceStop, int declarationStop, DeclarationModifiers modifiers) {
		super(source, sourceStart, sourceStop);
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
		this.prototype = new FunctionPrototype(signature, getFunctionType(), getModifiers());
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
	
	@Override
	public String getName() {
		return name;
	}
	
	/** Overrides {@link ValueDeclaration#getType()} */
	@Override
	public VarType getType() {
		return getFunctionType();
	}
	
	public VarType[] getArgumentTypes() {
		return ArrayOperator.map(arguments, VarType[]::new, arg -> arg.type);
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
		return new VarFunctionType(returnType, getArgumentTypes());
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.GLOBAL; // TODO read function visibility
	}
}
