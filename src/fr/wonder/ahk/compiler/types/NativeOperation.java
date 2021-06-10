package fr.wonder.ahk.compiler.types;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.types.Triplet;

import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.types.VarType.*;

public class NativeOperation implements Operation {
	
	private final VarType resultType;
	private final VarType leftOperand, rightOperand;
	private final String registry;

	private NativeOperation(VarType l, VarType r, Operator o, VarType resultType) {
		this.resultType = resultType;
		this.leftOperand = l;
		this.rightOperand = r;
		this.registry = (l == null ? "" : l.getName())+'_'+r.getName()+'_'+o.name();
	}
	
	@Override
	public VarType getResultType() {
		return resultType;
	}

	@Override
	public VarType getLOType() {
		return leftOperand;
	}
	
	@Override
	public VarType getROType() {
		return rightOperand;
	}
	
	@Override
	public String toString() {
		return registry;
	}
	
	private static final VarType[] nativeOrder = { BOOL, INT, FLOAT };
	private static final int getOrder(VarType t) {
		for(int i = 0; i < nativeOrder.length; i++)
			if(t == nativeOrder[i])
				return i;
		return -1;
	}
	
	private static final Map<Triplet<VarType, VarType, Operator>, NativeOperation> nativeOperations = new HashMap<>();

	private static void createOperation(VarType l, VarType r, Operator o, VarType result) {
		nativeOperations.put(new Triplet<>(l, r, o), new NativeOperation(l, r, o, result));
	}
	
	static {
		createOperation(INT,  INT, ADD,       INT);
		createOperation(INT,  INT, SUBSTRACT, INT);
		createOperation(null, INT, SUBSTRACT, INT);
		createOperation(INT,  INT, MULTIPLY,  INT);
		createOperation(INT,  INT, DIVIDE,    INT);
		createOperation(INT,  INT, MOD,       INT);
		createOperation(INT,  INT, EQUALS,    BOOL);
		createOperation(INT,  INT, GREATER,   BOOL);
		createOperation(INT,  INT, GEQUALS,   BOOL);
		createOperation(INT,  INT, LOWER,     BOOL);
		createOperation(INT,  INT, LEQUALS,   BOOL);
		createOperation(INT,  INT, NEQUALS,   BOOL);
		
		createOperation(FLOAT, FLOAT, ADD,       FLOAT);
		createOperation(FLOAT, FLOAT, SUBSTRACT, FLOAT);
		createOperation(null,  FLOAT, SUBSTRACT, FLOAT);
		createOperation(FLOAT, FLOAT, MULTIPLY,  FLOAT);
		createOperation(FLOAT, FLOAT, DIVIDE,    FLOAT);
		createOperation(FLOAT, FLOAT, MOD,       FLOAT);
		createOperation(FLOAT, FLOAT, EQUALS,    BOOL);
		createOperation(FLOAT, FLOAT, GREATER,   BOOL);
		createOperation(FLOAT, FLOAT, GEQUALS,   BOOL);
		createOperation(FLOAT, FLOAT, LOWER,     BOOL);
		createOperation(FLOAT, FLOAT, LEQUALS,   BOOL);

		createOperation(BOOL, BOOL, EQUALS,  BOOL);
		createOperation(BOOL, BOOL, NEQUALS, BOOL);
		
		createOperation(STR, STR, ADD, STR);
	}
	
	/** This function may return null */
	private static NativeOperation getStrict(VarType l, VarType r, Operator o) {
		return nativeOperations.get(new Triplet<>(l, r, o));
	}

	public static NativeOperation getOperation(VarType leftOp, VarType rightOp, Operator operator, boolean allowCast) {
		if(!allowCast)
			return getStrict(leftOp, rightOp, operator);
		int lo = getOrder(leftOp);
		int ro = getOrder(rightOp);
		if(lo == -1 || ro == -1)
			return getStrict(leftOp, rightOp, operator);
		int maxOrder = lo > ro ? lo : ro;
		return getStrict(nativeOrder[maxOrder], nativeOrder[maxOrder], operator);
	}
	
}
