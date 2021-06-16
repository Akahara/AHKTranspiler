package fr.wonder.ahk.transpilers.asm_x64.units.modifiers;

import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.ModifierSyntax;

public class NativeModifier implements ModifierSyntax {
	
	public final String nativeRef;
	
	public NativeModifier(String nativeRef) {
		this.nativeRef = nativeRef;
	}
	
	public static NativeModifier parseModifier(Modifier mod) {
		if(mod.getArgsCount() == 1 && mod.getArg(0) instanceof StrLiteral)
			return new NativeModifier(mod.getStr(0));
		else
			return null;
	}
	
}
