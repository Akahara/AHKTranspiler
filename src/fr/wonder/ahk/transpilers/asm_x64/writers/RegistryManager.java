package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintImplementation;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;

public class RegistryManager {
	
	private final UnitWriter writer;
	
	public RegistryManager(UnitWriter writer) {
		this.writer = writer;
	}
	
	/**
	 * Returns the registry (the label) of the variable or function denoted by {@code var}
	 * if {@code var} is a function the label to its <b>closure</b> is returned, <b>not</b>
	 * its own label.
	 */
	public String getRegistry(VarAccess var) {
		if(var.isLocallyScoped())
			throw new IllegalStateException("Cannot globally access a scoped variable");
		if(var instanceof FunctionPrototype)
			return getClosureRegistry((FunctionPrototype) var);
		return getGlobalRegistry(var);
	}
	
	public static String getFunctionRegistry(FunctionPrototype func) {
		return getGlobalRegistry(func);
	}
	
	public static String getUnitRegistry(String unitFullBase) {
		return unitFullBase.replaceAll("\\.", "_");
	}
	
	public static String getUnitInitFunctionRegistry(Unit unit) {
		return getUnitRegistry(unit.fullBase) + "_init";
	}
	
	public static String getGlobalRegistry(VarAccess var) {
		if(var instanceof FunctionPrototype && ((FunctionPrototype) var).getModifiers().hasModifier(Modifier.NATIVE))
			return ((FunctionPrototype) var).getModifiers().getModifier(NativeModifier.class).nativeRef;
		
		String unitRegistry = getUnitRegistry(var.getSignature().declaringUnit);
		String localRegistry;
		
		if(var instanceof VariablePrototype) {
			localRegistry = ((VariablePrototype) var).getName();
		} else if(var instanceof FunctionPrototype) {
			FunctionPrototype f = (FunctionPrototype) var;
			localRegistry = f.signature.computedSignature;
		} else {
			throw new IllegalArgumentException("Unimplemented registry " + var.getClass());
		}
		
		
		return unitRegistry + "_" + localRegistry;
	}
	
	public static String getClosureRegistry(FunctionPrototype func) {
		return getGlobalRegistry(func) + "@closure";
	}
	
	public String getStructNullRegistry(StructPrototype struct) {
		String registry = getUnitRegistry(struct.getSignature().declaringUnit) + "@" 
				+ struct.getSignature().name + "_null";
		if(!struct.getSignature().declaringUnit.equals(writer.unit.fullBase))
			writer.requireExternLabel(registry);
		return registry;
	}
	
	public static String getOperationClosureRegistry(NativeOperation op) {
		return "closure_op_" + op.loType + op.operator.name() + op.roType;
	}
	
	public static String getStructBlueprintImplRegistry(BlueprintImplementation bpImpl) {
		return getUnitRegistry(bpImpl.structure.getSignature().declaringUnit) + "@"
				+ bpImpl.structure.getName() + "_bpimpl_" + bpImpl.bpRef.name;
	}
	
	public static String getLambdaRegistry(Unit unit, SimpleLambda lambda) {
		int idx = unit.lambdas.indexOf(lambda);
		if(idx == -1)
			throw new IllegalArgumentException("Lambda does not exist in given unit");
		return "lambda_@" + idx;
	}

}
