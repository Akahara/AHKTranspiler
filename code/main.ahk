
base fr.wonder.main;

import ahk.Kernel;
import ahk.Streams;

unit Main;

global func int main() {
	float i = 3. % 1.4;
	
	Kernel.out << i << "\n";
	
	return 5;
}
