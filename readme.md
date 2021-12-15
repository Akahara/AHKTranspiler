AHKTranspiler
============

The AHK Transpiler is a collection containing a tokenizer, parsers, linker and multiple compilers to different languages for the AHK language.

> The AHK language is my longest running solo project, I try to do everything on my own, including custom parsers, lexers... with minimal dependencies.

In its current state the language can be used to do pretty much anything you would do when learning a new language to play arround with. It is not quite production-ready yet but it's ready enough to complete programming challenges like the [Project Euler](https://projecteuler.net/) or the [Advent of Code](https://adventofcode.com/) which I'll try to do this year.

## The AHK language

The AHK language is heavily inspired by Java and C++, it is:
- imperative
- Turing complete
- statically typed
- statically linked
- garbage collected *(will be)*

In AHK you write code in *Units*, units get tokenized and parsed individually but linked alltogether. A Unit can contain global variables, functions, structures, aliases and blueprints. Syntax is very similar to Java, here is a simple example program:

```
base fr.wonder.main;

import ahk.Kernel;

unit Main;

global func int main() {
  fizzBuzz();
}

func void fizzBuzz() {
  for(int i = 1..1000) {
    Kernel.out << "\n" << i << " ";
    if(i%3 == 0)
      Kernel.out << "Fizz";
    if(i%5 == 0)
      Kernel.out << "Buzz";
  }
  Kernel.out << "\n";
}
```

> This page is cannot be a tutorial, if you want to try things using AHK search through the examples (the `code` folder).

### Functions manipulations

AHK has [first class functions](https://en.wikipedia.org/wiki/First-class_function), functions can be stored in variables using aliases and used in operations:

```
alias Generator = func int(int);

func int identity(int x) {
  return x;
}

func int double(int x) {
  return 2*x;
}

global func int main() {
  Generator i = identity;
  Generator d = double;
  Generator g = i+g;
  Kernel.out << "I(4)= " << i(4) << "\n"; // 4
  Kernel.out << "I(4)+D(4)= " << i(4) + d(4) << "\n"; // 12
  Kernel.out << "(I+D)(4)= " << (i+d)(4) << "\n";     // 12
  Kernel.out << "(8*D)(4)= " << (8*d)(4) << "\n";     // 64
}
```

If `f`, `g` and `p`,`q` are defined as
- `f: (X) -> Y`
- `g: (Y) -> Z`
- `p,q: (A,B,C...) -> W`

Then operations on these are defined:
| Operators | Syntax | Interpetation |
|   :---:   |  :--:  | ------------- |
|   `>>`    |`f >> g`| `g∘f`, `g(f(.))`, `f` then `g`, `g` feeds on `f`. |
|   `<<`    |`f << g`| `f∘g`... |
| others <br>(`+`,`*`,`&&`...) | `p+q` | `p(.)+q(.)`, only applies if type `W` defines[^1] the operator `+`. |

Currently implicit conversions of return types do not work, you cannot add a function returning a float and one returning an int. Also boolean operators `&&` and `||` do not have lazy-evaluation, both operands are evaluated everytime.

[^1]: Native types like `int`, `float`... can be used, native operations are defined by default. New operations can de defined for custom types using operator overloading.

## Installation and compilers

### Installation & Running

To install simply clone or download the project, all dependencies are home-made, available libraries: [wonder.commons](https://github.com/Akahara/fr.wonder.commons) and [wonder.commons.systems](https://github.com/Akahara/fr.wonder.commons.systems). Add both dependencies to the module path and build with Java (JRE version 15 works, lower may not).\
\
To run simply execute the generated jar, command line arguments are not available yet so the only files that will be compiled must be in the `code` folder and generated binaries will be in the generated `exported` folder. To change these directories or choose a different compiler modify the `fr.wonder.ahk.AHKTranspiler#main` method.

### Available compilers

There was an AHK to Python compiler that has been deprecated and should soon be reimplemented or completely removed.\
Currently the default compiler is the AHK to x64 assembly one, it compiles fine to x64 linux, macosx is unsupported and windows is untested (probably won't work because of wrong system calls).

## Future plans

> Obviously AHK is not complete yet, I'm working on it in my free time and progress is slow and steady.

Current goals I'd like to implement:
- [ ] a garbage collector, currently instances can be created but they are not freed
- [ ] lambdas
- [ ] enums
- [ ] file IO
- [ ] multithreading
- [ ] a new, more optimized x64 compiler
- [ ] crossplatform support (one day)
