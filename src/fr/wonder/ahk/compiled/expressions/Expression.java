package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.ExpressionHolder;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.annotations.NonNull;
import fr.wonder.commons.exceptions.ErrorWrapper;

public abstract class Expression extends SourceObject implements ExpressionHolder {
	
	public final Expression[] expressions;
	
	/** Set by the linker using {@link #computeValueType(TypesTable, ErrorWrapper)} before linking statements */
	protected VarType type;
	
	public Expression(UnitSource source, int sourceStart, int sourceStop, Expression... expressions) {
		super(source, sourceStart, sourceStop);
		this.expressions = expressions;
	}
	
	public Expression(UnitSource source, int sourceStart, int sourceStop, Expression e, Expression[] expressions) {
		super(source, sourceStart, sourceStop);
		Expression[] exps = new Expression[1 + expressions.length];
		exps[0] = e;
		for(int i = 0; i < expressions.length; i++)
			exps[i+1] = expressions[i];
		this.expressions = exps;
	}
	
	/** Implementations must add an error to {@code errors} if the returned value is {@code null} */
	protected abstract VarType getValueType(TypesTable typesTable, ErrorWrapper errors);
	
	public void computeValueType(TypesTable typesTable, ErrorWrapper errors) {
		this.type = getValueType(typesTable, errors);
		if(this.type == null)
			this.type = Invalids.TYPE;
	}
	
	public VarType getType() {
		return type;
	}
	
	public abstract String toString();
	
	@NonNull
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
}
