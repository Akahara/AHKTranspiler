package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class SectionScope implements Scope {
	
	private final Scope parent;
	private final List<ValueDeclaration> vars = new ArrayList<>();
	
	private int size;
	
	private int funcCallOffset = 0;
	
	SectionScope(Scope parent) {
		this.parent = parent;
	}
	
	@Override
	public Scope getParent() {
		return parent;
	}
	
	@Override
	public Address declareVariable(VariableDeclaration var) {
		vars.add(var);
		size += MemSize.getPointerSize(var.getType()).bytes;
		return getVarAddress(var.getPrototype());
	}
	
	@Override
	public Address getVarAddress(VarAccess var) {
		int loc = funcCallOffset;
		for(ValueDeclaration v : vars) {
			if(v == var)
				return new MemAddress(Register.RSP, loc);
			else
				loc += MemSize.getPointerSize(v.getType()).bytes;
		}
		Address vl = parent.getVarAddress(var);
		// FIX check if vl.index is REG_RSP
		if(vl instanceof MemAddress && ((MemAddress) vl).base == Register.RSP)
			return ((MemAddress) vl).addOffset(loc);
		return vl;
	}
	
	@Override
	public int getSize() {
		return size;
	}
	
	@Override
	public int getTotalSize() {
		return size + parent.getTotalSize();
	}
	
	@Override
	public void addStackOffset(int offset) {
		this.funcCallOffset += offset;
	}
	
}
