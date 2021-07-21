package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class StructConstructor extends SourceObject implements ValueDeclaration {
	
	public FunctionArgument[] arguments;
	private ConstructorPrototype prototype;
	
	public StructConstructor(UnitSource source, int sourceStart, int sourceStop,
			FunctionArgument[] arguments) {
		super(source, sourceStart, sourceStop);
		this.arguments = arguments;
	}
	
	public VarType[] getArgumentTypes() {
		return ArrayOperator.map(arguments, VarType[]::new, arg -> arg.type);
	}
	
	@Override
	public String toString() {
		return "constructor(" + Utils.toString(arguments) + ")";
	}

	@Override
	public DeclarationModifiers getModifiers() {
		return DeclarationModifiers.NONE;
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.GLOBAL; // TODO read constructor visibility
	}

	@Override
	public String getName() {
		throw new IllegalStateException("Constructors have no name");
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new ConstructorPrototype(
				ArrayOperator.map(arguments, VarType[]::new, a->a.type),
				ArrayOperator.map(arguments, String[]::new, a->a.name),
				signature
		);
	}
	
	public ConstructorPrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("Signature not set");
		return prototype;
	}

	@Override
	public Signature getSignature() {
		return prototype.getSignature();
	}

	@Override
	public VarType getType() {
		throw new IllegalStateException("Struct constructors have no type");
	}

	/**
	 * Returns a signature similar to {@link VarFunctionType} signatures but
	 * containing argument names and no return type.
	 */
	public String getConstructorSignature() {
		String sig = "";
		for(FunctionArgument arg : arguments)
			sig += arg.name.length() + arg.name + arg.getType().getSignature();
		return sig;
	}
	
}
