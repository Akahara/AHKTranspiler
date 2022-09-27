package fr.wonder.ahk.compiled.units;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import fr.wonder.ahk.compiled.expressions.types.VarUserDefinedType;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;

public class ExternalAccesses<T extends VarUserDefinedType> {
	
	public static class ExternalTypeAccess<T extends VarUserDefinedType> {
		
		public final T typeInstance;
		public final SourceElement firstOccurrence;
		public int occurrenceCount;
		
		ExternalTypeAccess(T typeInstance, Token firstOccurence) {
			this.typeInstance = typeInstance;
			this.firstOccurrence = firstOccurence;
			this.occurrenceCount = 1;
		}
		
	}
	
	private final Map<String, ExternalTypeAccess<T>> accesses = new HashMap<>();
	private final Function<String, T> constructor;
	private final Function<Token, String> nameExtractor;
	
	public ExternalAccesses(Function<String, T> constructor, Function<Token, String> nameExtractor) {
		this.constructor = constructor;
		this.nameExtractor = nameExtractor;
	}
	
	public ExternalAccesses(Function<String, T> constructor) {
		this(constructor, t -> t.text);
	}
	
	/**
	 * @param token a token which type must be an {@link Tokens#isExternalDeclaration(TokenBase) external declaration}.
	 */
	public T getType(Token token) {
		String name = nameExtractor.apply(token);
		ExternalTypeAccess<T> knownType = accesses.get(name);
		if(knownType != null) {
			knownType.occurrenceCount++;
			return knownType.typeInstance;
		}
		
		T type = constructor.apply(name);
		accesses.put(name, new ExternalTypeAccess<T>(type, token));
		return type;
	}
	
//	private static VarUserDefinedType createTypeFromToken(Token token) {
//		if(token.base == TokenBase.VAR_STRUCT)
//			return new VarStructType(token.text);
//		else if(token.base == TokenBase.VAR_ENUM)
//			return new VarEnumType(Tokens.extractEnumName(token));
//		throw new IllegalArgumentException("Not a user defined type: " + token.toString());
//	}

	public Collection<ExternalTypeAccess<T>> getAccesses() {
		return accesses.values();
	}
	
}
