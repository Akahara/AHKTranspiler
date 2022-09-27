package fr.wonder.ahk.compiled.units;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarEnumType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiled.units.sections.EnumSection;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Prototypes;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.utils.Assertions;

public class Unit {
	
	public final UnitSource source;
	
	public final String base;
	public final String name;
	public final String fullBase;
	public final String[] importations;
	
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	public StructSection[] structures;
	public EnumSection[] enums;
	
	public List<SimpleLambda> lambdas = new ArrayList<>();
	
	public Alias[] accessibleAliases;
	public final int declaredAliasCount;
	
	/**
	 * List of user defined types used through variable declarations,
	 * when a declaration is read the concrete type is not known
	 * (we only know its name) so a single instance is registered
	 * and reused wherever the same name is used. This instance
	 * will be linked to its concrete declaration by the linker.
	 */
	public final ExternalAccesses<VarStructType> externalStructureTypes = new ExternalAccesses<>(VarStructType::new);
	public final ExternalAccesses<VarEnumType> externalEnumTypes = new ExternalAccesses<>(VarEnumType::new, Tokens::extractEnumName);
	
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

	public VarType getAliasOrStructType(Token token) {
		if(!Tokens.isExternalDeclaration(token.base) && token.base != TokenBase.VAR_ALIAS) // technically VAR_ALIAS==VAR_STRUCT and VAR_STRUCT is an external declaration
			throw new IllegalArgumentException("Not an external declaration token");
		
		if(token.base == TokenBase.VAR_ALIAS) {
			VarType aliasedType = getAliasType(token);
			if(aliasedType != null)
				return aliasedType;
		}
		
		return externalStructureTypes.getType(token);
	}
	
	public VarEnumType getEnumType(Token token) {
		Assertions.assertTrue(token.base == TokenBase.VAR_ENUM || token.base == TokenBase.VAR_ENUM_NAME);
		return externalEnumTypes.getType(token);
	}

	public VarType getAliasType(Token token) {
		Assertions.assertTrue(token.base == TokenBase.VAR_ALIAS, "Not an alias token");
		
		for(Alias alias : accessibleAliases) {
			if(alias.text.equals(token.text))
				return alias.resolvedType;
		}
		return null;
	}

}
