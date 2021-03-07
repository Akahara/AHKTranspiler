package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiler.Unit;

public interface Prototype<T extends ValueDeclaration> {
	
	public String getDeclaringUnit();
	public T getAccess(Unit unit);
	
}
