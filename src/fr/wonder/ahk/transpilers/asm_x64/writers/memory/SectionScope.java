package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;

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
	public VarLocation declareVariable(VariableDeclaration var) {
		vars.add(var);
		size += var.getType().getSize();
		return getVarLocation(var);
	}
	
	@Override
	public VarLocation getVarLocation(ValueDeclaration var) {
		int loc = funcCallOffset;
		for(ValueDeclaration v : vars) {
			if(v == var)
				return new MemoryLoc(VarLocation.REG_RSP, loc);
			else
				loc += v.getType().getSize();
		}
		VarLocation vl = parent.getVarLocation(var);
		// FIX check if vl.index is REG_RSP
		if(vl instanceof MemoryLoc && ((MemoryLoc) vl).base == VarLocation.REG_RSP)
			((MemoryLoc) vl).offset += loc;
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
