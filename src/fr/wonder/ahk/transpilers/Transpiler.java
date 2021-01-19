package fr.wonder.ahk.transpilers;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.handles.AHKExecutableHandle;
import fr.wonder.ahk.handles.AHKTranspilableHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public interface Transpiler {
	
	public String getName();
	public AHKExecutableHandle exportProject(AHKTranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException;
	public void exportAPI(AHKTranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException;
	public int runProject(AHKExecutableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException;
	
}
