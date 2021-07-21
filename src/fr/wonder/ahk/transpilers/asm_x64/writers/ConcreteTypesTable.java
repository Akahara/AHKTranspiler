package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;

/** A single instance of this class is necessary */
public class ConcreteTypesTable {
	
	private final Map<String, ConcreteType> concreteTypes = new HashMap<>();
	
	public ConcreteType getConcreteType(VarStructType structType) {
		Signature structSignature = structType.structure.getSignature();
		String key = structSignature.declaringUnit + '@' + structSignature.name;
		return concreteTypes.computeIfAbsent(key, k -> computeConcreteType(structType));
	}
	
	public static ConcreteType computeConcreteType(VarStructType structType) {
		StructPrototype prototype = structType.structure;
		List<VariablePrototype> members = Arrays.asList(prototype.members);
		members.sort((m1, m2) -> m1.getName().compareTo(m2.getName()));
		return new ConcreteType(members.toArray(VariablePrototype[]::new));
	}
	
}
