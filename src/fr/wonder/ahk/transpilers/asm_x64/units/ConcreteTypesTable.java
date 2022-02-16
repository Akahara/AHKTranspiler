package fr.wonder.ahk.transpilers.asm_x64.units;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;

/** A single instance of this class is necessary */
public class ConcreteTypesTable {
	
	private final Map<String, ConcreteType> concreteTypes = new HashMap<>();
	
	public ConcreteType getConcreteType(StructPrototype structure) {
		Signature structSignature = structure.getSignature();
		String key = structSignature.declaringUnit + '@' + structSignature.name;
		return concreteTypes.computeIfAbsent(key, k -> computeConcreteType(structure));
	}
	
	private static ConcreteType computeConcreteType(StructPrototype prototype) {
		List<VariablePrototype> members = Arrays.asList(prototype.members);
		members.sort((m1, m2) -> m1.getName().compareTo(m2.getName()));
		return new ConcreteType(members.toArray(VariablePrototype[]::new));
	}
	
}
