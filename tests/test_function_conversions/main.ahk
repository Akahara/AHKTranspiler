base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias FI = func int();
alias FF = func float();

alias GI = func int(int);
alias GF = func int(float);
alias HF = func float(int);

global func int main() {
	GI gi = (int x):int => x+3;
	GF gf = (float x):int => int:(x);
	
	FI fi = ():int => 3;
	FF ff = ():float => 4.6;
	
	Kernel.out << "Expected: 3.0\n";
	FF fiAsff = fi;
	float f = fiAsff();
	Kernel.out << f << "\n";
	
	Kernel.out << "Expected: 7\n";
	GF giAsgf = GF:(gi);
	int i = giAsgf(4);
	Kernel.out << i << "\n";
	
	Kernel.out << "Expected: 9.0\n";
	HF ggg = (float x):int => int:(x+3.5);
	float j = ggg(6);
	Kernel.out << j << "\n";
	
	Kernel.out << "Expected ~7.6\n";
	//FF fiPlusff = fi+ff;
	FF fiPlusff = FF:(fi)+ff;
	float g = fiPlusff();
	Kernel.out << g << "\n";
	
	return 0;
}
