package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.utils.Utils;

public class VarBoundStructType extends VarStructType {
	
	public final VarType[] boundTypes;
	
	public VarBoundStructType(String name, VarType[] genericBindings) {
		super(name);
		this.boundTypes = genericBindings;
	}
	
	@Override
	public VarType[] getSubTypes() {
		return boundTypes;
	}
	
	@Override
	public String toString() {
		return structure.getName() + "<" + Utils.toString(boundTypes) + ">";
	}

}
