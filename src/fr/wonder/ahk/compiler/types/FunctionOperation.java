package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

/**
 * Function operations are operations that take at least one
 * function-type operand.
 * 
 * <pre>
 * <blockquote>
 * alias IntFunc = func int(int);
 * alias FloatFunc = func float(int);
 * 
 * func int f(int x) {...}
 * 
 * // all these are well defined
 * IntFunc f1 = f;
 * IntFunc f2 = 2*f;
 * IntFunc f3 = f+f;
 * IntFunc f3 = f+5;
 * FloatFunc f4 = f+.5;
 * </blockquote>
 * </pre>
 * 
 * Any operator other than << and >> will be used to combine 
 * the result of the function operands. The << operator is used
 * to compose functions:
 * 
 * <pre>
 * <blockquote>
 * func int f(int x) {
 *   return 2*x;
 * }
 * func int g(int x) {
 *   return 3;
 * }
 * 
 * (f >> g)(0) // applies f then g, returns 3
 * (g << f)(0) // applies g then f, returns 6
 * </blockquote>
 * </pre>
 */
public class FunctionOperation extends Operation {

	public final Operation resultOperation;
	
	public FunctionOperation(VarType l, VarType r, Operation resultOperation, VarFunctionType funcType) {
		super(l, r, resultOperation.operator, funcType);
		this.resultOperation = resultOperation;
	}
	
}
