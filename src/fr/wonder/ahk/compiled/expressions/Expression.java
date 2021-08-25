package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.annotations.NonNull;
import fr.wonder.commons.exceptions.ErrorWrapper;

public abstract class Expression implements ExpressionHolder, SourceElement {
	
	public final SourceReference sourceRef;
	public final Expression[] expressions;
	
	/** Set by the linker using {@link #computeValueType(TypesTable, ErrorWrapper)} before linking statements */
	protected VarType type;
	
	public Expression(SourceReference sourceRef, Expression... expressions) {
		this.sourceRef = sourceRef;
		this.expressions = expressions;
	}
	
	public Expression(SourceReference sourceRef, Expression e, Expression[] expressions) {
		this.sourceRef = sourceRef;
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
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@NonNull
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
	public abstract String toString();
	
}
