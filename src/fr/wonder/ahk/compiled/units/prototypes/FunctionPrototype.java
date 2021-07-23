package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.types.FunctionArguments;

public class FunctionPrototype implements VarAccess, Prototype<FunctionSection>, CallablePrototype {

	public final Signature signature;
	/** The type of this function, contains its arguments and return type */
	public final VarFunctionType functionType;
	
	public final DeclarationModifiers modifiers;
	
	public FunctionPrototype(Signature signature, VarFunctionType functionType, DeclarationModifiers modifiers) {
		this.signature = signature;
		this.functionType = functionType;
		this.modifiers = modifiers;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof FunctionPrototype))
			return false;
		FunctionPrototype p = (FunctionPrototype) o;
		if(!signature.equals(p.signature))
			return false;
		if(!functionType.returnType.equals(p.functionType.returnType))
			return false;
		if(functionType.arguments.length != p.functionType.arguments.length)
			return false;
		for(int i = 0; i < functionType.arguments.length; i++) {
			if(!functionType.arguments[i].equals(p.functionType.arguments[i]))
				return false;
		}
		return true;
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

	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public VarFunctionType getType() {
		return functionType;
	}
	
	@Override
	public VarType[] getArgumentTypes() {
		return functionType.arguments;
	}

	@Override
	public FunctionSection getAccess(Unit unit) {
		if(unit.fullBase.equals(signature.declaringUnit))
			throw new IllegalArgumentException("Function " + this + " is not declared in unit " + unit);
		for(FunctionSection f : unit.functions) {
			if(f.returnType.equals(functionType.returnType) && FunctionArguments.matchNoConversions(functionType.arguments, f.getArgumentTypes()))
				return f;
		}
		return null;
	}

	@Override
	public boolean matchesDeclaration(ValueDeclaration decl) {
		return decl instanceof FunctionSection && decl.getSignature().equals(getSignature());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(signature, functionType, modifiers);
	}

}
