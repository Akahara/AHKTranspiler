package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiled.units.sections.Blueprint;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.OverloadedOperator;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.types.Operation;

public class Invalids {

	public static final String STRING = "INVALID";
	
	public static final UnitSource SOURCE = new UnitSource(STRING, "");
	public static final SourceReference SOURCE_REF = new SourceReference(SOURCE, 0, 0);
	public static final Unit UNIT = new Unit(SOURCE, STRING, STRING, new String[0], 0);
	
	static {
		UNIT.variables = new VariableDeclaration[0];
		UNIT.functions = new FunctionSection[0];
		UNIT.structures = new StructSection[0];
		UNIT.accessibleAliases = new Alias[0];
	}
	
	public static final DeclarationModifiers MODIFIERS = new DeclarationModifiers(DeclarationVisibility.LOCAL, new Modifier[0]);

	public static final Signature SIGNATURE = new Signature(STRING, "INVALID SIGNATURE", STRING);
	
	public static final VarType TYPE = new VarType() {
		public String getSignature() { return STRING; }
		public String getName() { return STRING; }
		public boolean equals(Object o) { return this == o; }
		public VarType[] getSubTypes() { return new VarType[0]; }
	};

	public static final GenericContext GENERIC_CONTEXT = new GenericContext(null, new VarGenericType[0]);
	
	public static final Operation OPERATION = new Operation(TYPE, TYPE, Operator.ADD, TYPE) {};

	public static final Expression EXPRESSION = new Expression(SOURCE_REF) {
		public String toString() { return STRING; }
	};
	
	public static final Statement STATEMENT = new Statement(SOURCE_REF) {
		public String toString() { return STRING; }
	};

	public static final VariableDeclaration VARIABLE_DECLARATION = new VariableDeclaration(UNIT, SOURCE_REF,
			STRING, TYPE, MODIFIERS, EXPRESSION);
	public static final AffectationSt AFFECTATION_STATEMENT = new AffectationSt(SOURCE_REF,
			EXPRESSION, EXPRESSION);

	public static final LiteralExp<?> LITERAL_EXPRESSION = new LiteralExp<Object>(SOURCE_REF, TYPE, null) {};

	public static final StructSection STRUCTURE = new StructSection(UNIT, SOURCE_REF, STRING, GENERIC_CONTEXT, MODIFIERS);
	
	static {
		STRUCTURE.members = new VariableDeclaration[0];
		STRUCTURE.constructors = new StructConstructor[0];
		STRUCTURE.nullFields = new ConstructorDefaultValue[0];
	}
	
	public static final StructPrototype STRUCT_PROTOTYPE = new StructPrototype(new VariablePrototype[0],
			new ConstructorPrototype[0], new OverloadedOperatorPrototype[0], GENERIC_CONTEXT, MODIFIERS, SIGNATURE);
	
	public static final StructConstructor CONSTRUCTOR = new StructConstructor(STRUCTURE, SOURCE_REF, MODIFIERS, new FunctionArgument[0]);
	public static final ConstructorPrototype CONSTRUCTOR_PROTOTYPE = new ConstructorPrototype(new VarType[0], new String[0], MODIFIERS, SIGNATURE);

	public static final VarFunctionType FUNCTION_TYPE = new VarFunctionType(TYPE, new VarType[0], GENERIC_CONTEXT);
	public static final FunctionPrototype FUNCTION_PROTO = new FunctionPrototype(SIGNATURE, FUNCTION_TYPE, GENERIC_CONTEXT, MODIFIERS);

	public static final VariablePrototype VARIABLE_PROTO = new VariablePrototype(SIGNATURE, TYPE, MODIFIERS);

	public static final Alias ALIAS = new Alias(SOURCE_REF, STRING, FUNCTION_TYPE);

	public static final FunctionSection FUNCTION = new FunctionSection(UNIT, SOURCE_REF, GENERIC_CONTEXT, MODIFIERS);

	public static final OverloadedOperator OVERLOADED_OPERATOR = new OverloadedOperator(STRUCTURE, SOURCE_REF, Operator.ADD, TYPE, TYPE, TYPE, STRING);

	public static final Blueprint BLUEPRINT = new Blueprint(UNIT, STRING, GENERIC_CONTEXT, MODIFIERS, SOURCE_REF);
	
	static {
		BLUEPRINT.variables = new VariableDeclaration[0];
		BLUEPRINT.functions = new FunctionSection[0];
	}

	public static final VarGenericType GENERIC_TYPE = new VarGenericType(STRING);
	
}
