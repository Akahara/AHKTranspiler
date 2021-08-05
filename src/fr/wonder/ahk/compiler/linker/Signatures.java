package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.AliasPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;

public class Signatures {

	public static Signature scopedVariableSignature(String varName) {
		return new Signature(VarAccess.INNER_UNIT, varName, varName);
	}
	
	public static Signature of(FunctionSection func) {
		return new Signature(
				func.unit.fullBase,
				func.name,
				func.name + "_" + func.getFunctionType().getSignature());
	}
	
	public static Signature of(VariableDeclaration var) {
		return new Signature(
				var.unit.fullBase,
				var.name,
				var.name);
	}
	
	public static Signature of(VariableDeclaration member, StructSection struct) {
		return new Signature(
				member.unit.fullBase + '@' + struct.name,
				member.name,
				member.name);
	}

	public static Signature of(StructConstructor constructor) {
		return new Signature(
				constructor.struct.unit.fullBase + "@" + constructor.struct.name,
				"constructor",
				"constructor_" + constructor.getConstructorSignature());
	}
	
	public static Signature of(StructSection structure) {
		return new Signature(
				structure.unit.fullBase,
				structure.name,
				structure.name);
	}
	
	public static Signature of(FunctionArgument arg) {
		return new Signature(
				VarAccess.INNER_UNIT,
				arg.name,
				"arg_" + arg.name);
	}
	
	public static Signature of(UnitPrototype unitProto) {
		return new Signature(
				unitProto.fullBase,
				unitProto.base,
				unitProto.fullBase);
	}

	public static Signature of(AliasPrototype alias, String unitFullBase) {
		return new Signature(
				unitFullBase,
				alias.text,
				"alias_"+alias.text+"_"+alias.resolvedType.getSignature());
	}

}
