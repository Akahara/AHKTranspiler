package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class DirectAccessExp extends Expression {
	
	public final String member;
	
	public DirectAccessExp(UnitSource source, int sourceStart, int sourceStop, Expression struct, String element) {
		super(source, sourceStart, sourceStop, struct);
		this.member = element;
	}
	
	public Expression getStruct() {
		return expressions[0];
	}
	
	/** Cannot be used safely before the linker types check */
	public VarStructType getStructType() {
		return (VarStructType) getStruct().getType();
	}
	
	@Override
	public String toString() {
		return getStruct()+"."+member;
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		VarType instanceType = getStruct().getType();
		if(!(instanceType instanceof VarStructType)) {
			errors.add("Cannot access a member of an instance of type " + instanceType + this.getErr());
			return Invalids.TYPE;
		}
		StructPrototype prototype = ((VarStructType) instanceType).structure;
		VariablePrototype member = prototype.getMember(this.member);
		if(member == null) {
			errors.add("Type " + prototype.getName() + " does not have a member named " + this.member + this.getErr());
			return Invalids.TYPE;
		}
		return member.type;
	}

}
