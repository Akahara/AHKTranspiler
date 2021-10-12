base fr.wonder.main;

import ahk.Kernel;

unit Main;

global func int main() {
	problem4();
}

func void problem1() {
	int s = 0;
	for(int i = 1..1000) {
		if(i%3 == 0 || i%5 == 0)
			s += i;
	}
	Kernel.out << s;
}

func void problem2() {
	int a = 1;
	int b = 1;
	int s = 0;
	while(b < 4000000) {
		if(b % 2 == 0)
			s += b;
		int c = a+b;
		a = b;
		b = c;
	}
	Kernel.out << s;
}

func void problem3() {
	int s = 600851475143;
	while(true) {
		int p = problem3smallestPrime(s);
		if(p >= s) {
			Kernel.out << p;
			return;
		} else {
			s = s/p;
		}
	}
}

func int problem3smallestPrime(int i) {
	for(int j = 2 : j < i : j++) {
		if(i%j == 0)
			return j;
	}
	return i;
}

func void problem4() {
	for(int i1 : 9..-1..0) {
		for(int i2 : 9..-1..0) {
			for(int i3 : 9..-1..0) {
				int x = 100001*i1 + 10010*i2 + 1100*i3;
				for(int d1 : 100..999) {
					if(x % d1 == 0) {
						for(int d2 : 100..999) {
							if(d1*d2 == x) {
								Kernel.out << x << " is a palindrome and x=" << d1 << "x" << d2;
								return;
							}
						}
					}
				}
			}
		}
	}
}