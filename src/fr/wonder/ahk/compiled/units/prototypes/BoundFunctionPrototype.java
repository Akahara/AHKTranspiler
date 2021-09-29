package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.units.sections.GenericContext;

public class BoundFunctionPrototype extends FunctionPrototype {
	
	public final FunctionPrototype originalFunction;
	public final GenericImplementationParameter[] typesParameters;
	
	public BoundFunctionPrototype(
			FunctionPrototype original,
			VarFunctionType boundType,
			GenericContext genericContext,
			GenericImplementationParameter[] typesParameters) {
		super(
				original.signature,
				boundType,
				genericContext,
				original.modifiers);
		this.originalFunction = original;
		this.typesParameters = typesParameters;
	}

}
