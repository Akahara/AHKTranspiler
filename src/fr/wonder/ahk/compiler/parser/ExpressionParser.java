package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.types.Tuple;

public class ExpressionParser extends AbstractParser {
	
	static class Section {
		
		SectionToken type;
		/** Inner positions (line[start|stop] won't likely be a parenthesis) */
		int start, stop;
		List<Section> subSections = new ArrayList<>();
		/** Operator - Position tuples */
		List<Tuple<Operator, Integer>> operators = new ArrayList<>();
		
		Section(SectionToken type, int start) {
			this.type = type;
			this.start = start;
		}
		
		Section(int start, int stop) {
			this.start = start;
			this.stop = stop;
		}
		
		@Override
		public String toString() {
			return type+"["+start+":"+stop+"]";
		}
		
		Section getSubSection(int start, int stop) {
			Section subSection = new Section(start, stop);
			for(Section sec : subSections) {
				if(sec.start == start && sec.stop == stop)
					return sec;
				if(sec.start < stop && sec.stop >= start)
					subSection.subSections.add(sec);
				if((sec.start < stop && sec.stop > stop) || (sec.stop > start && sec.start < start))
					throw new UnreachableException("Unexpected section overlap");
			}
			for(Tuple<Operator, Integer> op : operators) {
				if(start <= op.b && op.b < stop)
					subSection.operators.add(op);
			}
			return subSection;
		}
		
		int advancePointer(int pointer) {
			boolean updated = true;
			while(updated) {
				updated = false;
				for(Section s : subSections) {
					if(s.start-1 == pointer) {
						pointer = s.stop+1;
						updated = true;
						break;
					}
				}
			}
			return pointer+1;
		}
		
	}

	static Section getVisibleSection(Token[] line, int start, int stop) {
		List<Section> sections = new ArrayList<>();
		Section current = new Section(start, stop);
		
		for(int i = start; i < stop; i++) {
			TokenBase t = line[i].base;
			if(Tokens.isOperator(t)) {
				current.operators.add(new Tuple<>(Tokens.getOperator(t), i));
				continue;
			}
			for(SectionToken sec : Tokens.CODE_SECTIONS) {
				if(t == sec.start) {
					Section subSection = new Section(sec, i+1);
					current.subSections.add(subSection);
					sections.add(current);
					current = subSection;
					break;
				} else if(t == sec.stop) {
					if(current.type != sec)
						throw new IllegalStateException("Unexpected section closing: " + sec);
					current.stop = i;
					current = sections.remove(sections.size()-1);
					break;
				}
			}
		}
		if(!sections.isEmpty())
			throw new IllegalArgumentException("Unexpected unclosed section: " + sections.get(sections.size()-1));
		return current;
	}
	
	public static Expression parseExpression(Unit unit, Token[] line, int start, int stop, ErrorWrapper errors) {
		return parseExpression(unit, line, getVisibleSection(line, start, stop), errors);
	}
	
	private static Expression parseExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
//		Utils.dump(line, section.start, section.stop);
		if(section.stop == section.start) {
			errors.add("Empty expression:" + line[section.start].getErr());
			return Invalids.EXPRESSION;
		}
		
		Section firstSection = section.subSections.isEmpty() ? null : section.subSections.get(0);
		
		// remove extra parenthesis
		while(section.subSections.size() == 1 && firstSection.start == section.start+1 && 
				firstSection.stop == section.stop-1 && firstSection.type == SectionToken.SEC_PARENTHESIS) {
			section = firstSection;
			firstSection = section.subSections.isEmpty() ? null : section.subSections.get(0);
		}
		
		// parse operation
		if(!section.operators.isEmpty())
			return parseOperationExpression(unit, line, section, errors);
		
		// parse single token expression
		if(section.stop-section.start == 1) {
			Token tk = line[section.start];
			if(tk.base == TokenBase.LIT_NULL)
				return new NullExp(tk.sourceRef);
			else if(tk.base == TokenBase.VAR_VARIABLE)
				return new VarExp(tk.sourceRef, line[section.start].text);
			else if(Tokens.isLiteral(tk.base))
				return parseLiteral(line[section.start], errors);
			errors.add("Unknown expression type" + tk.getErr());
			return Invalids.EXPRESSION;
		}

