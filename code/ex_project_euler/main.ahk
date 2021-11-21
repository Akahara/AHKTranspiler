base fr.wonder.main;

import ahk.Kernel;

unit Main;

global func int main() {
	problem8();
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

func void problem5() {
	Kernel.out << smallestDivisible(20);
}

func int smallestDivisible(int max) {
	int[] primeFactors = [:max];
	for(int i = 1 : i < max : i++) {
		int[] pf = problem5primeFactors(i, max);
		for(int j = 0 : j < max : j++) {
			if(primeFactors[j] < pf[j])
				primeFactors[j] = pf[j];
		}
	}
	int smallest = 1;
	for(int i = 0 : i < max : i++) {
		while(primeFactors[i] > 0) {
			smallest *= i;
			primeFactors[i]--;
		}
	}
	return smallest;
}

func int[] problem5primeFactors(int x, int max) {
	int[] pf = [:max];
	int currentPrime = 2;
	while(x > 1) {
		while(x != 1 && x % currentPrime == 0) {
			x /= currentPrime;
			pf[currentPrime]++;
		}
		currentPrime++;
	}
	return pf;
}

func void problem6() {
	int n = 100;
	int sum = n*(n+1)/2;
	int sumSquared = sum*sum;
	int squaresSum = n*(n+1)*(2*n+1)/6;
	Kernel.out
		<< "SumSquared= " << sumSquared << "\n"
		<< "SquaresSum= " << squaresSum << "\n"
		<< "Dif= " << sumSquared - squaresSum << "\n";
}

func void problem7() {
	int p = 0;
	int i = 2;
	while(p < 10001) {
		if(problem3smallestPrime(i) == i) {
			p++;
		}
		i++;
	}
	Kernel.out << "Prime no" << p << " = " << i-1 << "\n";
}

func void problem8() {
	int[] bignum = [ 
7,3,1,6,7,1,7,6,5,3,1,3,3,0,6,2,4,9,1,9,2,2,5,1,1,9,6,7,4,4,2,6,5,7,4,7,4,2,3,5,5,3,4,9,1,9,4,9,3,4,
9,6,9,8,3,5,2,0,3,1,2,7,7,4,5,0,6,3,2,6,2,3,9,5,7,8,3,1,8,0,1,6,9,8,4,8,0,1,8,6,9,4,7,8,8,5,1,8,4,3,
8,5,8,6,1,5,6,0,7,8,9,1,1,2,9,4,9,4,9,5,4,5,9,5,0,1,7,3,7,9,5,8,3,3,1,9,5,2,8,5,3,2,0,8,8,0,5,5,1,1,
1,2,5,4,0,6,9,8,7,4,7,1,5,8,5,2,3,8,6,3,0,5,0,7,1,5,6,9,3,2,9,0,9,6,3,2,9,5,2,2,7,4,4,3,0,4,3,5,5,7,
6,6,8,9,6,6,4,8,9,5,0,4,4,5,2,4,4,5,2,3,1,6,1,7,3,1,8,5,6,4,0,3,0,9,8,7,1,1,1,2,1,7,2,2,3,8,3,1,1,3,
6,2,2,2,9,8,9,3,4,2,3,3,8,0,3,0,8,1,3,5,3,3,6,2,7,6,6,1,4,2,8,2,8,0,6,4,4,4,4,8,6,6,4,5,2,3,8,7,4,9,
3,0,3,5,8,9,0,7,2,9,6,2,9,0,4,9,1,5,6,0,4,4,0,7,7,2,3,9,0,7,1,3,8,1,0,5,1,5,8,5,9,3,0,7,9,6,0,8,6,6,
7,0,1,7,2,4,2,7,1,2,1,8,8,3,9,9,8,7,9,7,9,0,8,7,9,2,2,7,4,9,2,1,9,0,1,6,9,9,7,2,0,8,8,8,0,9,3,7,7,6,
6,5,7,2,7,3,3,3,0,0,1,0,5,3,3,6,7,8,8,1,2,2,0,2,3,5,4,2,1,8,0,9,7,5,1,2,5,4,5,4,0,5,9,4,7,5,2,2,4,3,
5,2,5,8,4,9,0,7,7,1,1,6,7,0,5,5,6,0,1,3,6,0,4,8,3,9,5,8,6,4,4,6,7,0,6,3,2,4,4,1,5,7,2,2,1,5,5,3,9,7,
5,3,6,9,7,8,1,7,9,7,7,8,4,6,1,7,4,0,6,4,9,5,5,1,4,9,2,9,0,8,6,2,5,6,9,3,2,1,9,7,8,4,6,8,6,2,2,4,8,2,
8,3,9,7,2,2,4,1,3,7,5,6,5,7,0,5,6,0,5,7,4,9,0,2,6,1,4,0,7,9,7,2,9,6,8,6,5,2,4,1,4,5,3,5,1,0,0,4,7,4,
8,2,1,6,6,3,7,0,4,8,4,4,0,3,1,9,9,8,9,0,0,0,8,8,9,5,2,4,3,4,5,0,6,5,8,5,4,1,2,2,7,5,8,8,6,6,6,8,8,1,
1,6,4,2,7,1,7,1,4,7,9,9,2,4,4,4,2,9,2,8,2,3,0,8,6,3,4,6,5,6,7,4,8,1,3,9,1,9,1,2,3,1,6,2,8,2,4,5,8,6,
1,7,8,6,6,4,5,8,3,5,9,1,2,4,5,6,6,5,2,9,4,7,6,5,4,5,6,8,2,8,4,8,9,1,2,8,8,3,1,4,2,6,0,7,6,9,0,0,4,2,
2,4,2,1,9,0,2,2,6,7,1,0,5,5,6,2,6,3,2,1,1,1,1,1,0,9,3,7,0,5,4,4,2,1,7,5,0,6,9,4,1,6,5,8,9,6,0,4,0,8,
0,7,1,9,8,4,0,3,8,5,0,9,6,2,4,5,5,4,4,4,3,6,2,9,8,1,2,3,0,9,8,7,8,7,9,9,2,7,2,4,4,2,8,4,9,0,9,1,8,8,
8,4,5,8,0,1,5,6,1,6,6,0,9,7,9,1,9,1,3,3,8,7,5,4,9,9,2,0,0,5,2,4,0,6,3,6,8,9,9,1,2,5,6,0,7,1,7,6,0,6,
0,5,8,8,6,1,1,6,4,6,7,1,0,9,4,0,5,0,7,7,5,4,1,0,0,2,2,5,6,9,8,3,1,5,5,2,0,0,0,5,5,9,3,5,7,2,9,7,2,5,
7,1,6,3,6,2,6,9,5,6,1,8,8,2,6,7,0,4,2,8,2,5,2,4,8,3,6,0,0,8,2,3,2,5,7,5,3,0,4,2,0,7,5,2,9,6,3,4,5,0 ];
	 int prodlen = 13;
	 int maxProd = -1;
	 for(int i = 0 : i < sizeof(bignum) - prodlen : i++) {
	 	int prod = 1;
	 	for(int j = 0 : j < prodlen : j++)
	 		prod *= bignum[i+j];
	 	if(maxProd < prod)
	 		maxProd = prod;
	 }
	 Kernel.out << "Max Product = " << maxProd << "\n";
}