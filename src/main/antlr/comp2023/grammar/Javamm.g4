grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

//fragment INT: 'int';
//fragment BOOLEAN : 'boolean';
//KEYWORD : INT | BOOLEAN;
KEYWORD : ('int' |'int[]' | 'String[]' | 'boolean' | 'String' | 'void') ;
INTEGER : ('0' | [1-9][0-9]*) ;
ID : [a-zA-Z_$][a-zA-Z_0-9]* ;
COMMENT : (('//' ~[\r\n]*) | ('/*' . *? '*/')) -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' name=ID ('.' pack+=ID)* ';'
    ;

classField
    : varDeclaration
    ;

classDeclaration
    : 'class' name=ID ('extends' extension=ID)? '{' ( classField )* ( methodDeclaration )* '}'
    ;

varDeclaration
    : type var=ID ';'
    ;

type
    : keyword=KEYWORD #Literal
    | name=ID #CustomType
    ;

returnType
    : type
    ;

returnStatement
    : expression
    ;

argument
    : type parameter=ID
    ;

methodDeclaration
    : (modifier=('static' | 'private' | 'public'))? returnType name=ID '(' ( argument (',' argument)*)? ')' '{' (varDeclaration)* (methodStatement)* 'return' returnStatement ';' '}' #Method
    | ('public')? 'static' returnType 'main' '(' argument ')' '{' (varDeclaration)* (statement)* '}' #Main
    ;

methodStatement
    : statement
    ;

ifStatement
    : statement
    ;

elseStatement
    : statement
    ;

condition
    : expression
    ;

statement
    : '{' (statement)* '}' #CodeBlock
    | 'if' '(' condition ')' ifStatement 'else' elseStatement #Conditional
    | 'while' '(' condition ')' statement #While
    | expression ';' #ExprStmt
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '!' expression #Negation
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #ArrayLength
    | expression '.' method=ID '(' (expression(',' expression)*)?')' #MethodInvocation
    | 'new' '[' expression ']' #ArrayInstantiation
    | 'new' type '(' ')' #CustomInstantiation
    | '(' expression ')' #Parenthesis
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | value=INTEGER #Integer
    | 'true' #True
    | 'false' #False
    | value=ID #Identifier
    | 'this' #This
    ;
