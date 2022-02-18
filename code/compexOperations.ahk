base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias Func = func float(float);
alias Fg = func float();

global func int main() {
	Kernel.out << 3^2 << "\n";
	Func id = (float x):float => x;
	Fg f5 = ():float => 2;
	Fg f3 = ():float => 3.5;
	Kernel.out << id(3) << "\n";
	Fg ff = f3%f5;
	float f = ff();
	Kernel.out << f << "\n";
	
	// variables are used to avoid literal optimization
	float e1 = 3;
	Kernel.out << e1^8 << "\n";
	int i1 = 5;
	Kernel.out << i1^4 << "\n";
	
	return 0;
}
