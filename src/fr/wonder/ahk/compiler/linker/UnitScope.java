package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.commons.types.Tuple;

class UnitScope implements Scope {
	
	private final UnitPrototype unit;
	private final UnitPrototype[] importedUnits;
	
	UnitScope(UnitPrototype unit, UnitPrototype[] units) {
		this.unit = unit;
		this.importedUnits = units;
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
	
	private Tuple<UnitPrototype, String> getUnitFromVarName(String name) {
		int dot = name.indexOf('.');
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			String varName = name.substring(dot+1);
			for(UnitPrototype proto : importedUnits) {
				if(proto.base.equals(unitName))
					return new Tuple<>(proto, varName);
			}
			throw new IllegalStateException("Unknown unit " + unitName);
		} else {
			return new Tuple<>(this.unit, name);
		}
	}
	
	@Override
	public VarAccess getVariable(String name) {
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name);
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		if(unit == null)
			return null;
		// note that no function and variable can have the same name
		VarAccess varProto = unit.getVariable(name);
		if(varProto != null)
			return varProto;
		// also the linker will do the job of searching for the right function, no need to do that here
		// it only requires a function to be found to begin its work
		FunctionPrototype[] functions = unit.getFunctions(name);
		if(functions.length != 0)
			return functions[0];
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
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name);
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		if(unit == null)
			return 0;
		int count = 0;
		for(FunctionPrototype func : unit.functions) {
			if(func.name.equals(name) && predicate.matches(func.functionType.arguments, args, conversions))
				count++;
		}
		return count;
	}
	
	private FunctionPrototype getFunction(String name, VarType[] args, ConversionTable conversions, ArgumentsMatchPredicate predicate) {
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name);
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		if(unit == null)
			return null;
		for(FunctionPrototype func : unit.functions) {
			if(func.name.equals(name) && predicate.matches(func.functionType.arguments, args, conversions))
				return func;
		}
		return null;
	}
	
	/** Returns the only function with parameters that exactly match <code>args</code> */
	FunctionPrototype getFunctionStrict(String name, VarType[] args) {
		return getFunction(name, args, null, FunctionSection::argsMatch0c);
	}
	
	/** Returns the number of functions that can be called using <code>args</code> with 1 implicit cast maximum */
	int countMatchingFunction1c(String name, VarType[] args, ConversionTable conversions) {
		return countMatchingFunctions(name, args, conversions, FunctionSection::argsMatch1c);
	}
	
	/** Returns the first function that can be called used the given arguments with 1 implicit cast maximum */
	FunctionPrototype getFunction1c(String name, VarType[] args, ConversionTable conversions) {
		return getFunction(name, args, conversions, FunctionSection::argsMatch1c);
	}
	
	int countMatchingFunctionXc(String name, VarType[] args, ConversionTable conversions) {
		return countMatchingFunctions(name, args, conversions, FunctionSection::argsMatchXc);
	}
	
	FunctionPrototype getFunctionXc(String name, VarType[] args, ConversionTable conversions) {
		return getFunction(name, args, conversions, FunctionSection::argsMatchXc);
	}
}