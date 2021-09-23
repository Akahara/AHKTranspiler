package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiled.units.sections.Blueprint;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Prototypes;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;

public class Unit {
	
	public final UnitSource source;
	
	public final String base;
	public final String name;
	public final String fullBase;
	public final String[] importations;
	
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	public StructSection[] structures;
	public Blueprint[] blueprints;
	
	public Alias[] accessibleAliases;
	public final int declaredAliasCount;
	
	/**
	 * List of structure types used through variable declarations,
	 * when a declaration is read the structure type is not known
	 * (we only know its name) so a single instance is registered
	 * and reused wherever the same struct name is used. This instance
	 * will be linked to its struct declaration by the linker.
	 */
	public final ExternalAccesses<VarStructType> usedStructTypes = new ExternalAccesses<>(VarStructType::new);
	public final ExternalAccesses<BlueprintRef> usedBlueprintTypes = new ExternalAccesses<>(BlueprintRef::new);
	
	/** Set by {@link Prototypes#buildPrototype(Unit)} */
	public UnitPrototype prototype;
	
	public Unit(UnitSource source, String base, String name, String[] importations, int declaredAliasCount) {
		this.source = source;
		this.base = base;
		this.name = name;
		this.fullBase = base+'.'+name;
		this.importations = importations;
		this.declaredAliasCount = declaredAliasCount;
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
		if(token.base != TokenBase.VAR_STRUCT)
			throw new IllegalArgumentException("Not a struct token");
		
		for(Alias alias : accessibleAliases) {
			if(alias.text.equals(token.text))
				return alias.resolvedType;
		}
		
		return usedStructTypes.getType(token);
	}

}
