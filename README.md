# MyCompiler

## Description
This is the course project for the 2021-2022 Compiler Technology course, a Java-implemented MIPS-SysY(a subset of C language) compiler from draft, which can compile any SysY language program into fast MIPS assembly codes.\\
The compiler includes six basic parts of a compiler: lexical analysis, syntax analysis, semantic Analysis, Intermediate Code Generation, optimization, and code generation, and is able to dispose a range of exceptions.

## Compilable Grammar
```
<Addition Operator> ::= +|-
<Multiplication Operator> ::= *|/
<Relational Operator> ::= <|<=|>|>=|!=|==
<Letter> ::= _|a|...|z|A|...|Z
<Number> ::= 0|<Non-zero Number>
<Non-zero Number> ::= 1|...|9
<Character> ::= '<Addition Operator>'|'<Multiplication Operator>'|'<Letter>'|'<Number>'
<String> ::= "{ASCII characters with decimal codes 32, 33, and 35-126}"
<Program> ::= [<Constant Declaration>][<Variable Declaration>]{<Function with Return Value Definition>|<Function without Return Value Definition>}<Main Function>
<Constant Declaration> ::= const<Constant Definition>;{ const<Constant Definition>;}
<Constant Definition> ::= int<Identifier>=<Integer>{,<Identifier> = <Integer>}
                    | char<Identifier> = <Character>{,<Identifier>=<Character>}
<Unsigned Integer> ::= <Non-zero Number>{<Number>}| 0
<Integer> ::= [+|-]<Unsigned Integer>
<Identifier> ::= <Letter>{<Letter>|<Number>}
<Declaration Header> ::= int<Identifier> |char<Identifier>
<Variable Declaration> ::= <Variable Definition>;{<Variable Definition>;}
<Variable Definition> ::= <Type Identifier>(<Identifier>|<Identifier>'['<Unsigned Integer>']'){,(<Identifier>|<Identifier>'['<Unsigned Integer>']' )}
<Type Identifier> ::= int | char
<Function with Return Value Definition> ::= <Declaration Header>'('<Parameter List>')' '{'<Compound Statement>'}'
<Function without Return Value Definition> ::= void<Identifier>'('<Parameter List>')''{'<Compound Statement>'}'
<Compound Statement> ::= [<Constant Declaration>][<Variable Declaration>]<Statement List>
<Parameter List> ::= <Type Identifier><Identifier>{,<Type Identifier><Identifier>}| <Empty>
<Main Function> ::= void main'('')' '{'<Compound Statement>'}'
<Expression> ::= [<+|->]<Term>{<Addition Operator><Term>} //[+|-] applies only to the first <Term>
<Term> ::= <Factor>{<Multiplication Operator><Factor>}
<Factor> ::= <Identifier>|<Identifier>'['<Expression>']'|'('<Expression>')'|<Integer>|<Character>|<Function Call with Return Value Statement>
<Statement> ::= <Conditional Statement>|<Loop Statement>|'{'<Statement List>'}'|<Function with Return Value Call Statement>;|<Function without Return Value Call Statement>;|<Assignment Statement>;|<Read Statement>;|<Write Statement>;|<Empty>;|<Return Statement>;
<Assignment Statement> ::= <Identifier>=<Expression>|<Identifier>'['<Expression>']'=<Expression>
<Conditional Statement> ::= if '('<Condition>')'<Statement>[else<Statement>]
<Condition> ::= <Expression><Relational Operator><Expression>|<Expression> // The condition is true if the expression is non-zero, otherwise false
<Loop Statement> ::= while '('<Condition> ')'<Statement>| do<Statement>while '('<Condition>')'|for'('<Identifier>=<Expression>;<Condition>;<Identifier>=<Identifier>(+|-)<Step>')'<Statement>
<Step> ::= <Unsigned Integer>
<Function with Return Value Call Statement> ::= <Identifier>'('<Value Parameter List>')'
<Function without Return Value Call Statement> ::= <Identifier>'('<Value Parameter List>')'
<Value Parameter List> ::= <Expression>{,<Expression>}|<Empty>
<Statement List> ::= {<Statement>}
<Read Statement> ::= scanf '('<Identifier>{,<Identifier>}')'
<Write Statement> ::= printf '(' <String>,<Expression>')'| printf '('<String> ')'| printf '('<Expression>')'
<Return Statement> ::= return ['('<Expression>')']
```
