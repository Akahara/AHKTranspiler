base fr.wonder.main;

import ahk.Kernel;
import ahk.Strings;

unit Main;

global func int main() {
	Kernel.out << 2.5 + "str" + true + 3;
}
