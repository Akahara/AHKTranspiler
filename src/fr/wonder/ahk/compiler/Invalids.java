package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.Modifier;
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
	
	public static final ValueDeclaration VALUE = new ValueDeclaration() {
		public int getSourceStop() { return 0; }
		public int getSourceStart() { return 0; }
		public UnitSource getSource() { return Invalids.SOURCE; }
		public DeclarationVisibility getVisibility() { return DeclarationVisibility.SECTION; }
		public VarType getType() { return Invalids.TYPE; }
		public String getName() { return Invalids.STRING; }
		public DeclarationModifiers getModifiers() { return Invalids.MODIFIERS; }
	};

	public static final Operation OPERATION = new Operation() {
		public VarType getResultType() { return Invalids.TYPE; }
		public VarType[] getOperandsTypes() { return new VarType[0]; }
	};

	public static final Signature ACCESS_SIGNATURE = new Signature(Invalids.STRING, "INVALID ACCESS", "INVALID ACCESS");
	public static final VarAccess ACCESS = new VarAccess() {
		public VarType getType() { return Invalids.TYPE; }
		public Signature getSignature() { return Invalids.ACCESS_SIGNATURE; }
	};

	public static final Expression EXPRESSION = new Expression(Invalids.SOURCE, 0, 0) {
		public String toString() { return "INVALID"; }
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) { return Invalids.TYPE; }
	};
	
}
