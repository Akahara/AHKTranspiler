package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
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
	
	public static final UnitSource SOURCE = new UnitSource(Invalids.STRING, "");
	
	public static final DeclarationModifiers MODIFIERS = new DeclarationModifiers(new Modifier[0]);
	
	public static final VarType TYPE = new VarType() {
		public String getSignature() { return Invalids.STRING; }
		public String getName() { return Invalids.STRING; }
		public boolean equals(Object o) { return this == o; }
	};
	
	public static final Signature DECLARATION_SIGNATURE = new Signature(Invalids.STRING, "INVALID DECLARATION", "INVALID DECLARATION");
	public static final ValueDeclaration VALUE = new ValueDeclaration() {
		public int getSourceStop() { return 0; }
		public int getSourceStart() { return 0; }
		public UnitSource getSource() { return Invalids.SOURCE; }
		public DeclarationVisibility getVisibility() { return DeclarationVisibility.SECTION; }
		public VarType getType() { return Invalids.TYPE; }
		public String getName() { return Invalids.STRING; }
		public Signature getSignature() { return Invalids.DECLARATION_SIGNATURE; }
		public DeclarationModifiers getModifiers() { return Invalids.MODIFIERS; }
	};

	public static final Operation OPERATION = new Operation() {
		public VarType getResultType() { return Invalids.TYPE; }
		public VarType getLOType() { return Invalids.TYPE; }
		public VarType getROType() { return Invalids.TYPE; }
	};

	public static final Signature ACCESS_SIGNATURE = new Signature(Invalids.STRING, "INVALID ACCESS", "INVALID ACCESS");
	public static final VarAccess ACCESS = new VarAccess() {
		public VarType getType() { return Invalids.TYPE; }
		public Signature getSignature() { return Invalids.ACCESS_SIGNATURE; }
		public boolean matchesDeclaration(ValueDeclaration decl) {
			throw new IllegalStateException("An invalid access was used");
		}
	};

	public static final Expression EXPRESSION = new Expression(Invalids.SOURCE, 0, 0) {
		public String toString() { return Invalids.STRING; }
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) { return Invalids.TYPE; }
	};
	
	public static final Statement STATEMENT = new Statement(Invalids.SOURCE, 0, 0) {
		public String toString() { return Invalids.STRING; }
	};

	public static final VariableDeclaration VARIABLE_DECLARATION = new VariableDeclaration(Invalids.SOURCE, 0, 0,
			Invalids.STRING, Invalids.TYPE, Invalids.EXPRESSION);
	public static final AffectationSt AFFECTATION_STATEMENT = new AffectationSt(Invalids.SOURCE, 0, 0,
			Invalids.EXPRESSION, Invalids.EXPRESSION);

	public static final LiteralExp<?> LITERAL_EXPRESSION = new LiteralExp<Object>(Invalids.SOURCE, 0, 0, null) {
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) { return Invalids.TYPE; }
	};

	public static final StructSection STRUCT = new StructSection(Invalids.SOURCE, 0, 0, Invalids.STRING, Invalids.MODIFIERS, new VariableDeclaration[0], new StructConstructor[0]);
	public static final StructPrototype STRUCT_PROTOTYPE = new StructPrototype(Invalids.MODIFIERS, new VariablePrototype[0], new ConstructorPrototype[0], Invalids.ACCESS_SIGNATURE);
	
	public static final StructConstructor CONSTRUCTOR = new StructConstructor(Invalids.SOURCE, 0, 0, new FunctionArgument[0]);
	
}
