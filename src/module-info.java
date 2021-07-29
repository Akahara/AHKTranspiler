module AHKTranspiler {
	exports fr.wonder.ahk.compiler.tokens;
	exports fr.wonder.ahk.transpilers.asm_x64.natives;
	exports fr.wonder.ahk.transpilers.asm_x64.writers;
	exports fr.wonder.ahk.utils;
	exports fr.wonder.ahk.compiled.expressions;
	exports fr.wonder.ahk.transpilers.asm_x64.units.modifiers;
	exports fr.wonder.ahk.compiler.types;
	exports fr.wonder.ahk;
	exports fr.wonder.ahk.compiled;
	exports fr.wonder.ahk.compiled.units;
	exports fr.wonder.ahk.compiled.statements;
	exports fr.wonder.ahk.compiler.linker;
	exports fr.wonder.ahk.transpilers;
//	exports fr.wonder.ahk.transpilers.python;
	exports fr.wonder.ahk.compiler;
	exports fr.wonder.ahk.compiled.expressions.types;
	exports fr.wonder.ahk.transpilers.asm_x64;
	exports fr.wonder.ahk.compiled.units.sections;
	exports fr.wonder.ahk.handles;
	exports fr.wonder.ahk.compiled.units.prototypes;
	exports fr.wonder.ahk.transpilers.common_x64;
	exports fr.wonder.ahk.transpilers.common_x64.addresses;
	exports fr.wonder.ahk.transpilers.common_x64.declarations;
	exports fr.wonder.ahk.transpilers.common_x64.instructions;

	requires transitive fr.wonder.commons;
	requires transitive fr.wonder.commons.systems;
}