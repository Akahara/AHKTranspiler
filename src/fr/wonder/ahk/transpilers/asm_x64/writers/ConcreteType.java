package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.transpilers.common_x64.MemSize;

public class ConcreteType {
	
	/** The size in bytes of this type */
	public final int size;
	private final VariablePrototype[] members;
	
	ConcreteType(VariablePrototype[] members) {
		this.members = members;
		this.size = members.length * MemSize.POINTER_SIZE;
	}
	
	public VarType getMemberType(String member) {
		for(var m : members)
			if(m.getName().equals(member))
				return m.getType();
		return Invalids.TYPE;
	}
	
	public int getOffset(String member) {
		for(int i = 0; i < members.length; i++)
			if(members[i].getName().equals(member))
				return i * MemSize.POINTER_SIZE;
		throw new IllegalStateException("Unknown struct member " + member);
	}
	
}
