package fr.wonder.ahk.compiled.units.sections;

import java.util.function.BiFunction;

import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;

public class Modifier {
	
	public static final String NATIVE = "native";
	
	public final String name;
	private final LiteralExp<?>[] arguments;
	/** set by {@link #validateArgs(ValueDeclaration, BiFunction[])}, null if not validated */
	public ModifierSyntax syntax;
	
	public Modifier(String name, LiteralExp<?>... arguments) {
		this.name = name;
		this.arguments = arguments;
	}
	
	public int getArgsCount() {
		return arguments.length;
	}
	
	public LiteralExp<?> getArg(int pos){
		return arguments[pos];
	}
	
	private void assertMod(int pos, Class<? extends LiteralExp<?>> clazz) {
		if(pos >= arguments.length)
			throw new IllegalArgumentException("Modifier only has " + arguments.length + " values");
		if(arguments[pos].getClass() != clazz)
			throw new IllegalArgumentException("Modifier value at index " + pos + " is not a " + clazz.getSimpleName());
	}
	
	public String getStr(int pos) {
		assertMod(pos, StrLiteral.class);
		return ((StrLiteral) arguments[pos]).value;
	}
	
	public long getInt(int pos) {
		assertMod(pos, IntLiteral.class);
		return ((IntLiteral) arguments[pos]).value;
	}
	
	public boolean getBool(int pos) {
		assertMod(pos, BoolLiteral.class);
		return ((BoolLiteral) arguments[pos]).value;
	}
	
	public float getFloat(int pos) {
		assertMod(pos, FloatLiteral.class);
		return ((FloatLiteral) arguments[pos]).value;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ValueDeclaration> boolean validateArgs(T func, BiFunction<T, Modifier, ModifierSyntax> syntax) {
		return validateArgs(func, new BiFunction[] { syntax });
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ValueDeclaration> boolean validateArgs(T func) {
		return validateArgs(func, new BiFunction[0]);
	}
	
	public <T extends ValueDeclaration> boolean validateArgs(T func, BiFunction<T, Modifier, ModifierSyntax>[] syntaxes) {
		for(var syntax : syntaxes) {
			ModifierSyntax s = syntax.apply(func, this);
			if(s != null) {
				this.syntax = s;
				return true;
			}
		}
		return false;
	}
	
}
