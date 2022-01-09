base ahk;

unit Kernel;

global struct KernelOut {
  operator kprinti : KernelOut << int = KernelOut;
  operator kprintf : KernelOut << float = KernelOut;
  operator kprints : KernelOut << str = KernelOut;
  operator kprintb : KernelOut << bool = KernelOut;
}

global KernelOut out;

@native("ahk_Kernel_argv");
global str[] argv;

@native("ahk_Kernel_print_dec");
global func void printi(int i) {}
global func KernelOut kprinti(KernelOut o, int i) { printi(i); return o; }
@native("ahk_Kernel_print_float");
global func void printf(float f) {}
global func KernelOut kprintf(KernelOut o, float f) { printf(f); return o; }
@native("ahk_Kernel_print_str");
global func void print(str s) {}
global func KernelOut kprints(KernelOut o, str s) { print(s); return o; }
@native("ahk_Kernel_print_strlen");
global func void prints(str s, int len) {}
@native("ahk_Kernel_print_bool");
global func void printb(bool b) {}
global func KernelOut kprintb(KernelOut o, bool b) { printb(b); return o; }
@native("ahk_Kernel_print_ln");
global func void printnl() {}

global func void printlni(int i) {
	printi(i);
	printnl();
}

global func void printlnf(float f) {
	printf(f);
	printnl();
}

global func void println(str s) {
	print(s);
	printnl();
}

global func void printlnb(bool b) {
	printb(b);
	printnl();
}

@native("ker_exit");
global func void exit(int exitCode) {}

@native("ahk_Kernel_sleep");
global func void sleep(int seconds) {}

@native("ahk_Kernel_nanosleep");
global func void nanosleep(int nanos) {}

@native("mem_run_gc");
global func void runGarbageCollector() {}
