package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.commons.exceptions.ErrorWrapper;

interface Scope {
	
	/** Creates and return a new InnerScope, inner to this one */
	Scope innerScope();
	/** Returns the scope that immediately encloses this one, UnitScopes do not have outer scopes */
	Scope outerScope();
	/** Returns the unit scope that contains this scope (the last parent of this scope) */
	UnitScope getUnitScope();
	
	/**
	 * Returns the accessible variable or function with the given name if any.
	 * <p>
	 * {@code name} may be the complete variable name (ie {@code someVar}) or
	 * a unit name and the variable name in that unit (ie {@code SomeUnit.someVar})
	 * 
	 * @param name    the name of the variable
	 * @param srcElem the source element from which to get an error message if the
	 *                query fails
	 * @param errors  the error wrapper
	 * @return the variable if it exists, an invalid otherwise
	 */
	VarAccess getVariable(String name, SourceElement srcElem, ErrorWrapper errors);
	
	/**
	 * Declares a variable in this scope.
	 * 
	 * @param var     the variable to declare
	 * @param srcElem the source element from which to get an error message if the
	 *                declaration fails
	 * @param errors  the error wrapper
	 */
	void registerVariable(VarAccess var, SourceElement srcElem, ErrorWrapper errors);
	
}