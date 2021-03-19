package fr.wonder.ahk.compiled.expressions.types;

import java.util.Arrays;

import fr.wonder.ahk.utils.Utils;

public class VarCompositeType extends VarType {
	
	public final VarType[] types;
	
	public VarCompositeType(VarType[] types) {
		this.types = types;
	}
	
	@Override
	public String getName() {
		return "(" + Utils.toString(types) + ")";
	}

	@Override
	public String getSignature() {
		String signature = "C" + types.length;
		for(VarType t : types)
			signature += t.getSignature();
		return signature;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarCompositeType && Arrays.equals(types, ((VarCompositeType) o).types);
	}

}
