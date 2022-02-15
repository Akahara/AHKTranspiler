package fr.wonder.ahk.compiler.types;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.commons.exceptions.ErrorWrapper;

/**
 * Utility class that helps to search through multiple functions or constructors
 * to see which can be called with what arguments.
 */
public class FunctionArguments {

	/**
	 * Returns whether the provided types exactly match the arguments.
	 */
	public static boolean matchNoConversions(VarType[] args, VarType[] provided) {
		return Arrays.equals(args, provided);
	}
	
	/**
	 * Returns the minimum number of implicit conversions that must be made
	 * before the provided types match the arguments.
	 */
	public static int getMinimumConversionCount(VarType[] args, VarType[] provided) {
		int count = 0;
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals(provided[i]))
				continue;
			else if(ConversionTable.canConvertImplicitely(provided[i], args[i]))
				count++;
			else
				return -1;
		}
		return count;
	}
	
	/**
	 * <p>
	 * Returns the constructor that bests suits the given argument types.
	 * 
	 * <p>
	 * If multiple constructors have a similar declaration, different arguments will
	 * result in different calls.
	 * 
	 * <p>
	 * Multiple implicit casts can be made but only the constructor with the lower
	 * number of casts will be retained. If multiple constructors could be called
	 * with the same number of casts or if no function can be called at all an error
	 * is reported.
	 * 
	 * @param constructors   an array of {@link ConstructorPrototype}
	 * @param args           the arguments given to the yet-unknown constructor
	 * @param callingElement the element calling a constructor, used to print
	 *                       meaningful errors
	 * @return a constructor that best suits the given arguments, or null if none
	 *         match given arguments
	 */
	public static ConstructorPrototype searchMatchingConstructor(
			ConstructorPrototype[] constructors, VarType[] args,
			SourceElement callingElement, ErrorWrapper errors) {
		
		int validFuncConversionCount = Integer.MAX_VALUE;
		ConstructorPrototype validFunc = null;
		boolean multipleMatches = false;
		for(ConstructorPrototype con : constructors) {
			if(con.argTypes.length != args.length)
				continue;
			int convertionCount = getMinimumConversionCount(con.argTypes, args);
			if(convertionCount == -1)
				continue;
			if(convertionCount == validFuncConversionCount) {
				validFunc = null;
				multipleMatches = true;
			} else if(convertionCount < validFuncConversionCount) {
				validFunc = con;
				validFuncConversionCount = convertionCount;
				multipleMatches = false;
			}
		}
		if(multipleMatches) {
			errors.add("Multiple constructor matching given parameters:" + callingElement.getErr());
		} else if(validFunc == null) {
			errors.add("No constructor matching given parameters:" + callingElement.getErr());
		}
		return validFunc;
	}

}
