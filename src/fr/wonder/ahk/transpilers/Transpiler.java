package fr.wonder.ahk.transpilers;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.commons.exceptions.AssertionException;
import fr.wonder.commons.exceptions.ErrorWrapper;

public interface Transpiler {
	
	public String getName();
	public void exportProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException, AssertionException;
	public void exportAPI(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException, AssertionException;
	public int runProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException, AssertionException;
	
}
