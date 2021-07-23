package fr.wonder.ahk.compiler.types;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.CallablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
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
	 * Returns the callable that bests suits the given argument types.
	 * 
	 * <p>
	 * If multiple callable have a similar declaration, given different arguments
	 * will result in different calls: 
	 * 
	 * <blockquote><pre>
	 * func foo(int a)
	 * func foo(float a)
	 * 
	 * foo(3);    // the first foo is called with an int
	 * foo(true); // the first foo is called with a bool implicitely casted to an int
	 * </pre></blockquote>
	 * 
	 * <p>
	 * Multiple implicit casts can be made but only the callable with the lower
	 * number of casts will be retained. If multiple functions could be called with
	 * the same number of casts or if no function can be called at all an error is
	 * reported.
	 * 
	 * @param callables      an array of {@link FunctionPrototype} or
	 *                       {@link ConstructorPrototype}
	 * @param args           the arguments given to the yet-unknown function
	 * @param callingElement the element calling a function or constructor, used to
	 *                       print meaningful errors
	 * @return a function or constructor that best suits the given arguments
	 */
	public static <T extends CallablePrototype> T searchMatchingCallable(T[] callables, VarType[] args, SourceElement callingElement, ErrorWrapper errors) {
		int validFuncConversionCount = Integer.MAX_VALUE;
		T validFunc = null;
		boolean multipleMatches = false;
		for(T func : callables) {
			if(func.getArgumentTypes().length != args.length)
				continue;
			int convertionCount = getMinimumConversionCount(func.getArgumentTypes(), args);
			if(convertionCount == -1)
				continue;
			if(convertionCount == validFuncConversionCount) {
				validFunc = null;
				multipleMatches = true;
			} else if(convertionCount < validFuncConversionCount) {
				validFunc = func;
				validFuncConversionCount = convertionCount;
				multipleMatches = false;
			}
		}
		if(multipleMatches) {
			errors.add("Multiple functions match given parameters:" + callingElement.getErr());
		} else if(validFunc == null) {
			errors.add("No matching function match given parameters:" + callingElement.getErr());
		}
		return validFunc;
	}

}
