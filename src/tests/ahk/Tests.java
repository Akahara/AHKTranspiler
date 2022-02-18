package tests.ahk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.wonder.ahk.AHKTranspiler;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.AsmX64Transpiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.loggers.AnsiLogger;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.systems.process.ProcessUtils;
import fr.wonder.commons.utils.ColorUtils;

public class Tests {
	
	private static final int INVALID_EXIT_CODE = 655244356;
	private static final int LOGGERS_LOG_LEVEL = Logger.LEVEL_WARN;
	
	private static final File testsFolder = new File("tests");
	private static final File defaultManifest = new File(testsFolder, "manifest.txt");
	private static final File outputDir = new File(testsFolder, "out");
	
	private final File testDir;
	private final String testName;
	
	private boolean testPassed = false;
	
	public static void main(String[] args) {
		List<Tests> instances = new ArrayList<>();
		List<Thread> threads = new ArrayList<>();
		for(File dir : testsFolder.listFiles(f -> f.isDirectory() && f.getName().startsWith("test_"))) {
			String testName = dir.getName().substring(5);
			Tests instance = new Tests(dir, testName);
			Thread thread = new Thread(instance::runTest, "TEST:"+testName);
			instances.add(instance);
			threads.add(thread);
			thread.start();
			
			/*
			 * Note: currently, some parts of the x64 transpiler are not thread-safe
			 * so tests cannot be run simultaneously
			 */
			try { thread.join(); } catch (InterruptedException e) { throw new UnreachableException(); }
		}
		for(Thread t : threads) {
			try { t.join(); } catch (InterruptedException e) { throw new UnreachableException(); }
		}
		int successCount = 0;
		for(Tests instance : instances) {
			if(instance.testPassed)
				successCount++;
		}
		System.out.println(String.format("\nRan %d tests, %d successes, %d failures", instances.size(), successCount, instances.size()-successCount));
		for(Tests instance : instances) {
			if(!instance.testPassed)
				System.out.println("- Failed " + instance.testName);
		}
	}
	
	private Tests(File testDir, String testName) {
		this.testDir = testDir;
		this.testName = testName;
	}
	
