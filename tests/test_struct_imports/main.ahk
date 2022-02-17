base fr.wonder.main;

import ahk.Kernel;
import fr.wonder.main.A;
import fr.wonder.main.B;
import fr.wonder.main.C;

unit Main;

global func int main() {
	A a = null;
	C c = null;
	Kernel.printlni(a.i);
	Kernel.printlni(B.testCase(a));
	Kernel.printlni(c.a.i);
}
