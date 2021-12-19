base ahk;

import ahk.Kernel;

unit Streams;

global int mode_read = 1;
global int mode_write = 2;
global int eof = 5;

global struct Stream {
	
	int fd;
	int openMode;
	bool closed;
	
	constructor(int fd, int openMode);
	
}

@native("ahk_Streams_openfile");
func int openFileSyscall(str path) {}
@native("ahk_Streams_read");
func int readSyscall(int fd) {}
//@native("ahk_Streams_write");
//func int writeSyscall(int fd, int byte) {}
@native("ahk_Streams_close");
func int closeSyscall(int fd) {}

global func Stream openFile(str path) {
	int fd = openFileSyscall(path);
	Kernel.out << fd << "\n";
	return Stream(fd, mode_read);
}

global func int read(Stream stream) {
	if(stream.closed || stream.openMode != mode_read) // TODO use & once it is implemented
		return eof;
	return readSyscall(stream.fd);
}

global func void close(Stream stream) {
	if(stream.closed) return;
	stream.closed = true;
	closeSyscall(stream.fd);
}