		Section lastSection = section.subSections.isEmpty() ? null : section.subSections.get(section.subSections.size()-1);
		
		// parse conversion expression
		if(section.subSections.size() == 1 && lastSection.type == SectionToken.SEC_PARENTHESIS &&
				lastSection.start == section.start+3 && line[section.start+1].base == TokenBase.TK_COLUMN) {
			
			Token typeToken = line[section.start];
			VarType type;
			if(typeToken.base == TokenBase.VAR_UNIT) {
				errors.add("Cannot cast to a struct type:" + typeToken.getErr());
				type = Invalids.TYPE;
			} else {
				type = parseType(unit, line, new Pointer(section.start), errors);
			}
			if(type != null) {
				Expression casted = parseExpression(unit, line, lastSection.start, lastSection.stop, errors);
				return new ConversionExp(sourceRefOfSection(line, firstSection), type, casted, false);
			}
			errors.add("Expected a type to cast to:" + typeToken.getErr());
			return Invalids.EXPRESSION;
		}
		
		// parse sizeof
		if(line[section.start].base == TokenBase.KW_SIZEOF) {
			Expression exp = parseExpression(unit, line, section.getSubSection(section.start+1, section.stop), errors);
			return new SizeofExp(sourceRefOfSection(line, firstSection), exp);
		}
		
		// parse direct access exp
		if(section.stop-section.start >= 3 && line[section.stop-2].base == TokenBase.TK_DOT) {
			return parseDirectAccessExpression(unit, line, section, errors);
		}
		
		// parse function and arrays
		if(lastSection != null) {
			if(lastSection.type == SectionToken.SEC_PARENTHESIS) {
				if(section.start+2 == lastSection.start && line[section.start].base == TokenBase.VAR_UNIT)
					return parseConstructorExpression(unit, line, lastSection, errors);
				else
					return parseFunctionExpression(unit, line, section, errors);
			} else if(lastSection.type == SectionToken.SEC_BRACKETS) {
				if(section.start+1 == lastSection.start)
					return parseArrayExpression(unit, line, section, errors);
				else
					return parseIndexingExpression(unit, line, section, errors);
			}
			throw new UnreachableException("Invalid section type " + lastSection.type);
		}
		
