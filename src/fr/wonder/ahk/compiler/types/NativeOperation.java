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
	private final VarType[] operandsTypes;
	private final String registry;
	
	private NativeOperation(VarType l, VarType r, Operator o, VarType resultType) {
		this.resultType = resultType;
		this.operandsTypes = new VarType[] { l, r };
		this.registry = l.getName()+'_'+r.getName()+'_'+o.name();
	}

	@Override
	public VarType getResultType() {
		return resultType;
	}
	
	@Override
	public VarType[] getOperandsTypes() {
		return operandsTypes;
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
	
	private static void add(VarType l, VarType r, Operator o, VarType result) {
		nativeOperations.put(new Triplet<>(l, r, o), new NativeOperation(l, r, o, result));
	}
	
	static {
		add(INT, INT, ADD, INT);
		add(INT, INT, SUBSTRACT, INT);
		add(INT, INT, MULTIPLY, INT);
		add(INT, INT, DIVIDE, INT);
		add(INT, INT, MOD, INT);
		add(INT, INT, EQUALS, BOOL);
		add(INT, INT, GREATER, BOOL);
		add(INT, INT, GEQUALS, BOOL);
		add(INT, INT, LOWER, BOOL);
		add(INT, INT, LEQUALS, BOOL);
		add(INT, INT, NEQUALS, BOOL);
		
		add(FLOAT, FLOAT, ADD, FLOAT);
		add(FLOAT, FLOAT, SUBSTRACT, FLOAT);
		add(FLOAT, FLOAT, MULTIPLY, FLOAT);
		add(FLOAT, FLOAT, DIVIDE, FLOAT);
		add(FLOAT, FLOAT, MOD, FLOAT);
		add(FLOAT, FLOAT, EQUALS, BOOL);
		add(FLOAT, FLOAT, GREATER, BOOL);
		add(FLOAT, FLOAT, GEQUALS, BOOL);
		add(FLOAT, FLOAT, LOWER, BOOL);
		add(FLOAT, FLOAT, LEQUALS, BOOL);

		add(BOOL, BOOL, EQUALS, BOOL);
		add(BOOL, BOOL, NEQUALS, BOOL);
		
		add(STR, STR, ADD, STR);
	}
	
	public static NativeOperation get(VarType l, VarType r, Operator o) {
		return nativeOperations.get(new Triplet<>(l, r, o));
	}

	public static Operation getOperation(VarType leftOp, Operator operator, VarType rightOp) {
		int lo = getOrder(leftOp);
		int ro = getOrder(rightOp);
		if(lo == -1 || ro == -1)
			return get(leftOp, rightOp, operator);
		int maxOrder = lo > ro ? lo : ro;
		return get(nativeOrder[maxOrder], nativeOrder[maxOrder], operator);
	}
	
}
