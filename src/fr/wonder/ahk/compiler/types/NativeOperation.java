package fr.wonder.ahk.compiler.types;

import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.Operator.NOT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.BOOL;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.STR;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.types.Triplet;
import fr.wonder.commons.utils.ArrayOperator;

/**
 * Native operations are operations that can be made on native types (bool, int,
 * float and str)
 * 
 * <p>
 * There are only a few well defined operations, most other report themselves to
 * well defined one, using implicit conversions (bool converts to int and int
 * converts to float).
 * 
 * <p>
 * Additionally some more special operations are defined, either taking a single
 * operand (always the right one) or applying to strings:
 * <p>
 * <ul>
 * <li>STR + STR</li>
 * <li>- INT</li>
 * <li>- FLOAT</li>
 * <li>! BOOL</li>
 * </ul>
 * </p>
 * 
 * There are no other operations taking a single operand or involving strings.
 * 
 * <p>
 * Operators are classed in categories, depending on the operations they are
 * involved in. The category of an operator can be retrieved using
 * {@link #getOperatorCategory(Operator)},
 * 
 * 
 * <p>
 * For transpilers, there is the complete list of defined operations for each
 * operator:
 * <ul>
 * <li>Operators + - * (category 0)
 * <ul>
 * <li>INT with INT returns INT</li>
 * <li>FLOAT with FLOAT returns FLOAT</li>
 * </ul>
 * </li>
 * 
 * <li>Operators == != < > <= >= === (category 1)
 * <ul>
 * <li>BOOL with BOOL returns BOOL</li>
 * <li>INT with INT returns BOOL</li>
 * <li>FLOAT with FLOAT returns BOOL</li>
 * </ul>
 * </li>
 * 
 * <li>Operators / % ^ (category 2)
 * <ul>
 * <li>INT with INT returns INT</li>
 * <li>FLOAT with FLOAT returns FLOAT</li>
 * <li>operations involving booleans are undefined</li>
 * </ul>
 * </li>
 * 
 * <li>Operators << >> (category 3)
 * <ul>
 * <li>INT with INT returns INT</li>
 * <li>this is the only defined operation, no implicit casts are allowed</li>
 * </ul>
 * </li>
 * 
 * <li>Operator ! (category 4)
 * <ul>
 * <li>The ! operator cannot be used with 2 operands</li>
 * </ul>
 */
public class NativeOperation extends Operation {
	
	protected NativeOperation(VarType l, VarType r, Operator o, VarType resultType) {
		super(l, r, o, resultType);
	}

	/** Used to minimize the number */
	private static final VarType[] nativeOrder = { BOOL, INT, FLOAT };
	
	private static final int getOrder(VarType t) {
		return ArrayOperator.indexOf(nativeOrder, t);
	}
	
	public static final NativeOperation STR_ADD_STR = new NativeOperation(STR, STR, ADD, STR);
	/** boolean negation "!x" */
	public static final NativeOperation NOT_BOOL = new NativeOperation(null, BOOL, NOT, BOOL);
	/** negation "-x" */
	public static final NativeOperation NEG_FLOAT = new NativeOperation(null, FLOAT, SUBSTRACT, FLOAT);
	/** negation "-x" */
	public static final NativeOperation NEG_INT = new NativeOperation(null, INT, SUBSTRACT, INT);
	
	/**
	 * B for bool, I for int, F for float, O for check order: check for the same
	 * operation with higher order types (bool+int refers to int+int for example),
	 * X for none.
	 */
	private static final int B=0,I=1,F=2,O=-1,X=-2;
	
