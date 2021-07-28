package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class Invalids {

	public static final String STRING = "INVALID";
	
	public static final UnitSource SOURCE = new UnitSource(STRING, "");
	
	public static final DeclarationModifiers MODIFIERS = new DeclarationModifiers(new Modifier[0]);

	public static final Signature SIGNATURE = new Signature(STRING, "INVALID SIGNATURE", STRING);
	
	public static final VarType TYPE = new VarType() {
		public String getSignature() { return STRING; }
		public String getName() { return STRING; }
		public boolean equals(Object o) { return this == o; }
	};
	
	public static final Operation OPERATION = new Operation() {
		public VarType getResultType() { return TYPE; }
		public VarType getLOType() { return TYPE; }
		public VarType getROType() { return TYPE; }
	};

	public static final Expression EXPRESSION = new Expression(SOURCE, 0, 0) {
		public String toString() { return STRING; }
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) { return TYPE; }
	};
	
	public static final Statement STATEMENT = new Statement(SOURCE, 0, 0) {
		public String toString() { return STRING; }
	};

	public static final VariableDeclaration VARIABLE_DECLARATION = new VariableDeclaration(SOURCE, 0, 0,
			STRING, TYPE, EXPRESSION);
	public static final AffectationSt AFFECTATION_STATEMENT = new AffectationSt(SOURCE, 0, 0,
			EXPRESSION, EXPRESSION);

	public static final LiteralExp<?> LITERAL_EXPRESSION = new LiteralExp<Object>(SOURCE, 0, 0, null) {
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) { return TYPE; }
	};

	public static final StructSection STRUCT = new StructSection(SOURCE, 0, 0, STRING, new VariableDeclaration[0], new StructConstructor[0], null);
	public static final StructPrototype STRUCT_PROTOTYPE = new StructPrototype(DeclarationVisibility.GLOBAL, new VariablePrototype[0], new ConstructorPrototype[0], SIGNATURE);
	
	public static final StructConstructor CONSTRUCTOR = new StructConstructor(SOURCE, 0, 0, new FunctionArgument[0]);
	public static final ConstructorPrototype CONSTRUCTOR_PROTOTYPE = new ConstructorPrototype(new VarType[0], new String[0], SIGNATURE);

	public static final VarFunctionType FUNCTION_TYPE = new VarFunctionType(TYPE, new VarType[0]);
	public static final FunctionPrototype FUNCTION_PROTO = new FunctionPrototype(SIGNATURE, FUNCTION_TYPE, MODIFIERS);

	public static final VariablePrototype VARIABLE_PROTO = new VariablePrototype(SIGNATURE, TYPE, MODIFIERS);;

}
