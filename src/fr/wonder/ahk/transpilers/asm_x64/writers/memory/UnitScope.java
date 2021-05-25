package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class UnitScope implements Scope {
	
	private final Unit unit;
	
	public UnitScope(Unit unit) {
		this.unit = unit;
	}
	
	@Override
	public Scope getParent() {
		throw new IllegalStateException("Unit scopes do not have parents");
	}
	
	@Override
	public Address declareVariable(VariableDeclaration var) {
		throw new IllegalStateException("Unit scopes cannot hold variables");
	}
	
	@Override
	public Address getVarAddress(VarAccess var) {
		if(!isReachable(var))
			throw new IllegalStateException("Value declaration missed! " + var);
		if(var instanceof FunctionArgument)
			throw new IllegalStateException("A unit scope tried to access a function argument! " + var);
		return new MemAddress(new LabelAddress(
				var.getSignature().declaringUnit.equals(unit.fullBase) ?
					UnitWriter.getLocalRegistry((Prototype<?>) var) :
					UnitWriter.getGlobalRegistry((Prototype<?>) var)));
	}
	
	private boolean isReachable(VarAccess var) {
//		if(Utils.arrayContains(unit.variables, var))
//			return true;
//		for(UnitImportation imported : unit.importations) {
//			if(Utils.arrayContains(imported.unit.variables, var))
//				return true;
//		}
//		return false;
		return true; // FIX check if variable is reachable?
	}
	
	@Override
	public int getSize() {
		throw new IllegalStateException("Unit scopes do not have sizes");
	}
	
	@Override
	public int getTotalSize() {
		throw new IllegalStateException("Unit scopes do not have sizes");
	}
	
	@Override
	public void addStackOffset(int offset) {
		throw new IllegalStateException("Unit scopes cannot be used to call functions");
	}
	
}
