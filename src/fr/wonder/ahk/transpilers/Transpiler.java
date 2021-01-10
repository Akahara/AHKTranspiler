package fr.wonder.ahk.transpilers;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.ahk.utils.ErrorWrapper;

public interface Transpiler {
	
	public String getName();
	public void exportProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException;
	public void runProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException;
	
}