		errors.add("Unknown expression type " + unit.source.getErr(line, section.start, section.stop-1));
		return Invalids.EXPRESSION;
	}

	public static LiteralExp<?> parseLiteral(Token t, ErrorWrapper errors) {
		switch(t.base) {
		case LIT_INT:
			try {
				return new IntLiteral(t.sourceRef, Long.parseLong(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse int literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_FLOAT:
			try {
				return new FloatLiteral(t.sourceRef, Double.parseDouble(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse float literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_STR:
			return new StrLiteral(t.sourceRef, t.text);
		case LIT_BOOL_TRUE:
			return new BoolLiteral(t.sourceRef, true);
		case LIT_BOOL_FALSE:
			return new BoolLiteral(t.sourceRef, false);
		default:
			errors.add("Unable to parse literal: Token is not a literal value" + t.getErr());
			return Invalids.LITERAL_EXPRESSION;
		}
	}

	/** Assumes that section.operators is not empty */
	private static Expression parseOperationExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		Tuple<Operator, Integer> operator = section.operators.get(0);
		// Tuple.second is the operator position in the line
		
		for(int i = 1; i < section.operators.size(); i++) {
			Tuple<Operator, Integer> op = section.operators.get(i);
			if(op.a.priority <= operator.a.priority)
				operator = op;
		}
		Token opt = line[operator.b];
		Expression leftOperand = null;
		Expression rightOperand = null;
		if(operator.b == section.start) {
			if(!operator.a.doesSingleOperand)
				errors.add("Operator " + opt.text + " does not allow for single operand operations" + opt.getErr());
		} else {
			leftOperand = parseExpression(unit, line, section.getSubSection(section.start, operator.b), errors);
		}
		if(operator.b+1 == section.stop) {
			errors.add("Operator " + opt.text + " does not have a right operand" + opt.getErr());
		} else {
			rightOperand = parseExpression(unit, line, section.getSubSection(operator.b+1, section.stop), errors);
		}
		
		if(leftOperand == null) {
			if(operator.a == Operator.SUBSTRACT) {
				if(rightOperand instanceof IntLiteral)
					return new IntLiteral(SourceReference.concat(opt, rightOperand), -((IntLiteral) rightOperand).value);
				else if(rightOperand instanceof FloatLiteral)
					return new FloatLiteral(SourceReference.concat(opt, rightOperand), -((FloatLiteral) rightOperand).value);
			}
			
			return new OperationExp(sourceRefOfSection(line, section), operator.a, rightOperand);
		} else {
			return new OperationExp(sourceRefOfSection(line, section), operator.a, leftOperand, rightOperand);
		}
	}
	
	public static Expression[] parseArgumentList(Unit unit, Token[] line, int start, int stop, ErrorWrapper errors) {
		return parseArgumentList(unit, line, getVisibleSection(line, start, stop), errors);
	}
	
	private static Expression[] parseArgumentList(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		List<Expression> arguments = new ArrayList<>();
		if(section.stop-section.start != 0) {
			int last = section.start;
			for(int i = section.start; i < section.stop; i = section.advancePointer(i)) {
				if(line[i].base == TokenBase.TK_COMMA) {
					arguments.add(parseExpression(unit, line, section.getSubSection(last, i), errors));
					last = i+1;
				}
			}
			arguments.add(parseExpression(unit, line, section.getSubSection(last, section.stop), errors));
		}
		return arguments.toArray(Expression[]::new);
	}

	/** Assumes that the section has its size >= 3 and that the second-to-last token is a dot */
	private static Expression parseDirectAccessExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		Token memberToken = line[section.stop-1];
		if(memberToken.base != TokenBase.VAR_VARIABLE) {
			errors.add("Expected a struct member name:" + memberToken.getErr());
			return Invalids.EXPRESSION;
		}
		Expression structInstance = parseExpression(unit, line, section.getSubSection(section.start, section.stop-2), errors);
		String memberName = memberToken.text;
		return new DirectAccessExp(sourceRefOfSection(line, section), structInstance, memberName);
	}
	
	/** Assumes that the last subsection of section is a parenthesis section */
	private static Expression parseFunctionExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		Section parenthesis = section.subSections.get(section.subSections.size()-1);
		Expression[] arguments = parseArgumentList(unit, line, parenthesis, errors);
		Expression function = parseExpression(unit, line, section.getSubSection(section.start, parenthesis.start-1), errors);
		return new FunctionCallExp(sourceRefOfSection(line, section), function, arguments);
	}
	
	/** Assumes that section is a parenthesis section and two tokens before it is a struct token */
	private static Expression parseConstructorExpression(Unit unit, Token[] line,
			Section section, ErrorWrapper errors) {
		Expression[] arguments = parseArgumentList(unit, line, section, errors);
		Token structToken = line[section.start-2];
		VarStructType type = unit.getStructType(structToken);
		return new ConstructorExp(SourceReference.concat(structToken, line[section.stop]), type, arguments);
	}
	
	/** Assumes that the last subsection of section is a bracket section */
	private static Expression parseIndexingExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		Section brackets = section.subSections.get(section.subSections.size()-1);
		Expression[] arguments = parseArgumentList(unit, line, brackets, errors);
		if(arguments.length == 0)
			errors.add("Empty index" + unit.source.getErr(line));
		Expression array = parseExpression(unit, line, section.getSubSection(section.start, brackets.start-1), errors);
		return new IndexingExp(sourceRefOfSection(line, section), array, arguments);
	}
	
	private static Expression parseArrayExpression(Unit unit, Token[] line, Section section, ErrorWrapper errors) {
		Section brackets = section.subSections.get(section.subSections.size()-1);
		Expression[] values = parseArgumentList(unit, line, brackets, errors);
		return new ArrayExp(sourceRefOfSection(line, section), values);
	}
	
	private static SourceReference sourceRefOfSection(Token[] line, Section section) {
		return SourceReference.fromLine(line, section.start, section.stop-1);
	}
	
}
