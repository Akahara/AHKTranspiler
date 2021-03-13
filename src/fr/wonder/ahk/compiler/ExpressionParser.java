package fr.wonder.ahk.compiler;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.types.Tuple;

public class ExpressionParser {
	
	private static class Section {
		
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
		
		int getPointerPos(int pointer) {
			boolean updated = true;
			while(updated) {
				updated = false;
				for(Section s : subSections) {
					if(s.start == pointer) {
						pointer = s.stop;
						updated = true;
						break;
					}
				}
			}
			return pointer;
		}
		
	}

	private static Section getVisibleSection(Token[] line, int start, int stop) {
		List<Section> sections = new ArrayList<>();
		Section current = new Section(start, stop);
		
//		Utils.dump(line);
//		System.out.println(start + " " + stop);
		
		for(int i = start; i < stop; i++) {
			TokenBase t = line[i].base;
			for(SectionToken sec : Tokens.CODE_SECTIONS) {
				if(t == sec.start) {
					Section subSection = new Section(sec, i+1);
					current.subSections.add(subSection);
					sections.add(current);
					current = subSection;
				} else if(t == sec.stop) {
					if(current.type != sec)
						throw new IllegalStateException("Unexpected section closing: " + sec);
					current.stop = i;
					current = sections.remove(sections.size()-1);
				}
			}
			if(Tokens.isOperator(t))
				current.operators.add(new Tuple<>(Tokens.getOperator(t), i));
		}
		if(!sections.isEmpty()) {
//			Utils.dump(line);
			throw new IllegalArgumentException("Unexpected unclosed section: " + sections.get(sections.size()-1));
		}
		return current;
	}
	
	public static Expression parseExpression(UnitSource source, Token[] line, int start, int stop, ErrorWrapper errors) {
		return parseExpression(source, line, getVisibleSection(line, start, stop), errors);
	}
	
	private static Expression parseExpression(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
		if(section.stop == section.start) {
			Utils.dump(line);
			System.out.println(section.start + "  " + section.stop);
			errors.add("Empty expression:" + line[section.start].getErr());
			return Invalids.EXPRESSION;
		}
		
		Section firstSection = section.subSections.isEmpty() ? null : section.subSections.get(0);
		
		while(section.subSections.size() == 1 && firstSection.start == section.start+1 && 
				firstSection.stop == section.stop-1 && firstSection.type == SectionToken.SEC_PARENTHESIS) {
			section = firstSection;
			firstSection = section.subSections.isEmpty() ? null : section.subSections.get(0);
		}
		
		int sourceStart = line[section.start].sourceStart;
		int sourceStop = line[section.start].sourceStop;
		
		if(!section.operators.isEmpty())
			return parseOperationExpression(source, line, section, errors);
		
		if(section.stop-section.start == 1) {
			Token tk = line[section.start];
			if(tk.base == TokenBase.VAR_VARIABLE)
				return new VarExp(source, sourceStart, sourceStop, line[section.start].text);
			else if(Tokens.isLiteral(tk.base))
				return parseLiteral(line[section.start], errors);
			else
				errors.add("Unknown expression type" + tk.getErr());
		}

		Section lastSection = section.subSections.isEmpty() ? null : section.subSections.get(section.subSections.size()-1);
		
		if(section.subSections.size() == 1 && lastSection.type == SectionToken.SEC_PARENTHESIS &&
				lastSection.start == section.start+3 && line[section.start+1].base == TokenBase.TK_COLUMN) {
			
			VarType type = Tokens.getType(line[section.start]);
			if(type != null) {
				Expression casted = parseExpression(source, line, lastSection.start, lastSection.stop, errors);
				return new ConversionExp(source, sourceStart, sourceStop, type, casted, false);
			}
		}
		
		if(line[section.start].base == TokenBase.KW_SIZEOF) {
			Expression exp = parseExpression(source, line, section.getSubSection(section.start+1, section.stop), errors);
			return new SizeofExp(source, sourceStart, sourceStop, exp);
		}
		
		if(lastSection != null) {
			if(lastSection.type == SectionToken.SEC_PARENTHESIS) {
				return parseFunctionExpression(source, line, section, errors);
			} else if(lastSection.type == SectionToken.SEC_BRACKETS) {
				if(section.start+1 == lastSection.start)
					return parseArrayExpression(source, line, section, errors);
				else
					return parseIndexingExpression(source, line, section, errors);
			}
		}
		
		errors.add("Unknown expression type " + source.getErr(line, section.start, section.stop-1));
		return Invalids.EXPRESSION;
	}
	
