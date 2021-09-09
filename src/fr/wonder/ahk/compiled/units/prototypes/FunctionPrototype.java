package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.GenericContext;

public class FunctionPrototype implements VarAccess, Prototype<FunctionPrototype> {

	public final Signature signature;
	/** The type of this function, contains its arguments and return type */
	public final VarFunctionType functionType;
	public final GenericContext genericContext;
	public final DeclarationModifiers modifiers;
	
	public FunctionPrototype(
			Signature signature,
			VarFunctionType functionType,
			GenericContext genericContext,
			DeclarationModifiers modifiers) {
		
		this.signature = signature;
		this.functionType = functionType;
		this.genericContext = genericContext;
		this.modifiers = modifiers;
	}
	
	@Override
	public String toString() {
		return signature.declaringUnit + "." + signature.name + ":" + functionType.toString();
	}
	
	public String getName() {
		return signature.name;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	public DeclarationModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public VarFunctionType getType() {
		return functionType;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(signature, functionType, modifiers);
	}
	
	public boolean hasGenericBindings() {
		return genericContext.hasGenericMembers();
	}

}
