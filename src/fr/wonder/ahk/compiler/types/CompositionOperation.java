package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.utils.Assertions;

/**
 * Compositions are the way to get a single
 * function from two, by applying one after
 * the other.
 * 
 * <p>
 * Compositions can be made using the << and
 * >> operators, the first function applied
 * is the left one for >>, the right one for
 * <<.
 * 
 * <pre>
 * <blockquote>
 * func int f(int x) {
 *   return 2*x;
 * }
 * 
 * func int g(int x) {
 *   return 3;
 * }
 * 
 * (f >> g)(0) // applies f then g, returns 3
 * (g << f)(0) // applies g then f, returns 6
 * </blockquote>
 * </pre>
 */
public class CompositionOperation extends Operation {
	
	public CompositionOperation(VarFunctionType l, VarFunctionType r, Operator o) {
		super(l, r, o, getComposedType(l, r, o));
		
		Assertions.assertIn(o, Operator.SHR, Operator.SHL);
	}
	
	private static VarFunctionType getComposedType(VarFunctionType l, VarFunctionType r, Operator o) {
		if(l.genericContext.hasGenericMembers() || r.genericContext.hasGenericMembers())
			throw new UnimplementedException("Generic functions composition"); // TODO implement generic function composition
		if(o == Operator.SHR) {
			return new VarFunctionType(r.returnType, l.arguments, GenericContext.EMPTY_CONTEXT);
		} else {
			return new VarFunctionType(l.returnType, r.arguments, GenericContext.EMPTY_CONTEXT);
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
