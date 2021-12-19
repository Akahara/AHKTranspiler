
base fr.wonder.main;

import ahk.Kernel;
import ahk.Streams;

unit Main;

global func int main() {
	Kernel.println("------ Expected: this file's code");
	
	Stream stream = Streams.openFile("main.ahk");
	
	Kernel.out << Streams.read(stream) << "\n";
	Kernel.sleep(10);
	Streams.close(stream);
	Kernel.out << "closed\n";
	Kernel.sleep(5);
	
	return 5;
}
