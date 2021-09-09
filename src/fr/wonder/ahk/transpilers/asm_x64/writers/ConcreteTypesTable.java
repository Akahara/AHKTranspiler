package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.commons.exceptions.UnreachableException;

/** A single instance of this class is necessary */
public class ConcreteTypesTable {
	
	private final Map<String, ConcreteType> concreteTypes = new HashMap<>();
	
	public ConcreteType getConcreteType(VarType structType) {
		StructPrototype structure;
		if(structType instanceof VarStructType)
			structure = ((VarStructType) structType).structure;
		else
			throw new UnreachableException("Invalid type given:" + structType.getClass());
		Signature structSignature = structure.getSignature();
		String key = structSignature.declaringUnit + '@' + structSignature.name;
		return concreteTypes.computeIfAbsent(key, k -> computeConcreteType(structure));
	}
	
	public ConcreteType getConcreteType(StructSection structure) {
		Signature structSignature = structure.getPrototype().getSignature();
		String key = structSignature.declaringUnit + '@' + structSignature.name;
		return concreteTypes.computeIfAbsent(key, k -> computeConcreteType(structure.getPrototype()));
	
	}
	
	public static ConcreteType computeConcreteType(StructPrototype prototype) {
		List<VariablePrototype> members = Arrays.asList(prototype.members);
		members.sort((m1, m2) -> m1.getName().compareTo(m2.getName()));
		return new ConcreteType(members.toArray(VariablePrototype[]::new));
	}
	
}
