base ahk;

unit Kernel;

@native("ahk_Kernel_argv");
str[] argv;

@native("ahk_Kernel_print_dec");
func void print(int i) {}
@native("ahk_Kernel_print_hex");
func void print(float f) {}
@native("ahk_Kernel_print_str");
func void print(str s) {}
@native("ahk_Kernel_print_strlen");
func void print(str s, int len) {}
@native("ahk_Kernel_print_bool");
func void print(bool b) {}
@native("ahk_Kernel_print_ln");
func void println() {}

func void println(int i) {
	print(i);
	println();
}

func void println(float f) {
	print(f);
	println();
}

func void println(str s) {
	print(s);
	println();
}

func void println(bool b) {
	print(b);
	println();
}

@native("ker_exit");
func void exit(int exitCode) {}
