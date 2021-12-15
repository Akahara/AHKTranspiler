base ahk;

unit Streams;

alias StreamInput = func int();
alias StreamOutput = func void(int);

global int eof = 5;

global struct Stream {
	
	global StreamInput in;
	global StreamOutput out;
	
	constructor(StreamInput in, StreamOutput out);
	
}

global func Stream openFile(str path) {
	StreamInput in = ():int=>0;
	StreamOutput out;
	
	return Stream(in, out);
}
