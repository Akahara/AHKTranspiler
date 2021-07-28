package fr.wonder.ahk.compiled.units;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Prototypes;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.commons.types.Triplet;

public class Unit {
	
	public final UnitSource source;
	
	public final String base;
	public final String name;
	public final String fullBase;
	public final String[] importations;
	
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	public StructSection[] structures;
	public Map<String, Alias> aliases = new HashMap<>();
	
	public UnitCompilationState compilationState = UnitCompilationState.FRESH;
	
	/**
	 * List of structure types used through variable declarations,
	 * when a declaration is read the structure type is not known
	 * (we only know its name) so a single instance is registered
	 * and reused wherever the same struct name is used. This instance
	 * will be linked to its struct declaration by the linker.<br>
	 * <br>
	 * Triplets in this list contain the type instance, the first
	 * token that required it and the number of references
	 * to it (for error logging purpose).
	 */
	public final Map<String, Triplet<VarStructType, Token, Integer>> usedStructTypes = new HashMap<>();
	
	/** Set by {@link Prototypes#buildPrototype(Unit)} */
	public UnitPrototype prototype;
	
	public Unit(UnitSource source, String base, String name, int declarationStart,
			int declarationStop, String[] importations) {
		this.source = source;
		this.base = base;
		this.name = name;
		this.fullBase = base+'.'+name;
		this.importations = importations;
	}
	
	@Override
	public String toString() {
		return fullBase;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Unit && fullBase.equals(((Unit) other).fullBase);
	}

	public VarType getStructOrAliasType(Token token) {
		if(token.base != TokenBase.VAR_UNIT)
			throw new IllegalArgumentException("Not a struct token");
		
		Alias knownAlias = aliases.get(token.text);
		if(knownAlias != null)
			return knownAlias.functionType;
		
		return getStructType(token);
	}
	
	public VarStructType getStructType(Token token) {
		var knownType = usedStructTypes.get(token.text);
		if(knownType != null) {
			knownType.c++;
			return knownType.a;
		}
		
		VarStructType type = new VarStructType(token.text);
		usedStructTypes.put(token.text, new Triplet<>(type, token, 1));
		return type;
	}
	
}
