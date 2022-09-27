base ahk;

unit Kernel;

int stdOut = 1;

global struct KernelOut {
  operator kprinti : KernelOut << int = KernelOut;
  operator kprintf : KernelOut << float = KernelOut;
  operator kprints : KernelOut << str = KernelOut;
  operator kprintb : KernelOut << bool = KernelOut;
}

global KernelOut out;
global str endl = "\n";

@native("ahk_Kernel_argv");
global str[] argv;

@native("ahk_Strings_print_int2str");
global func void printi(int fd, int i) {}
global func KernelOut kprinti(KernelOut o, int i) { printi(stdOut, i); return o; }

@native("ahk_Strings_print_float2str");
global func void printf(int fd, float f) {}
global func KernelOut kprintf(KernelOut o, float f) { printf(stdOut, f); return o; }

@native("ahk_Strings_print_str");
global func void print(int fd, str s) {}
global func KernelOut kprints(KernelOut o, str s) { print(stdOut, s); return o; }

@native("ahk_Strings_print_bool2str");
global func void printb(int fd, bool b) {}
global func KernelOut kprintb(KernelOut o, bool b) { printb(stdOut, b); return o; }

@native("ahk_Strings_print_ln");
global func void printnl(int fd) {}
@native("ahk_Strings_print_unnormalizedstr");
global func void prints(int fd, str s, int len) {}

global func void printlni(int i) {
	printi(stdOut, i);
	printnl(stdOut);
}

global func void printlnf(float f) {
	printf(stdOut, f);
	printnl(stdOut);
}

global func void println(str s) {
	print(stdOut, s);
	printnl(stdOut);
}

global func void printlnb(bool b) {
	printb(stdOut, b);
	printnl(stdOut);
}

@native("ker_exit");
global func void exit(int exitCode) {}

@native("ahk_Kernel_sleep");
global func void sleep(int seconds) {}

@native("ahk_Kernel_nanosleep");
global func void nanosleep(int nanos) {}

@native("mem_run_gc");
global func void runGarbageCollector() {}
