package fr.wonder.ahk.transpilers.asm_x64.natives;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.AsmOperationWriter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.types.Tuple;

public class ProcessFiles {
	
	private static final Map<String, FileWriter> AHK_LIB = new HashMap<>();
	
	private static interface FileWriter {
		
		String writeFile(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException;
		
	}
	
	static {
		AHK_LIB.put("ahk.Kernel", (h, d, e) -> copyNative(d, "asm/natives/kernel.fasm", "natives/kernel.asm"));
	}
	
	public static String[] writeFiles(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException {
		List<String> files = new ArrayList<>();
		
		files.add(copyNative(dir, "asm/natives/intrinsic.fasm", "intrinsic.asm"));
		files.add(writeOSIntrinsic(handle, dir, errors));
		files.add(writeEntryPoint(handle, dir, errors));
		files.add(writeClosures(dir));
		files.add(copyNative(dir, "asm/natives/memory.fasm", "natives/memory.asm"));
		files.add(copyNative(dir, "asm/natives/errors.fasm", "natives/errors.asm"));
		files.add(copyNative(dir, "asm/natives/values.fasm", "natives/values.asm"));
		files.add(copyNative(dir, "asm/natives/lib/streams.fasm", "natives/lib/streams.asm"));
		
		for(Entry<String, FileWriter> unit : AHK_LIB.entrySet()) {
			files.add(unit.getValue().writeFile(handle, dir, errors));
		}
		
		files.removeIf(Objects::isNull);
		
		return files.toArray(String[]::new);
	}
	
	private static String readNative(String path) throws IOException {
		InputStream in = ProcessFiles.class.getResourceAsStream("/"+path);
		if(in == null) {
			throw new IllegalStateException("Missing source file " + path);
		} else {
			return new String(in.readAllBytes());
		}
	}
	
	private static String formatNative(String path, String... args) throws IOException {
		if(args.length % 2 != 0)
			throw new IllegalArgumentException();
		String source = readNative(path);
		for(int i = 0; i < args.length; i += 2)
			source = source.replaceAll(args[i], args[i+1]);
		return source;
	}
	
	private static String writeFile(File dir, String source, String name) throws IOException {
		File f = new File(dir, name);
		f.getParentFile().mkdirs();
		f.createNewFile();
		FilesUtils.write(f, source);
		return name;
	}
	
	private static String copyNative(File dir, String path, String name) throws IOException {
		return writeFile(dir, readNative(path), name);
	}
	
	private static String writeOSIntrinsic(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException {
		OSInstrinsic osInstrinsic = OSInstrinsic.getOS(handle.manifest.BUILD_TARGET);
		if(osInstrinsic == null) {
			errors.add("Unknown target operating system: " + handle.manifest.BUILD_TARGET);
			return null;
		}
		return copyNative(dir, "asm/natives/osintrinsic/" + osInstrinsic.toString().toLowerCase() + ".fasm", "osintrinsic.asm");
	}
	
	private static String writeEntryPoint(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException {
		String initializationFunctionsExterns = "";
		String initializationFunctionsCalls = "";
		for(Unit u : handle.units) {
			String reg = RegistryManager.getUnitInitFunctionRegistry(u);
			initializationFunctionsExterns += "extern " + reg + "\n";
			initializationFunctionsCalls += "call " + reg + "\n  ";
		}
		
		String source = formatNative("asm/natives/entry_point.fasm",
				"&entry_point", RegistryManager.getGlobalRegistry(handle.manifest.entryPointFunction.getPrototype()),
				"&units_initialization_externs", initializationFunctionsExterns,
				"&units_initialization_calls", initializationFunctionsCalls);
		return writeFile(dir, source, "natives/entry_point.asm");
	}
	
	private static String writeClosures(File dir) throws IOException {
		Tuple<String, String> closures = AsmOperationWriter.writeOperationsAsClosures();
		String source = formatNative("asm/natives/closures.fasm",
				"&native_operation_closures_globals", closures.a,
				"&native_operation_closures", closures.b);
		return writeFile(dir, source, "natives/closures.asm");
	}
	
}
