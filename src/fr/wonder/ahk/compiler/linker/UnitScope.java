package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.LinkedUnit;
import fr.wonder.ahk.compiler.types.ConversionTable;

class UnitScope implements Scope {
	
	final LinkedUnit declaringUnit;
	
	UnitScope(LinkedUnit declaringUnit) {
		this.declaringUnit = declaringUnit;
	}

	@Override
	public Scope innerScope() {
		return new InnerScope(this);
	}

	@Override
	public Scope outerScope() {
		throw new IllegalStateException("Invalid scope state");
	}

	@Override
	public UnitScope getUnitScope() {
		return this;
	}
	
	@Override
	public ValueDeclaration getVariable(String name) {
		int dot = name.indexOf('.');
		LinkedUnit unit;
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			name = name.substring(dot+1);
			unit = declaringUnit.getReachableUnit(unitName);
			if(unit == null)
				return null;
		} else {
			unit = declaringUnit;
		}
		// note that no function and variable can have the same name
		for(VariableDeclaration var : unit.variables)
			if(var.name.equals(name))
				return var;
		// also the linker will do the job of searching for the right function, no need to do that here
		// it only requires a function to be found to begin its work
		for(FunctionSection func : unit.functions)
			if(func.name.equals(name))
				return func;
		return null;
	}

	@Override
	public void registerVariable(ValueDeclaration var) {
		throw new IllegalStateException("Invalid scope state");
	}
	
	private static interface ArgumentsMatchPredicate {
		
		boolean matches(VarType[] args, VarType[] provided, ConversionTable conversions);
		
	}
	
	private int countMatchingFunctions(String name, VarType[] args, ConversionTable conversions, ArgumentsMatchPredicate predicate) {
		int dot = name.indexOf('.');
		LinkedUnit unit;
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			name = name.substring(dot+1);
			unit = declaringUnit.getReachableUnit(unitName);
		} else {
			unit = declaringUnit;
		}
		if(unit == null)
			return 0;
		int count = 0;
		for(FunctionSection func : unit.functions) {
			if(func.name.equals(name) && predicate.matches(func.argumentTypes, args, conversions))
				count++;
		}
		return count;
	}
	
	private FunctionSection getFunction(String name, VarType[] args, ConversionTable conversions, ArgumentsMatchPredicate predicate) {
		int dot = name.indexOf('.');
		LinkedUnit unit;
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			name = name.substring(dot+1);
			unit = declaringUnit.getReachableUnit(unitName);
		} else {
			unit = declaringUnit;
		}
		if(unit == null)
			return null;
		for(FunctionSection func : unit.functions) {
			if(func.name.equals(name) && predicate.matches(func.argumentTypes, args, conversions))
				return func;
		}
		return null;
	}
	
	/** Returns the only function with parameters that exactly match <code>args</code> */
	FunctionSection getFunctionStrict(String name, VarType[] args) {
		return getFunction(name, args, null, FunctionSection::argsMatch0c);
	}
	
	/** Returns the number of functions that can be called using <code>args</code> with 1 implicit cast maximum */
	int countMatchingFunction1c(String name, VarType[] args, ConversionTable conversions) {
		return countMatchingFunctions(name, args, conversions, FunctionSection::argsMatch1c);
	}
	
	/** Returns the first function that can be called used the given arguments with 1 implicit cast maximum */
	FunctionSection getFunction1c(String name, VarType[] args, ConversionTable conversions) {
		return getFunction(name, args, conversions, FunctionSection::argsMatch1c);
	}
	
	int countMatchingFunctionXc(String name, VarType[] args, ConversionTable conversions) {
		return countMatchingFunctions(name, args, conversions, FunctionSection::argsMatchXc);
	}
	
	FunctionSection getFunctionXc(String name, VarType[] args, ConversionTable conversions) {
		return getFunction(name, args, conversions, FunctionSection::argsMatchXc);
	}
}