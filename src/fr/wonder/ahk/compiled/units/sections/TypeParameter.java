package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.utils.Utils;

public class TypeParameter {
	
	public static final BlueprintRef[] NO_TYPE_RESTRICTION = new BlueprintRef[0];
	
	public final char name;
	public final BlueprintRef[] typeRestrictions;
	
	public final VarGenericType typeInstance;
	
	public TypeParameter(char name, BlueprintRef[] typeRestrictions) {
		this.name = name;
		this.typeRestrictions = typeRestrictions;
		this.typeInstance = new VarGenericType(this);
	}

	@Override
	public String toString() {
		return name + (typeRestrictions.length == 0 ? "" : ":" + Utils.toString(typeRestrictions));
	}
	
}
