package fr.wonder.ahk.compiled.expressions.types;

import java.util.Arrays;

public class VarCompositeType extends VarType {
	
	public final String[] names;
	public final VarType[] types;
	
	public VarCompositeType(String[] names, VarType[] types) {
		this.names = names;
		this.types = types;
	}
	
	@Override
	public String getName() {
		String name = "(";
		for(int i = 0; i < names.length; i++) {
			name += types[i] + " " + names[i];
			if(i != names.length-1)
				name += ", ";
		}
		return name + ")";
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
