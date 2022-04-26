base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias Func = func float(float);
alias Fg = func float();
alias Fh = func str();

global func int main() {
	Kernel.out << "Expected: 9 3. 2. 6561. 625.\n";
	
	Kernel.out << 3^2 << "\n";
	Func id = (float x):float => x;
	Fg f5 = ():float => 5;
	Fg f3 = ():float => 3;
	Kernel.out << id(3) << "\n";
	Fg ff = f5%f3;
	float f = ff();
	Kernel.out << f << "\n";
	// variables are used to avoid literal optimization
	float e3 = 3;
	Kernel.out << e3^8 << "\n";
	int i5 = 5;
	Kernel.out << i5^4 << "\n";
	str sab = "ab";
	
	Kernel.out << "Expected: T T T\n";
	
	Kernel.out << (i5 < 6) << "\n";
	Kernel.out << (e3 < 4) << "\n";
	Kernel.out << (f3<f5)() << "\n";
	
	Kernel.out << "Expected: F T T\n";
	
	Kernel.out << (e3>=4) << "\n";
	Kernel.out << (f3>=f3)() << "\n";
	Kernel.out << (f5>f3)() << "\n";
	
	Kernel.out << "Expected: F F T\n";
	
	Kernel.out << (i5 === 4) << "\n";
	Kernel.out << (sab === "ab") << "\n";
	Kernel.out << (sab === sab) << "\n";
	
	Kernel.out << "Expected: T T F\n";
	
	Kernel.out << (sab == "ab") << "\n";
	Kernel.out << ("a"+"b" == "ab") << "\n";
	Kernel.out << ("abc" == "ab") << "\n";
	
	Kernel.out << "Expected: abcd T F\n";
	
	Fh h = ():str => "abcd";
	Kernel.out << h() << "\n";
	Kernel.out << (h == "ab" + "cd")() << "\n";
	Kernel.out << (h == "cd")() << "\n";
	
	Kernel.out << "Expected: 7 1" << "\n";
	
	Kernel.out << i5 | 3 << "\n";
	Kernel.out << i5 & 11 << "\n";
	
	return 0;
}
