base fr.wonder.main;

import ahk.Kernel;
import ahk.Strings;

unit Main;

global func int main() {
	str s;
	Kernel.printlni(6);
	Kernel.printlni(0);
	s = Strings.int2str(1234);
	Kernel.println(s);
	s = Strings.bool2str(true);
	Kernel.println(s);
	Kernel.printlnb(true);
	Kernel.printlnb(false);
	s = Strings.int2hexstr(255);
	Kernel.println(s);
	Kernel.out << "abc " << 546 << "\n";
	Kernel.printlnf(3.4);
	Kernel.printlnf(3.2567);
	Kernel.printlnf(5.0);
	Kernel.printlnf(0);
	Kernel.printlnf(0.256);
	Kernel.printlnf(-0.256);
	Kernel.out << 42.42 << "\n";
	s = Strings.float2str(4.5);
	Kernel.out << s << "\n";
}
