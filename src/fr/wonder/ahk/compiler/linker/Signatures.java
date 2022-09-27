package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.AliasPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.EnumSection;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;

public class Signatures {

	public static Signature scopedVariableSignature(String varName) {
		return new Signature(VarAccess.INNER_UNIT, varName, varName);
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
	
	public static Signature of(FunctionSection func) {
		return new Signature(
				func.unit.fullBase,
				func.name,
				func.name + "_" + func.getFunctionType().getSignature());
	}
	
	public static Signature of(FunctionArgument arg) {
		return new Signature(
				VarAccess.INNER_UNIT,
				arg.name,
				"arg_" + arg.name);
	}
	
	public static Signature of(VariableDeclaration var) {
		return new Signature(
				var.unit.fullBase,
				var.name,
				var.name);
	}
	
	private static String structSig(StructSection structure) {
		return structure.unit.fullBase + '@' + structure.name;
	}
	
	public static Signature of(StructSection structure) {
		return new Signature(
				structure.unit.fullBase,
				structure.name,
				structSig(structure));
	}
	
	public static Signature of(VariableDeclaration member, StructSection struct) {
		return new Signature(
				structSig(struct),
				member.name,
				member.name);
	}

	public static Signature of(StructConstructor constructor) {
		return new Signature(
				structSig(constructor.struct),
				"constructor",
				"constructor_" + constructor.getConstructorSignature());
	}

	public static Signature of(EnumSection enumeration) {
		return new Signature(
				enumeration.unit.fullBase,
				enumeration.name,
				enumeration.name);
	}
	
	public static Signature of(OverloadedOperatorPrototype operator, StructSection structure) {
		String computedSignature = "operator_"+operator.loType+"_"+operator.operator+"_"+
				operator.roType+"_"+operator.resultType;
		return new Signature(
				structSig(structure),
				computedSignature,
				computedSignature);
	}
	
	public static Signature ofClosureArgument(VarAccess originalVariable) {
		String computedSignature = "closure_arg_" + originalVariable.getSignature().computedSignature;
		return new Signature(
				VarAccess.INNER_UNIT,
				originalVariable.getSignature().name,
				computedSignature);
	}
	
}
