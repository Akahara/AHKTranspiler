package fr.wonder.ahk.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Useful class for error handling, a common use of an error wrapper is as follow:
 * <blockquote><pre>
 * void foo() {
 *   ErrorWrapper errors = new ErrorWrapper("Something happened");
 *   try {
 *     ...
 *     bar(..., errors.subErrors("Unable to bar!"));
 *     errors.assertNoErrors();
 *     ...
 *     // possible reuse of #errors
 *   } catch (CompilationError x) {
 *     errors.dump();
 *   }
 * }
 * 
 * void bar(..., ErrorWrapper errors) {
 *   ...
 *   if(thingsWentWrong)
 *     errors.add("Things went wrong!");
 *   ...
 * }
 * </pre></blockquote>
 */
public class ErrorWrapper {
	
	private static boolean logTraces = true;
	
	private final String header;
	private final List<String> errors = new ArrayList<>();
	
	private final List<ErrorWrapper> subErrors = new ArrayList<>();
	
	public ErrorWrapper(String header) {
		this.header = header;
	}
	
	public void add(String s) {
		for(String l : s.split("\n"))
			errors.add(l);
		if(logTraces) {
			String trace = "";
			for(StackTraceElement t : new Exception().getStackTrace())
				trace += "("+t.getFileName()+":"+t.getLineNumber()+") ";
			errors.add(trace);
		}
	}

	public void addWarning(String warn) {
		// TODO fix
		System.out.println("warn: " + warn);
	}

	public void trace(String s) {
		StringWriter writer = new StringWriter();
		new Error().printStackTrace(new PrintWriter(writer));
		add(s + writer.toString());
	}
	
	public ErrorWrapper subErrrors(String header) {
		ErrorWrapper sub = new ErrorWrapper(header);
		subErrors.add(sub);
		return sub;
	}
	
	public boolean noErrors() {
		if(!errors.isEmpty())
			return false;
		for(ErrorWrapper sub : subErrors)
			if(!sub.noErrors())
				return false;
		return true;
	}
	
	public void dump() {
		dump(0);
	}
	
	private void dump(int level) {
		if(noErrors())
			return;
		System.err.println("| ".repeat(level) + header+":");
		for(String e : errors)
			System.err.println("| ".repeat(level+1) + e);
		for(ErrorWrapper sub : subErrors)
			sub.dump(level+1);
	}
	
	public void assertNoErrors() throws CompilationError {
		if(!noErrors())
			throw new CompilationError();
	}
	
}
