
base fr.wonder.main;

import ahk.Kernel;
import ahk.Streams;

unit Main;

global func int main() {
	Kernel.println("------ Expected: this file's code");
	
	Stream stream = Streams.openFile("main.ahk");
	
	int i;
	bool f = true;
	while(f || (i != 0 && i != Streams.eof)) {
		i = stream.in();
		Kernel.out << i << "\n";
		f = false;
	}
	
	return 5;
}
