package plc.project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                // FUN main() DO RETURN 0; END
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                // VAR x = 1; VAR y = 10; FUN main() DO x + y; END
                Arguments.of("Globals & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")))
                        )))
                ), Environment.NIL.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testScope(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testScope() {
        return Stream.of(
            /*FUN main() DO
            LET x = 1;
            LET y = 2;
            log(x);
            log(y);
            IF TRUE DO
                    LET x = 3;
                    y = 4;
                    log(x);
                    log(y);
            END
            log(x);
            log(y);
            END*/
                Arguments.of("If scope", new Ast.Source(
                        Arrays.asList(), Arrays.asList(
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Declaration("x",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1)))),
                                new Ast.Statement.Declaration("y",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y")))),
                                new Ast.Statement.If(new Ast.Expression.Literal(Boolean.TRUE),
                                        Arrays.asList(
                                                new Ast.Statement.Declaration("x",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(3)))),
                                                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "y"), new Ast.Expression.Literal(BigInteger.valueOf(4))),
                                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y"))))
                                        ), Arrays.asList()),
                                new Ast.Statement.If(new Ast.Expression.Literal(Boolean.TRUE),
                                        Arrays.asList(
                                                new Ast.Statement.Declaration("x",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(4)))),
                                                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "y"), new Ast.Expression.Literal(BigInteger.valueOf(5))),
                                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y"))))
                                        ), Arrays.asList()),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y"))))
                        ))
                )), Environment.NIL.getValue()),

                /*      VAR x = 1;
                        VAR y = 2;
                        VAR z = 3;
                        FUN f(z) DO
                        RETURN x + y + z;
                        END
                        FUN main() DO
                        LET y = 4;
                        RETURN f(5);
                        END */
                Arguments.of("Function Scope", new Ast.Source(
                        Arrays.asList( new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                                new Ast.Global("z", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(3))))),
                        Arrays.asList( //functions
                                //Fun f(z) do return x + y + z
                                new Ast.Function("f", Arrays.asList("z"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "x"), new Ast.Expression.Access(Optional.empty(), "y")),
                                                        new Ast.Expression.Access(Optional.empty(), "z"))
                                        ))),
                                new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                        new Ast.Statement.Declaration("y",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(4)))),
                                        new Ast.Statement.Return(new Ast.Expression.Function("f", Arrays.asList(new Ast.Expression.Literal(BigInteger.valueOf(5))))
                                        )))
                        )), BigInteger.valueOf(8)),
                /*
                Var x = 10;
                Var y = 20;
                Var z = 6;
                FUN add(a,b) DO
                    RETURN a + b;
                FUN sub(a,b) DO
                    RETURN a - b;
                FUN mult(a,b) DO
                    RETURN a * b;
                FUN div(a,b) DO
                    RETURN a / b;
                FUN pow(a,b) DO
                    RETURN a ^ b;
                FUN main() DO
                    LET a = x;
                    LET b = y;
                    Let c = 0;
                    while(z > 0) DO
                        if(z == 6)
                            c = c + add(a,b);
                        if(z == 5)
                            c = c + mult(a,b);
                         if(z == 4)
                            c = c + sub(a,b);
                        if(z == 3)
                            c = c + div(a,2);
                        if(z == 2)
                            c = c + pow(a,2);
                        z = z - 1;
                    END;
                print(b);
                print(a);
                print(c)
                 RETURN C;
                 END;
                */
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList( new Ast.Global("x", true, Optional.of(new Ast.Expression.Literal(BigInteger.TEN))),
                                new Ast.Global("y", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(20)))),
                                new Ast.Global("z", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(6))))),
                        Arrays.asList( //functions
                                //Fun f(z) do return x + y + z
                                new Ast.Function("add", Arrays.asList("a", "b"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Access(Optional.empty(), "a"), new Ast.Expression.Access(Optional.empty(), "b"))))),
                                new Ast.Function("sub", Arrays.asList("a", "b"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("-",
                                                        new Ast.Expression.Access(Optional.empty(), "a"), new Ast.Expression.Access(Optional.empty(), "b"))))),
                                new Ast.Function("mult", Arrays.asList("a", "b"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("*",
                                                        new Ast.Expression.Access(Optional.empty(), "a"), new Ast.Expression.Access(Optional.empty(), "b"))))),
                                new Ast.Function("div", Arrays.asList("a", "b"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("/",
                                                        new Ast.Expression.Access(Optional.empty(), "a"), new Ast.Expression.Access(Optional.empty(), "b"))))),
                                new Ast.Function("exp", Arrays.asList("a", "b"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                new Ast.Expression.Binary("^",
                                                        new Ast.Expression.Access(Optional.empty(), "a"), new Ast.Expression.Access(Optional.empty(), "b"))))),
                                new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                        new Ast.Statement.Declaration("a",Optional.of(new Ast.Expression.Access(Optional.empty(), "x"))),
                                        new Ast.Statement.Declaration("b",Optional.of(new Ast.Expression.Access(Optional.empty(), "y"))),
                                        new Ast.Statement.Declaration("c",Optional.of(new Ast.Expression.Literal(BigInteger.ZERO))),
                                        new Ast.Statement.While(new Ast.Expression.Binary(">", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.ZERO)),
                                                Arrays.asList(
                                                        new Ast.Statement.If(new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.valueOf(6))),
                                                                Arrays.asList(new Ast.Statement.Assignment(
                                                                        new Ast.Expression.Access(Optional.empty(), "c"), new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "c"),
                                                                        new Ast.Expression.Function("add", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Access(Optional.empty(), "b")))))
                                                                ), Arrays.asList()),

                                                        new Ast.Statement.If(new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.valueOf(5))),
                                                                Arrays.asList(new Ast.Statement.Assignment(
                                                                        new Ast.Expression.Access(Optional.empty(), "c"), new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "c"),
                                                                        new Ast.Expression.Function("mult", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Access(Optional.empty(), "b")))))
                                                                ), Arrays.asList()),

                                                        new Ast.Statement.If(new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.valueOf(4))),
                                                                Arrays.asList(new Ast.Statement.Assignment(
                                                                        new Ast.Expression.Access(Optional.empty(), "c"), new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "c"),
                                                                        new Ast.Expression.Function("sub", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Access(Optional.empty(), "b")))))
                                                                ), Arrays.asList()),

                                                        new Ast.Statement.If(new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.valueOf(3))),
                                                                Arrays.asList(new Ast.Statement.Assignment(
                                                                        new Ast.Expression.Access(Optional.empty(), "c"), new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "c"),
                                                                        new Ast.Expression.Function("div", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Literal(BigInteger.valueOf(2))))))
                                                                ), Arrays.asList()),

                                                        new Ast.Statement.If(new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.valueOf(2))),
                                                                Arrays.asList(new Ast.Statement.Assignment(
                                                                        new Ast.Expression.Access(Optional.empty(), "c"),  new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "c"),
                                                                        new Ast.Expression.Function("exp", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Literal(BigInteger.valueOf(2))))))), Arrays.asList()),
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "c")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "z"),  new Ast.Expression.Binary("-", new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(BigInteger.ONE)))
                                                )),
                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a")))),
                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "b")))),
                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "c")))),
                                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "c")))
                                ))), BigInteger.valueOf(325)),
                /*fun main()
                    LET x = 5;
                    LET y = 10;
                    LET z = 'c';
                    print(a);
                    print(b);

                 */
                Arguments.of("switch scope", new Ast.Source(
                        Arrays.asList(), Arrays.asList(
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Declaration("x",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(5)))),
                                new Ast.Statement.Declaration("y",Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(10)))),
                                new Ast.Statement.Declaration("z",Optional.of(new Ast.Expression.Literal('c'))),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x")))),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "y")))),
                                new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "z"), Arrays.asList(
                                        new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('a')), Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(new Character('b'))))),
                                        new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('b')), Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(new Character('c'))))),
                                        new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('c')), Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "z"), new Ast.Expression.Literal(new Character('f'))))),
                                        new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "x"))))))
                                )),
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "z"))))
                        ))
                )), Environment.NIL.getValue())
        );
    }
    @ParameterizedTest
    @MethodSource
    void testGlobal(String test, Ast.Global ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                // VAR name;
                Arguments.of("Mutable", new Ast.Global("name", true, Optional.empty()), Environment.NIL.getValue()),
                // VAL name = 1;
                Arguments.of("Immutable", new Ast.Global("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE)
        );
    }

    @Test
    void testList() {
        // LIST list = [1, 5, 10];
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Optional<Ast.Expression> value = Optional.of(new Ast.Expression.PlcList(values));
        Ast.Global ast = new Ast.Global("list", true, value);

        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testFunction(String test, Ast.Function ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                // FUN main() DO RETURN 0; END
                Arguments.of("Main",
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                // FUN square(x) DO RETURN x * x; END
                Arguments.of("early return",
                        new Ast.Function("func", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.If(new Ast.Expression.Literal(true), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO))), Arrays.asList()),
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE))
                        )),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                Arguments.of("Arguments",
                        new Ast.Function("square", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                )
        );
    }

    @Test
    void testExpressionStatement() {
        // print("Hello, World!");
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Statement.Expression(
                    new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                // LET name;
                Arguments.of("Declaration",
                        new Ast.Statement.Declaration("name", Optional.empty()),
                        Environment.NIL.getValue()
                ),
                // LET name = 1;
                Arguments.of("Initialization",
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        // variable = 1;
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"variable"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testListAssignmentStatement() {
        // list[2] = 3;
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.valueOf(3));
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(2))), "list"),
                new Ast.Expression.Literal(BigInteger.valueOf(3))
        ), Environment.NIL.getValue(), scope);

        Assertions.assertEquals(expected, scope.lookupVariable("list").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                // IF TRUE DO num = 1; END
                Arguments.of("True Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                // IF FALSE DO ELSE num = 10; END
                Arguments.of("False Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }
    @ParameterizedTest
    @MethodSource
    void testMoreIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("numb", true, Environment.create(BigInteger.ONE));
        //scope.defineVariable("numb", true, Environment.create(BigInteger.valueOf(3)));
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("numb").getValue().getValue());
    }

    private static  Stream<Arguments> testMoreIfStatement() {
        return Stream.of(
                //IF numb + 1 == 3 DO numb = 1 ELSE numb = 10; END
                Arguments.of("Binary Condition and comparison",
                        new Ast.Statement.If(
                                new Ast.Expression.Binary("==", new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), "numb"), new Ast.Expression.Literal(BigInteger.ONE)), new Ast.Expression.Literal(BigInteger.valueOf(3))),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "numb"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "numb"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }
    @Test
    void testDefaultCase() {
        //tests default case with variable assignment for characters
        //SWITCH letter CASE 'a': letter = 'b'; CASE 'b': letter = c; DEFAULT: letter = 'z'; END

        Scope scope = new Scope(null);
        scope.defineVariable("letter", true, Environment.create(new Character('f')));

        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"), new Ast.Expression.Literal(new Character('z')))
        );

        List<Ast.Statement.Case> cases = Arrays.asList(
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('a')), Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"), new Ast.Expression.Literal(new Character('b'))))),
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('b')), Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"), new Ast.Expression.Literal(new Character('c'))))),
                new Ast.Statement.Case(Optional.empty(), statements)
        );

        Ast.Statement.Switch ast = new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "letter"), cases);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(new Character('z'), scope.lookupVariable("letter").getValue().getValue());
    }

    @Test
    void testMoreSwitchStatement() {
        // test case statement with variable declaration and variable assignment.
        //SWITCH i CASE '1': LET x; i = 10; DEFAULT: LET x = 0; END
        Scope scope = new Scope(null);
        scope.defineVariable("i", true, Environment.create(BigInteger.ONE));

        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Declaration("x", Optional.empty()),
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "i"), new Ast.Expression.Literal(BigInteger.TEN))
        );

        List<Ast.Statement.Case> cases = Arrays.asList(
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal(BigInteger.ONE)), statements),
                new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Declaration("x", Optional.of(new Ast.Expression.Literal(BigInteger.ZERO)))))
        );

        Ast.Statement.Switch ast = new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "i"), cases);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("i").getValue().getValue());
    }


    @Test
    void testSwitchStatement() {
        // SWITCH letter CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
        Scope scope = new Scope(null);
        scope.defineVariable("letter", true, Environment.create(new Character('y')));

        List<Ast.Statement> statements = Arrays.asList(
                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), "letter"),
                        new Ast.Expression.Literal(new Character('n')))
        );

        List<Ast.Statement.Case> cases = Arrays.asList(
                new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal(new Character('y'))), statements),
                new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))))
        );

        Ast.Statement.Switch ast = new Ast.Statement.Switch(new Ast.Expression.Access(Optional.empty(), "letter"), cases);

        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(ast, Environment.NIL.getValue(), scope);
            Assertions.assertEquals("yes" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }

        Assertions.assertEquals(new Character('n'), scope.lookupVariable("letter").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        // WHILE num < 10 DO num = num + 1; END
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.create(BigInteger.ZERO));
        test(new Ast.Statement.While(
                new Ast.Expression.Binary("<",
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Literal(BigInteger.TEN)
                ),
                Arrays.asList(new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(),"num"),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(),"num"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        )
                ))
        ),Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                // NIL
                Arguments.of("Nil", new Ast.Expression.Literal(null), Environment.NIL.getValue()), //remember, special case
                // TRUE
                Arguments.of("Boolean", new Ast.Expression.Literal(true), true),
                // 1
                Arguments.of("Integer", new Ast.Expression.Literal(BigInteger.ONE), BigInteger.ONE),
                // 1.0
                Arguments.of("Decimal", new Ast.Expression.Literal(BigDecimal.ONE), BigDecimal.ONE),
                // 'c'
                Arguments.of("Character", new Ast.Expression.Literal('c'), 'c'),
                // "string"
                Arguments.of("String", new Ast.Expression.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                // (1)
                Arguments.of("Literal", new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)), BigInteger.ONE),
                // (1 + 10)
                Arguments.of("Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        BigInteger.valueOf(11)
                ),
                //(2 ^ 31 - 1) (int max)
                Arguments.of("Larger Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("-",
                                new Ast.Expression.Group(new Ast.Expression.Binary("^",
                                        new Ast.Expression.Literal(new BigInteger("2")),
                                        new Ast.Expression.Literal(new BigInteger("31")))),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        )),
                        BigInteger.valueOf(Integer.MAX_VALUE)
                ),
                //(7 < 8 && 8 < 9)
                Arguments.of("Even Larger Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("&&",
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Literal(new BigInteger("7")),
                                        new Ast.Expression.Literal(new BigInteger("8"))),
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Literal(new BigInteger("8")),
                                        new Ast.Expression.Literal(new BigInteger("9"))))),
                        Boolean.TRUE
                ),

                //((((1 + 2) + 3) * 4) + 6) / 6)
                Arguments.of("Nested Expressions",
                        new Ast.Expression.Group(new Ast.Expression.Binary("/",
                                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Group(new Ast.Expression.Binary("*",
                                                new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                                                new Ast.Expression.Literal(BigInteger.valueOf(1)),
                                                                new Ast.Expression.Literal(BigInteger.valueOf(2)))),
                                                        new Ast.Expression.Literal(BigInteger.valueOf(3)))),
                                                new Ast.Expression.Literal(BigInteger.valueOf(4)))),
                                        new Ast.Expression.Literal(BigInteger.valueOf(6)))),
                                new Ast.Expression.Literal(BigInteger.valueOf(6)))),
                        BigInteger.valueOf(5))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                // TRUE && FALSE
                Arguments.of("And",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                Arguments.of("And short circuit",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        false
                ),
                Arguments.of("True and True",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(true)
                        ),
                        true
                ),
                Arguments.of("False or False",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                Arguments.of("False or True",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Literal(true)
                        ),
                        true
                ),
                // TRUE || undefined
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                // 1 < 10
                Arguments.of("Less Than",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                // false < true (0 < 1)
                Arguments.of("Less Than when booleans",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(false),
                                new Ast.Expression.Literal(true)
                        ),
                        true
                ),

                // 11 < 10
                Arguments.of("Less Than False with strings",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal("trial 2"),
                                new Ast.Expression.Literal("trial 1")
                        ),
                        false
                ),
                // 1 > 10
                Arguments.of("Greater Than false",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                // 10 > 10
                Arguments.of("Greater Than when equal",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigDecimal.TEN),
                                new Ast.Expression.Literal(BigDecimal.TEN)
                        ),
                        false
                ),

                // b > a
                Arguments.of("Greater Than True with chars",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal('b'),
                                new Ast.Expression.Literal('a')
                        ),
                        true
                ),
                // 1 == 10
                Arguments.of("Equal false BigInt",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                // 10 == 10
                Arguments.of("Equal true BigInt",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.TEN),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                // 1 != "1"
                Arguments.of("Not equal distinct types",
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal("1")
                        ),
                        true
                ),
                // "hello there" =="hello there"
                Arguments.of("Equal true strings",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal("hello there"),
                                new Ast.Expression.Literal("hello there")
                        ),
                        true
                ),

                // 'a' != 'b'
                Arguments.of("Not equal chars",
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal('c'),
                                new Ast.Expression.Literal('a')
                        ),
                        true
                ),
                // "a" + "b"
                Arguments.of("Concatenation",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        ),
                        "ab"
                ),
                // 10 + "b"
                Arguments.of("Concatenation with non string and string",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.TEN),
                                new Ast.Expression.Literal("b")
                        ),
                        "10b"
                ),
                // 1 + 10
                Arguments.of("Addition",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),

                // 1 - 10
                Arguments.of("Subtraction",
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(-9)
                ),

                // 2 * 25
                Arguments.of("Multiplication of Ints",
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigInteger.valueOf(2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(25))
                        ),
                        BigInteger.valueOf(50)
                ),
                // 1.3 * 2.0
                Arguments.of("Multiplication of Decimals",
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigDecimal.valueOf(1.3)),
                                new Ast.Expression.Literal(BigDecimal.valueOf(2.0))
                        ),
                        BigDecimal.valueOf(2.6)
                ),
                // 1.2 / 3.4
                Arguments.of("Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(new BigDecimal("1.2")),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                ),
                // 2 ^ 2500
                Arguments.of("Exponentiation",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("2")),
                                new Ast.Expression.Literal(new BigInteger("2500"))
                        ),
                        new BigInteger("375828023454801203683362418972386504867736551759258677056523839782231681498337708535732725752658844333702457749526057760309227891351617765651907310968780236464694043316236562146724416478591131832593729111221580180531749232777515579969899075142213969117994877343802049421624954402214529390781647563339535024772584901607666862982567918622849636160208877365834950163790188523026247440507390382032188892386109905869706753143243921198482212075444022433366554786856559389689585638126582377224037721702239991441466026185752651502936472280911018500320375496336749951569521541850441747925844066295279671872605285792552660130702047998218334749356321677469529682551765858267502715894007887727250070780350262952377214028842297486263597879792176338220932619489509376")
                ),
                // negative base
                Arguments.of("Negative base",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("-2")),
                                new Ast.Expression.Literal(new BigInteger("3"))
                        ),
                        new BigInteger("-8")
                ),
                // negative base
                Arguments.of("Negative base, even exponent",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("-2")),
                                new Ast.Expression.Literal(new BigInteger("100"))
                        ),
                        new BigInteger("1267650600228229401496703205376")
                ),
                Arguments.of("Negative Exponent",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("5")),
                                new Ast.Expression.Literal(new BigInteger("-2"))
                        ),
                        new BigInteger("0")
                ),
                Arguments.of("Negative Exponent with one as base",
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(new BigInteger("1")),
                                new Ast.Expression.Literal(new BigInteger("-2"))
                        ),
                        new BigInteger("1")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                // variable
                Arguments.of("Variable",
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        "variable"
                )
        );
    }

    @Test
    void testListAccessExpression() {
        // list[1]
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test(new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1))), "list"), BigInteger.valueOf(5), scope);
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                // function()
                Arguments.of("Function",
                        new Ast.Expression.Function("function", Arrays.asList()),
                        "function"
                ),
                // print("Hello, World!")
                Arguments.of("Print",
                        new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("Hello, World!"))),
                        Environment.NIL.getValue()
                )
        );
    }

    @Test
    void testPlcList() {
        // [1, 5, 10]
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        List<Ast.Expression> values = Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                new Ast.Expression.Literal(BigInteger.valueOf(5)),
                new Ast.Expression.Literal(BigInteger.TEN));

        Ast ast = new Ast.Expression.PlcList(values);

        test(ast, expected, new Scope(null));
    }


    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

    private static <T extends Ast> void testRuntimeException(Ast ast, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        RuntimeException re = Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        Assertions.assertEquals(RuntimeException.class, re.getClass());
        System.out.println(re);
    }

    @ParameterizedTest
    @MethodSource
    void testExceptions(String test, Ast ast) {
        testRuntimeException(ast,new Scope(null));
    }
    private static Stream<Arguments> testExceptions() {
        return Stream.of(
                Arguments.of("Integer Decimal Subtraction",
                        //name()
                        new Ast.Expression.Binary("-", new Ast.Expression.Literal(BigInteger.valueOf(1)), new Ast.Expression.Literal(BigDecimal.valueOf(1.0)))),
                Arguments.of("Redefined Global",
                        //name()
                        new Ast.Source(Arrays.asList(
                                new Ast.Global("name", true, Optional.empty()),
                                new Ast.Global("name", true, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1))))), Arrays.asList())),
                Arguments.of("While condition not boolean",
                        //name()
                        new Ast.Statement.While(new Ast.Expression.Literal("false"), Arrays.asList())),
                Arguments.of("List with offset not BigInt",
                        //name()
                        new Ast.Source(Arrays.asList(new Ast.Global("list", true,
                                Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                        new Ast.Expression.Literal(BigInteger.TEN)))))),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Return( new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigDecimal.ONE)),"list"))))
                                ))),
                Arguments.of("Offset out of bounds exception",
                        //name()
                        new Ast.Source(Arrays.asList(new Ast.Global("list", true,
                                Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                        new Ast.Expression.Literal(BigInteger.TEN)))))),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Return( new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.TEN)),"list"))))
                                ))),
                Arguments.of("Illegal Division by zero",
                        //name()
                        new Ast.Expression.Binary("/", new Ast.Expression.Literal(BigInteger.ONE), new Ast.Expression.Literal(BigInteger.ZERO))),
                Arguments.of("No main source",
                        //name()
                        new Ast.Source(Arrays.asList(new Ast.Global("list", true,
                                Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE),
                                        new Ast.Expression.Literal(BigInteger.valueOf(5)),
                                        new Ast.Expression.Literal(BigInteger.TEN)))))),
                                Arrays.asList(
                                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Return( new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.TEN)),"list"))))
                                ))),
                Arguments.of("Illegal exponentiation of non Big Int numbers",
                        new Ast.Expression.Binary("^", new Ast.Expression.Literal(BigInteger.TEN), new Ast.Expression.Literal(BigDecimal.TEN))),
                Arguments.of("Illegal exponentiation with exponent > int_max",
                        new Ast.Expression.Binary("^", new Ast.Expression.Literal(BigInteger.valueOf(2)), new Ast.Expression.Literal(new BigDecimal("2147483648")))),

                Arguments.of("No main source",
                        //name()
                        new Ast.Source(Arrays.asList(new Ast.Global("a", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Assignment( new Ast.Expression.Literal(BigInteger.TEN),new Ast.Expression.Access(Optional.empty(), "a"))))))),
                Arguments.of("Changing immutable variable",
                        new Ast.Source(Arrays.asList(new Ast.Global("a", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.Assignment( new Ast.Expression.Access(Optional.empty(), "a"),new Ast.Expression.Literal(BigInteger.TEN))))))),
                Arguments.of("Variable defined in IF statement no Longer in Scope",
                        new Ast.Source(Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(
                                                new Ast.Statement.If(new Ast.Expression.Literal(Boolean.TRUE),
                                                        Arrays.asList( new Ast.Statement.If(new Ast.Expression.Literal(Boolean.TRUE),
                                                                Arrays.asList( new Ast.Statement.If(new Ast.Expression.Literal(Boolean.TRUE),
                                                                        Arrays.asList(new Ast.Statement.Declaration("a", Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))),
                                                                        Arrays.asList())),
                                                                Arrays.asList())),
                                                        Arrays.asList()),
                                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Access(Optional.empty(), "a")))
                                                ))))))
        );

    }
}
