package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.UnitImportation;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.utils.Utils;

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
	public VarLocation declareVariable(VariableDeclaration var) {
		throw new IllegalStateException("Unit scopes cannot hold variables");
	}
	
	@Override
	public VarLocation getVarLocation(ValueDeclaration var) {
		if(!isReachable(var))
			throw new IllegalStateException("Value declaration missed! " + var);
		return new MemoryLoc(var.getUnit() == unit ?
				UnitWriter.getLocalRegistry(var) :
				UnitWriter.getGlobalRegistry(var));
	}
	
	private boolean isReachable(ValueDeclaration var) {
		if(Utils.arrayContains(unit.variables, var))
			return true;
		for(UnitImportation imported : unit.importations) {
			if(Utils.arrayContains(imported.unit.variables, var))
				return true;
		}
		return false;
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