	@SuppressWarnings("unused")
	private void runTest() {
		Logger compilerLogger = new AnsiLogger(testName, LOGGERS_LOG_LEVEL);
		File logFile = new File(testDir, "latest.log");
		File testCaseFile = new File(testDir, "expected.output");
		Transpiler transpiler = new AsmX64Transpiler(compilerLogger);
		
		ByteArrayOutputStream compilerOutputBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream processOutputBuffer = new ByteArrayOutputStream();
		int processExitCode = INVALID_EXIT_CODE;
		
		compilerLogger.redirectOut(new PrintStream(compilerOutputBuffer));
		
		TestCaseOutput expectedOutput;
		try {
			FilesUtils.write(logFile, "");
			expectedOutput = TestCaseOutput.parseFromFile(testCaseFile);
		} catch (IOException | NumberFormatException e) {
			System.err.println("=========== Invalid test '" + testName + "': " + e);
			return;
		}

		long compilationTimestamp = System.currentTimeMillis();
		long compilationDuration = 0;
		boolean compiledSuccessfully = false;
		long executionTimestamp = 0;
		long executionDuration = 0;
		boolean ranSuccessfully = false;
		
		try {
			File manifestFile = FilesUtils.firstAlternative(new File(testDir, "manifest.txt"), defaultManifest);
			File testOutputDir = FilesUtils.createDir(outputDir, testDir.getName());
			ProjectHandle project = runUnsafe(compilerLogger, () ->
				AHKTranspiler.createProject(testDir, manifestFile));
			LinkedHandle linkedProject = runUnsafe(compilerLogger, () -> 
				project
					.compile(new ErrorWrapper("Compile error"))
					.link(new ErrorWrapper("Linking error")));
			ExecutableHandle executableProject = runUnsafe(compilerLogger, () ->
				transpiler.exportProject(linkedProject, testOutputDir, new ErrorWrapper("Unable to export", true)));
			compilationDuration = System.currentTimeMillis() - compilationTimestamp;
			compiledSuccessfully = true;
			File execFile = new File(testOutputDir, "process");
			ProcessBuilder pb = new ProcessBuilder("./process arg1 arg2".split(" ")).directory(testOutputDir);
			executionTimestamp = System.currentTimeMillis();
			Process process = pb.start();
			if(process != null) {
				if(!process.waitFor(3, TimeUnit.SECONDS)) {
					process.destroyForcibly();
					processOutputBuffer.write("Process exceeded time bounds\n".getBytes());
				} else {
					processExitCode = process.exitValue();
					ranSuccessfully = true;
				}
				executionDuration = System.currentTimeMillis() - executionTimestamp;
			}
			process.getInputStream().transferTo(processOutputBuffer);
			process.getErrorStream().transferTo(processOutputBuffer);
		} catch (TestError e) {
			// do nothing, let the finally block to execute
		} catch (Throwable e) {
			compilerLogger.merr(e, "Unexpected error in tests");
		} finally {
			String processOutput = processOutputBuffer.toString();
			String compilerOutput = compilerOutputBuffer.toString();
			
			StringBuilder fullLog = new StringBuilder();
			StringBuilder errorLog = new StringBuilder();
			
			boolean testSucceeded = true;
			
			// validate compiler & process output
			testSucceeded &= expectedOutput.checkCompilerOutput(errorLog, compilerOutput, compiledSuccessfully);
			testSucceeded &= expectedOutput.checkProcessOutput(errorLog, processOutput, processExitCode, ranSuccessfully);
			
			this.testPassed = testSucceeded;
			
			if(testSucceeded) {
				fullLog.insert(0, "=========== Test '" + testName + "' succeeded ===========\n");
			} else {
				fullLog.insert(0, "===========  Test '" + testName + "' failed  ===========\n");
			}
			fullLog.append(errorLog);
			int processOutputInsertionPosition = fullLog.length();
			if(ranSuccessfully) {
				String signal = ProcessUtils.getErrorSignal(processExitCode);
				if(signal == null) signal = "";
				fullLog.append(String.format("Exited with status %d = 0x%x %s\n", processExitCode, processExitCode, signal));
				fullLog.append(String.format("Compilation ran for %dms\n", compilationDuration));
				fullLog.append(String.format("Executable ran for  %dms\n", executionDuration));
			}
			fullLog.append("===========/     '" + testName + "'          /===========\n");
			
			System.out.print(fullLog.toString());
			
			try {
				fullLog.insert(processOutputInsertionPosition,
						"=========== Process output ===========\n" +
						processOutput + "\n" +
						(processOutput.endsWith("\n") ? "" : "(no newline at end)\n") +
						"===========/Process output/===========\n");
				fullLog.insert(processOutputInsertionPosition,
						"=========== Compiler output ===========\n" +
						compilerOutput +
						"===========/Compiler output/===========\n");
				ColorUtils.stripAnsi(fullLog);
				FilesUtils.write(logFile, fullLog.toString());
			} catch (IOException e) {
				System.err.println("Could not write test log for '" + testName + "'");
			}
		}
	}
	
	private static class TestError extends Exception {

		private static final long serialVersionUID = -6677001061567914045L;
		
	}
	
	private static interface UnsafeRunnable<T> {
		
		public T run() throws WrappedException, Exception;
		
	}
	
	private static <T> T runUnsafe(Logger logger, UnsafeRunnable<T> runnable) throws TestError {
		try {
			return runnable.run();
		} catch (WrappedException e) {
			e.errors.dump(logger);
			throw new TestError();
		} catch (IOException e) {
			logger.merr(e, "IO compiler error");
			throw new TestError();
		} catch (Throwable t) {
			logger.merr(t, "Unexpected error");
			throw new TestError();
		}
	}
	
}
