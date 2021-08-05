base ahk;

unit Kernel;

@native("ahk_Kernel_argv");
global str[] argv;

@native("ahk_Kernel_print_dec");
global func void printi(int i) {}
@native("ahk_Kernel_print_hex");
global func void printf(float f) {}
@native("ahk_Kernel_print_str");
global func void print(str s) {}
@native("ahk_Kernel_print_strlen");
global func void prints(str s, int len) {}
@native("ahk_Kernel_print_bool");
global func void printb(bool b) {}
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