	/**
	 * These tables contain the result for operations between native types:
	 * the column represents the left operand type: B, I or F (in this order),
	 * and the row represents the right operand type.
	 * 
	 * When a cell contains O, it "redirects" to the nearest cell on the descending
	 * diagonal, this means that a single NativeOperation instance is created
	 * (ie int_int_ADD) and (if cast is allowed in #getOperation) more than one
	 * pair of types will use it (ie int,bool bool,int bool,bool and int,int).
	 */
	private static final int[][] RESULT_TABLES = {
			{ // + - *
				O,O,O,
				O,I,O,
				O,O,F
			},
			{ // == != < > <= >= ===
				B,O,O,
				O,B,O,
				O,O,B
			},
			{ // / % ^
				X,X,X,
				X,I,O,
				X,O,F
			},
			{ // << >>
				X,X,X,
				X,I,X,
				X,X,X
			},
			{ // || && // TODO fix boolean operators (int||bool should result in boolcast||bool->bool)
				B,I,X,
				I,B,X,
				X,X,X,
			}
	};
	
	private static final Map<Triplet<Integer, Integer, Operator>, NativeOperation> strictOperations = new HashMap<>();
	private static final Map<Triplet<Integer, Integer, Operator>, NativeOperation> castedOperations = new HashMap<>();
	
	/** May return null */
	public static NativeOperation getOperation(VarType l, VarType r, Operator o, boolean allowCast) {
		// "special" operators
		if(l == STR && r == STR && o == ADD)
			return STR_ADD_STR;
		if(o == Operator.NOT) {
			if(l != null)
				throw new IllegalArgumentException("The negation operation takes one argument only");
			if(getOrder(r) != -1) // r is int/float/bool
				return NOT_BOOL;
			return null;
		}
		if(o == Operator.SUBSTRACT && l == null) {
			if(r == INT)
				return NEG_INT;
			else if(r == FLOAT)
				return NEG_FLOAT;
			else
				return null;
		}

		int lorder = getOrder(l);
		int rorder = getOrder(r);
		
		if(lorder == -1 || rorder == -1)
			return null;
		
		var key = new Triplet<>(lorder, rorder, o);
		
		var operationsMap = allowCast ? castedOperations : strictOperations;
		if(operationsMap.containsKey(key))
			return operationsMap.get(key);
		
		int[] table = getOperatorTable(o);
		int resultOrder = table[lorder+rorder*3];
		
		VarType result = resultOrder < 0 ? null : nativeOrder[resultOrder];
		NativeOperation strictOperation = result == null ? null : new NativeOperation(l, r, o, result);
		NativeOperation castedOperation = strictOperation;
		strictOperations.put(key, strictOperation);
		
		if(resultOrder == O) { // O means "refer to the upper order type"
			int castedOrder = Math.max(Math.max(lorder, rorder), I);
			resultOrder = table[castedOrder*4];
			var castedKey = new Triplet<>(castedOrder, castedOrder, o);
			if(strictOperations.containsKey(castedKey)) {
				castedOperation = strictOperations.get(castedKey);
			} else {
				VarType castedType = nativeOrder[castedOrder];
				castedOperation = resultOrder < 0 ? null : new NativeOperation(
						castedType, castedType, o, nativeOrder[resultOrder]);
				strictOperations.put(castedKey, castedOperation);
				castedOperations.put(castedKey, castedOperation);
			}
		}
		castedOperations.put(key, castedOperation);
		
		return allowCast ? castedOperation : strictOperation;
	}
	
	private static int[] getOperatorTable(Operator o) {
		return RESULT_TABLES[getOperatorCategory(o)];
	}
	
	/**
	 * Returns the category of the given operator.
	 * 
	 * @see NativeOperation
	 */
	public static int getOperatorCategory(Operator o) {
		switch(o) {
		case ADD:
		case SUBSTRACT:
		case MULTIPLY:
			return 0;
		case EQUALS:
		case GEQUALS:
		case GREATER:
		case LEQUALS:
		case LOWER:
		case NEQUALS:
		case STRICTEQUALS:
			return 1;
		case DIVIDE:
		case MOD:
		case POWER:
			return 2;
		case SHL:
		case SHR:
			return 3;
		case OR:
		case AND:
			return 4;
		case NOT:
			throw new UnreachableException("The not operator was not taken care of");
		}
		throw new UnreachableException(o.toString());
	}
	
}
