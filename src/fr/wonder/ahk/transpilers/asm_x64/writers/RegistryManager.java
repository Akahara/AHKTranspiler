package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.Modifier;
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
		if(!var.getSignature().declaringUnit.equals(writer.unit.fullBase))
			return getGlobalRegistry(var);
		return getLocalRegistry(var);
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
		
		return getUnitRegistry(var.getSignature().declaringUnit) + "_" + getLocalRegistry(var);
	}
	
	public static String getLocalRegistry(VarAccess var) {
		if(var instanceof VariablePrototype)
			return ((VariablePrototype) var).getName();
		if(var instanceof FunctionPrototype) {
			FunctionPrototype f = (FunctionPrototype) var;
			return f.getName() + "_" + f.getType().getSignature();
		}
		
		throw new IllegalArgumentException("Unimplemented registry " + var.getClass());
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
	
}
