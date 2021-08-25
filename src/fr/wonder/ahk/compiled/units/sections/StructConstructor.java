package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class StructConstructor implements SourceElement {
	
	public final SourceReference sourceRef;
	public final StructSection struct;
	public final FunctionArgument[] arguments;
	public final DeclarationModifiers modifiers;
	
	private ConstructorPrototype prototype;
	
	public StructConstructor(StructSection struct, SourceReference sourceRef,
			DeclarationModifiers modifiers, FunctionArgument[] arguments) {
		
		this.sourceRef = sourceRef;
		this.struct = struct;
		this.modifiers = modifiers;
		this.arguments = arguments;
	}
	
	public VarType[] getArgumentTypes() {
		return ArrayOperator.map(arguments, VarType[]::new, arg -> arg.type);
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@Override
	public String toString() {
		return "constructor(" + Utils.toString(arguments) + ")";
	}

	public void setSignature(Signature signature) {
		this.prototype = new ConstructorPrototype(
				ArrayOperator.map(arguments, VarType[]::new, a->a.type),
				ArrayOperator.map(arguments, String[]::new, a->a.name),
				modifiers,
				signature
		);
	}
	
	public ConstructorPrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("Signature not set");
		return prototype;
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
