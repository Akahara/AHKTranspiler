package fr.wonder.ahk.compiled.units;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.units.ExternalTypeAccess.ParametrizedAccess;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiler.tokens.Token;

public class ExternalAccesses<T> {
	
	private final Map<String, ExternalTypeAccess<T>> accesses = new HashMap<>();
	private final Function<String, T> constructor;
	
	public ExternalAccesses(Function<String, T> constructor) {
		this.constructor = constructor;
	}
	
	public T getType(Token token) {
		var knownType = accesses.get(token.text);
		if(knownType != null) {
			knownType.occurrenceCount++;
			return knownType.typeInstance;
		}
		
		T type = constructor.apply(token.text);
		accesses.put(token.text, new ExternalTypeAccess<>(type, token));
		return type;
	}
	
	public void addParametrizedInstance(VarBoundStructType bound, GenericContext context, Token token) {
		accesses.get(bound.name).parametrizedInstances.add(new ParametrizedAccess(bound, token, context));
	}

	public Collection<ExternalTypeAccess<T>> getAccesses() {
		return accesses.values();
	}
	
}
