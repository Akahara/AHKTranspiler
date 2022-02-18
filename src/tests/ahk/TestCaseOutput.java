package tests.ahk;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.utils.ColorUtils;

public abstract class TestCaseOutput {
	
	public static TestCaseOutput parseFromFile(File file) throws IOException {
		String lines = FilesUtils.read(file);
		while(lines.matches("#.*\n(?:.|\n)*")) // skip comments
			lines = lines.substring(lines.indexOf('\n')+1);
		int ln = lines.indexOf('\n');
		String firstLine = lines.substring(0, ln == -1 ? lines.length() : ln);
		String remaining = ln == -1 ? "" : lines.substring(ln+1);
		
		if(firstLine.matches("normal \\d+")) {
			return normalOutputCase(Integer.parseInt(firstLine.substring(7)), remaining);
		} else if(firstLine.matches("compilation_error")) {
			return compilationErrorCase(remaining.split("\n"));
		} else {
			throw new IOException("Unknown test case '" + firstLine + "'");
		}
	}
	
	private static TestCaseOutput normalOutputCase(int expectedExitCode, String expectedOutput) {
		return new TestCaseOutput() {
			@Override
			public boolean checkCompilerOutput(StringBuilder log, String compilerOutput, boolean compiledSuccessfully) {
				if(!compiledSuccessfully) {
					int split = 0;
					for(int i = 0; i < 5 && split != -1; i++)
						split = compilerOutput.indexOf('\n', split+1);
					log.append("Compiler error:\n");
					if(split == -1) {
						log.append(compilerOutput + ColorUtils.ANSI.RESET);
					} else {
						log.append(compilerOutput.substring(0, split) + ColorUtils.ANSI.RESET);
						log.append("...\n");
					}
				}
				return compiledSuccessfully;
			}
			@Override
			public boolean checkProcessOutput(StringBuilder log, String processOutput, int exitCode, boolean ranSuccessfully) {
				boolean validOutput = true;
				if(!ranSuccessfully) {
					log.append("Process did not run successfully\n");
					validOutput = false;
				}
				if(exitCode != expectedExitCode) {
					log.append("Process ended with an unexpected exit status: " + exitCode + " (expected " + expectedExitCode + ")\n");
					validOutput = false;
				}
				if(!processOutput.equals(expectedOutput)) {
					log.append("Process ended with an unexpected output\n");
					validOutput = false;
				}
				return validOutput;
			}
		};
	}
	
	private static TestCaseOutput compilationErrorCase(String[] errorLines) {
		return new TestCaseOutput() {
			@Override
			public boolean checkCompilerOutput(StringBuilder log, String compilerOutput, boolean compiledSuccessfully) {
				boolean validOutput = true;
				if(compiledSuccessfully) {
					log.append("Compiler was not supposed to compile successfully\n");
					validOutput = false;
				}
				for(String s : errorLines) {
					if(!Pattern.compile(s).matcher(compilerOutput).find()) {
						log.append("Compiler output does not contain '" + s + "'\n");
						validOutput = false;
					}
				}
				return validOutput;
			}
		};
	}
	
	public boolean checkProcessOutput(StringBuilder log, String processOutput, int exitCode, boolean ranSuccessfully) { return true; }
	public boolean checkCompilerOutput(StringBuilder log, String compilerOutput, boolean compiledSuccessfully) { return true; }
	
}
