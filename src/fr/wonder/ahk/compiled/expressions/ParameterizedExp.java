package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintTypeParameter;
import fr.wonder.ahk.utils.Utils;

public class ParameterizedExp extends Expression {
	
	public final VarType[] genericBindings;
	public BlueprintTypeParameter[] typesParameters;
	
	public ParameterizedExp(SourceReference sourceRef, Expression target, VarType[] genericBindings) {
		super(sourceRef, target);
		this.genericBindings = genericBindings;
	}
	
	public Expression getTarget() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return getTarget() + "<[" + Utils.toString(genericBindings) + "]>";
	}
	
}
