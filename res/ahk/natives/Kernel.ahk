base ahk;

import ahk.Test;

unit Kernel;

@native("ahk_Kernel_print_dec");
func void print(int i) {}
@native("ahk_Kernel_print_hex");
func void print(float f) {}
@native("ahk_Kernel_print_str");
func void print(str s) {}
@native("ahk_Kernel_print_strlen");
func void print(str s, int len) {}
@native("ahk_Kernel_print_hex");
func void print(bool b) {}
@native("ahk_Kernel_print_ln");
func void println() {}

@native("ker_exit");
func void exit(int exitCode) {}

func void test() {
	Test.test();
}
