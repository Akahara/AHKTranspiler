package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.commons.utils.Assertions;

public class CompositionOperation extends Operation {
	
	public CompositionOperation(VarFunctionType l, VarFunctionType r, Operator o) {
		super(l, r, o, getComposedType(l, r, o));
		
		Assertions.assertIn(o, Operator.SHR, Operator.SHL);
	}
	
	private static VarFunctionType getComposedType(VarFunctionType l, VarFunctionType r, Operator o) {
		if(o == Operator.SHR) {
			return new VarFunctionType(r.returnType, l.arguments);
		} else {
			return new VarFunctionType(l.returnType, r.arguments);
		}
	}
	
	public boolean isLeftFirstApplied() {
		return operator == Operator.SHR;
	}

	public VarFunctionType getLOType() {
		return (VarFunctionType) loType;
	}
	
	public VarFunctionType getROType() {
		return (VarFunctionType) roType;
	}
	
	public VarFunctionType getFirstApplied() {
		return isLeftFirstApplied() ? getLOType() : getROType();
	}
	
	public VarFunctionType getSecondApplied() {
		return isLeftFirstApplied() ? getROType() : getLOType();
	}
	
}
