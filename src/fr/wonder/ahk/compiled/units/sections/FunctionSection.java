package fr.wonder.ahk.compiled.units.sections;

import java.util.Arrays;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.utils.Utils;

public class FunctionSection extends SourceObject implements Operation, ValueDeclaration {
	
	// set by the unit parser
	public String name;
	public VarType returnType;
	public FunctionArgument[] arguments;
	public VarType[] argumentTypes;
	public DeclarationModifiers modifiers;
	
	// set by the unit parser using the statement parser
	public Statement[] body;
	
//	// set by the linker using #makeSignature
//	/** the global scope signature of this function */
//	private String signature;
//	/** the signature that can be used when already inside the declaring unit */
//	private String unitSignature;
	private Signature signature;
	
	public FunctionSection(UnitSource source, int sourceStart, int sourceStop) {
		super(source, sourceStart, sourceStop);
	}
	
//	/**
//	 * Used to create an dummy empty function, is currently used by the asm x64 {@link FunctionWriter}
//	 * to have an empty initialization function.
//	 */
//	public FunctionSection(Unit unit) {
//		super(unit.source, 0, 0);
//		this.returnType = VarType.VOID;
//		this.arguments = new FunctionArgument[0];
//		this.argumentTypes = new VarType[0];
//		this.modifiers = new DeclarationModifiers(new Modifier[0]);
//		this.body = new Statement[0];
//		this.makeSignature(unit.fullBase);
//	}
	
	@Override
	public String toString() {
		return "func " + returnType + " " + name + "(" + Utils.toString(arguments) + ")";
	}

	/** Called by the linker after the function argument types where computed */
	public void setSignature(Signature signature) {
		this.signature = signature;
	}
	
	/** global scope signature (declaring unit full base + unit scope signature) */
	public Signature getSignature() {
		return signature;
	}
	
	@Override
	public VarType getResultType() {
		return returnType;
	}
	
	@Override
	public VarType[] getOperandsTypes() {
		return argumentTypes;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	/** Beware, this is not the same as {@link #getFunctionType()} */
	@Override
	public VarType getType() {
		return getResultType();
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
	/**
	 * Returns the {@link VarFunctionType} associated with this function,
	 * cannot be called before the linker linked types.
	 */
	public VarType getFunctionType() {
		return new VarFunctionType(returnType, argumentTypes);
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.GLOBAL; // TODO read function visibility
	}
	
	public static boolean argsMatch0c(VarType[] args, VarType[] provided) {
		return Arrays.equals(args, provided);
	}
	
	/** redirects to {@link #argsMatch0c(VarType[], VarType[])} */
	public static boolean argsMatch0c(VarType[] args, VarType[] provided, ConversionTable conversions) {
		return argsMatch0c(args, provided);
	}
	
	public static boolean argsMatch1c(VarType[] args, VarType[] provided, ConversionTable conversions) {
		if(args.length != provided.length)
			return false;
		boolean casted = false;
		for(int i = 0; i < args.length; i++) {
			if(args[i] == provided[i]) {
				continue;
			} else if(!casted && conversions.canConvertImplicitely(provided[i], args[i])) {
				casted = true;
			} else {
				return false;
			}
		}
		return true;
	}
	
	public static boolean argsMatchXc(VarType[] args, VarType[] provided, ConversionTable conversions) {
		if(args.length != provided.length)
			return false;
		for(int i = 0; i < args.length; i++)
			if(!conversions.canConvertImplicitely(provided[i], args[i]))
				return false;
		return true;
	}
}