	public static LiteralExp<?> parseLiteral(Token t, ErrorWrapper errors) {
		switch(t.base) {
		case LIT_INT:
			try {
				return new IntLiteral(t.getSource(), t.sourceStart, t.sourceStop, Integer.parseInt(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse int literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_FLOAT:
			try {
				return new FloatLiteral(t.getSource(), t.sourceStart, t.sourceStop, Float.parseFloat(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse float literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_STR:
			return new StrLiteral(t.getSource(), t.sourceStart, t.sourceStop, t.text);
		case LIT_BOOL_TRUE:
			return new BoolLiteral(t.getSource(), t.sourceStart, t.sourceStop, true);
		case LIT_BOOL_FALSE:
			return new BoolLiteral(t.getSource(), t.sourceStart, t.sourceStop, false);
		default:
			errors.add("Unable to parse literal: Token is not a literal value" + t.getErr());
			return Invalids.LITERAL_EXPRESSION;
		}
	}

	/** Assumes that section.operators is not empty */
	private static Expression parseOperationExpression(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
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
			leftOperand = parseExpression(source, line, section.getSubSection(section.start, operator.b), errors);
		}
		if(operator.b+1 == section.stop) {
			errors.add("Operator " + opt.text + " does not have a right operand" + opt.getErr());
		} else {
			rightOperand = parseExpression(source, line, section.getSubSection(operator.b+1, section.stop), errors);
		}
		
		int sourceStart = line[section.start].sourceStart;
		int sourceStop = line[section.stop-1].sourceStop;
		
		if(leftOperand == null) {
			return new OperationExp(source, sourceStart, sourceStop, operator.a, rightOperand);
		} else {
			return new OperationExp(source, sourceStart, sourceStop, operator.a, leftOperand, rightOperand);
		}
	}
	
	private static Expression[] parseArgumentList(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
		List<Expression> arguments = new ArrayList<>();
		if(section.stop-section.start != 0) {
			int last = section.start;
			for(int i = section.start; i < section.stop; i = section.getPointerPos(i+1)) {
				if(line[i].base == TokenBase.TK_COMA) {
					arguments.add(parseExpression(source, line, section.getSubSection(last, i), errors));
					last = i+1;
				}
			}
			arguments.add(parseExpression(source, line, section.getSubSection(last, section.stop), errors));
		}
		return arguments.toArray(Expression[]::new);
	}
	
	/** Assumes that the last subsection of section is a parenthesis section */
	private static Expression parseFunctionExpression(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
		Section parenthesis = section.subSections.get(section.subSections.size()-1);
		Expression[] arguments = parseArgumentList(source, line, parenthesis, errors);
		Expression function = parseExpression(source, line, section.getSubSection(section.start, parenthesis.start-1), errors);
		return new FunctionCallExp(source, line[section.start].sourceStart, line[section.stop-1].sourceStop, function, arguments);
	}
	
	/** Assumes that the last subsection of section is a bracket section */
	private static Expression parseIndexingExpression(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
		Section brackets = section.subSections.get(section.subSections.size()-1);
		Expression[] arguments = parseArgumentList(source, line, brackets, errors);
		if(arguments.length == 0)
			errors.add("Empty index" + source.getErr(line));
		Expression array = parseExpression(source, line, section.getSubSection(section.start, brackets.start-1), errors);
		return new IndexingExp(source, line[section.start].sourceStart, line[section.stop-1].sourceStop, array, arguments);
	}
	
	private static Expression parseArrayExpression(UnitSource source, Token[] line, Section section, ErrorWrapper errors) {
		Section brackets = section.subSections.get(section.subSections.size()-1);
		Expression[] values = parseArgumentList(source, line, brackets, errors);
		return new ArrayExp(source, line[section.start].sourceStart, line[section.stop-1].sourceStop, values);
	}
	
}
