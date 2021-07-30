base ahk;

unit Kernel;

@native("ahk_Kernel_argv");
str[] argv;

@native("ahk_Kernel_print_dec");
func void printi(int i) {}
@native("ahk_Kernel_print_hex");
func void printf(float f) {}
@native("ahk_Kernel_print_str");
func void print(str s) {}
@native("ahk_Kernel_print_strlen");
func void prints(str s, int len) {}
@native("ahk_Kernel_print_bool");
func void printb(bool b) {}
@native("ahk_Kernel_print_ln");
func void printnl() {}

func void printlni(int i) {
	printi(i);
	printnl();
}

func void printlnf(float f) {
	printf(f);
	printnl();
}

func void println(str s) {
	print(s);
	printnl();
}

func void printlnb(bool b) {
	printb(b);
	printnl();
}

@native("ker_exit");
func void exit(int exitCode) {}
