package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tests have been provided for a few selective parts of the AST, and are not
 * exhaustive. You should add additional tests for the remaining parts and make
 * sure to handle all of the cases defined in the specification which have not
 * been tested here.
 */
public final class AnalyzerTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSource(String test, Ast.Source ast, Ast.Source expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            expected.getGlobals().forEach(global -> Assertions.assertEquals(global.getVariable(), analyzer.scope.lookupVariable(global.getName())));
            expected.getFunctions().forEach(fun -> Assertions.assertEquals(fun.getFunction(), analyzer.scope.lookupFunction(fun.getName(), fun.getParameters().size())));
        }
    }
    private static Stream<Arguments> testSource() {
        return Stream.of(
                // VAR value: Boolean = TRUE; FUN main(): Integer DO RETURN value; END
                Arguments.of("Invalid Return",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("value", "Boolean", true, Optional.of(new Ast.Expression.Literal(true)))
                                ),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "value")))
                                        )
                                )
                        ),
                        null
                ),
                // FUN main() DO RETURN 0; END
                Arguments.of("Missing Integer Return Type for Main",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                            new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("0"))))
                                        )
                                )
                        ),
                        null
                ),
                // FUN main(num : Integer) DO END;
                Arguments.of("No Main function with arity 0",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList("num"), Arrays.asList("Integer"), Optional.empty(), Arrays.asList()))),
                        null
                ),
                // FUN Fun() DO END;
                Arguments.of("No main Function",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("Fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList()))),
                        null
                ),
                // FUN main() : Decimal DO END;
                Arguments.of("Main Function wrong return type",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Decimal"), Arrays.asList()))),
                        null
                ),
                Arguments.of("Valid Return",
                        //VAR value: Integer = 1; FUN main(): Integer DO RETURN value; END
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("value", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))
                                ),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "value")))
                                        )
                                )
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                init(new Ast.Global("value", "Integer", true, Optional.of(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)))),
                                        ast->ast.setVariable(new Environment.Variable("value", "value", Environment.Type.INTEGER, true, Environment.create(BigInteger.ONE))))),
                                Arrays.asList(
                                init(new Ast.Function("main", Arrays.asList(), Arrays.asList(),Optional.of("Integer"), Arrays.asList(
                                     new Ast.Statement.Return(
                                             init(new Ast.Expression.Access(Optional.empty(), "value"), ast ->ast.setVariable(new Environment.Variable("value", "value", Environment.Type.INTEGER, true, Environment.NIL)))))),
                                        ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL ))
                                ))

                )),
                Arguments.of("Multiple Functions with Different Return types",
                        //Fun fun() : String return "true";
                        //Fun main() : Integer return 0;
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("String"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal("true")))
                                        ),
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                                        )
                                )
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        init(new Ast.Function("fun", Arrays.asList(), Arrays.asList(),Optional.of("String"), Arrays.asList(
                                                        new Ast.Statement.Return(
                                                                init(new Ast.Expression.Literal("true"), ast ->ast.setType(Environment.Type.STRING))))),
                                                ast -> ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(), Environment.Type.STRING, args -> Environment.NIL ))
                                        ),
                                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(),Optional.of("Integer"), Arrays.asList(
                                                        new Ast.Statement.Return(
                                                                init(new Ast.Expression.Literal(BigInteger.ZERO), ast ->ast.setType(Environment.Type.INTEGER))))),
                                                ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL ))
                                        ))

                        ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testGlobal(String test, Ast.Global ast, Ast.Global expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                Arguments.of("Declaration",
                        // VAR name: Integer;
                        new Ast.Global("name", "Integer", true, Optional.empty()),
                        init(new Ast.Global("name", "Integer", true, Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        })
                ),
                Arguments.of("Declaration with val",
                        // VAR name: Integer;
                        new Ast.Global("name", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Global("name", "Integer", true, Optional.of(init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)))), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.create(BigInteger.ONE)));
                        })
                ),
                Arguments.of("Variable Type Mismatch",
                        // VAR name: Decimal = 1;
                        new Ast.Global("name", "Decimal", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        null
                ),
                Arguments.of("List",
                        // LIST list: Decimal = [1.0, 2.0];
                        new Ast.Global("list", "Decimal", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(new BigDecimal("1.0")), new Ast.Expression.Literal(new BigDecimal("2.0")))))),
                       init(new Ast.Global("list", "Decimal", true,
                                       Optional.of(init(new Ast.Expression.PlcList(Arrays.asList(init(new Ast.Expression.Literal(new BigDecimal("1.0")), ast ->ast.setType(Environment.Type.DECIMAL)), init(new Ast.Expression.Literal(new BigDecimal("2.0")), ast -> ast.setType(Environment.Type.DECIMAL)))),
                                                    ast -> ast.setType(Environment.Type.DECIMAL)))),
                               ast ->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.DECIMAL, true, Environment.NIL)))),
                Arguments.of("Invalid List",
                        // LIST list: Integer = [1.0, 2.0];
                        new Ast.Global("list", "Integer", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(new BigDecimal("1.0")), new Ast.Expression.Literal(new BigDecimal("2.0")))))),
                        null
                ),
                Arguments.of("Valid List Any",
                        // LIST list: Any = [1.0, 2.0];
                        new Ast.Global("list", "Any", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE), new Ast.Expression.Literal(new BigDecimal("2.0")))))),
                        init(new Ast.Global("list", "Any", true,
                                        Optional.of(init(new Ast.Expression.PlcList(Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE), ast ->ast.setType(Environment.Type.INTEGER)), init(new Ast.Expression.Literal(new BigDecimal("2.0")), ast -> ast.setType(Environment.Type.DECIMAL)))),
                                                ast -> ast.setType(Environment.Type.ANY)))),
                                ast ->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.ANY, true, Environment.NIL)))),
                Arguments.of("Valid List Any",
                        // LIST list: Any = ["apple", "pear"];
                        new Ast.Global("list", "Any", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal("apple"), new Ast.Expression.Literal("pear"))))),
                        init(new Ast.Global("list", "Any", true,
                                        Optional.of(init(new Ast.Expression.PlcList(Arrays.asList(init(new Ast.Expression.Literal("apple"), ast ->ast.setType(Environment.Type.STRING)), init(new Ast.Expression.Literal("pear"), ast -> ast.setType(Environment.Type.STRING)))),
                                                ast -> ast.setType(Environment.Type.ANY)))),
                                ast ->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.ANY, true, Environment.NIL)))),
                Arguments.of("Valid List chars",
                        // LIST list: Any = ["a", "b"];
                        new Ast.Global("list", "Character", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Literal('a'), new Ast.Expression.Literal('b'))))),
                        init(new Ast.Global("list", "Character", true,
                                        Optional.of(init(new Ast.Expression.PlcList(Arrays.asList(init(new Ast.Expression.Literal('a'), ast ->ast.setType(Environment.Type.CHARACTER)), init(new Ast.Expression.Literal('b'), ast -> ast.setType(Environment.Type.CHARACTER)))),
                                                ast -> ast.setType(Environment.Type.CHARACTER)))),
                                ast ->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.CHARACTER, true, Environment.NIL)))),
                Arguments.of("Unknown Type",
                        // VAR name: Unknown;
                        new Ast.Global("name", "Unknown", true, Optional.empty()),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunction(String test, Ast.Function ast, Ast.Function expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getFunction(), analyzer.scope.lookupFunction(expected.getName(), expected.getParameters().size()));
        }
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                Arguments.of("Hello World",
                        // FUN main(): Integer DO print("Hello, World!"); END
                        // Recall note under Ast.Function, we do not check for missing RETURN
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(
                                        new Ast.Expression.Literal("Hello, World!")
                                )))
                         )),
                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                        init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Return 0",
                        // FUN main(): Integer DO RETURN 0; END
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                        new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("0")))
                                )
                        ),
                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Return(init(new Ast.Expression.Literal(new BigInteger("0")), ast -> ast.setType(Environment.Type.INTEGER)))
                        )),
                        ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("No explicit return type",
                        // FUN empty() DO END
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.empty(),
                                Arrays.asList()
                        ),
                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList()),
                                ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL)))
                ),
                Arguments.of("Return Type Mismatch",
                        // FUN increment(num: Integer): Decimal DO RETURN num + 1; END
                        new Ast.Function("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Decimal"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ))
                        )),
                        null
                ),
                Arguments.of("Return Type Correct",
                        // FUN increment(num: Integer): Integer DO RETURN num + 1; END
                        new Ast.Function("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ))
                        )),
                        init(new Ast.Function("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                init(new Ast.Expression.Binary("+",
                                                        init(new Ast.Expression.Access(Optional.empty(), "num"), ast ->  ast.setVariable(new Environment.Variable("num", "num", Environment.Type.INTEGER, true, Environment.NIL))),
                                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                                ), ast -> ast.setType(Environment.Type.INTEGER))
                                        ))),
                                ast -> ast.setFunction(new Environment.Function("increment", "increment", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Early Return Correct",
                        // FUN fun() :Integer DO if(true) Do Return 1; END Return 0 END
                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.If(new Ast.Expression.Literal(true), Arrays.asList(new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE))), Arrays.asList()),
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE)))),
                        init(new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.If(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                        Arrays.asList(new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)))), Arrays.asList()),
                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER))))),
                                ast->ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Early Return incorrect",
                        // FUN fun() :Integer DO if(true) Do Return 'c'; END Return 0 END
                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.If(new Ast.Expression.Literal(true), Arrays.asList(new Ast.Statement.Return(new Ast.Expression.Literal('c'))), Arrays.asList()),
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE)))),
                        null
                ),
                Arguments.of("Early Return incorrect",
                        // FUN fun() :Integer DO if(true) Do Return 1; else return 'c' END;
                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.If(new Ast.Expression.Literal(true),
                                        Arrays.asList(new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ONE))),
                                        Arrays.asList(new Ast.Statement.Return(new Ast.Expression.Literal('c')))))),
                            null
                ),
                Arguments.of("Function with no type return return null",
                        // FUN fun() return "ok;
                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(null)))),
                        init(new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                new Ast.Statement.Return(init(new Ast.Expression.Literal(null), ast->ast.setType(Environment.Type.NIL)))
                        )), ast->ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(), Environment.Type.NIL, args-> Environment.NIL)))
                ),
                Arguments.of("Function with no type return return null",
                        // FUN fun() return "ok;
                        new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal("ok")))),
                      null
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testStatementExpression(String test, Ast.Statement.Expression ast, Ast.Statement.Expression expected) {
       test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testStatementExpression() {
        return Stream.of(
                Arguments.of("Function",
                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))),
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)))),
                                ast ->ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                ),
                Arguments.of("Literal",
                        new Ast.Statement.Expression(new Ast.Expression.Literal(BigInteger.ONE)),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Ast.Statement.Declaration expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        })
                ),
                Arguments.of("Initialization",
                        // LET name = 1;
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL)))
                ),
                Arguments.of("Initialization and declaration",
                        // LET name : Integer = 1;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL)))
                ),
                Arguments.of("Initialization and declaration - type and return value mismatch",
                        // LET name : Integer = 1;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(new Ast.Expression.Literal(BigDecimal.ONE))),
                        null
                ),
                Arguments.of("Wrong type and value returned",
                        // LET name = 1;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.of(new Ast.Expression.Literal(BigDecimal.ONE))),
                        null
                ),
                Arguments.of("Missing Type",
                        // LET name;
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.empty()),
                        null
                ),
                Arguments.of("Unknown Type",
                        // LET name: Unknown;
                        new Ast.Statement.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatement(String test, Ast.Statement.Assignment ast, Ast.Statement.Assignment expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL);
            scope.defineVariable("any", "any", Environment.Type.ANY, true, Environment.NIL);
            scope.defineVariable("comparable", "comparable", Environment.Type.COMPARABLE, true, Environment.NIL);
        }));
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                ),
                Arguments.of("Invalid Type",
                        // variable = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal("string")
                        ),
                        null
                ),
                Arguments.of("Any Type",
                        // var = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "any"),
                                new Ast.Expression.Literal("string")
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "any"), ast -> ast.setVariable(new Environment.Variable("any", "any", Environment.Type.ANY, true, Environment.NIL))),
                                init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                        )
                ),
                Arguments.of("comparable Type",
                        // var = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "comparable"),
                                new Ast.Expression.Literal('c')
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "comparable"), ast -> ast.setVariable(new Environment.Variable("comparable", "comparable", Environment.Type.COMPARABLE, true, Environment.NIL))),
                                init(new Ast.Expression.Literal('c'), ast -> ast.setType(Environment.Type.CHARACTER))
                        )
                ),
                Arguments.of("Any as RHS",
                        // var = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "comparable"),
                                new Ast.Expression.Access(Optional.empty(), "any")
                        ),
                       null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatement(String test, Ast.Statement.If ast, Ast.Statement.If expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        // IF TRUE DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        new Ast.Statement.If(
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.Expression(
                                        init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Invalid Condition",
                        // IF "FALSE" DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal("FALSE"),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                            new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Invalid Statement",
                        // IF TRUE DO print(9223372036854775807); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE))
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Empty Statements",
                        // IF TRUE DO END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(),
                                Arrays.asList()
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSwitchStatement(String test, Ast.Statement.Switch ast, Ast.Statement.Switch expected) {
        test(ast, expected,
                init(new Scope(null),
                        scope -> {
                            // we need letter and number to be defined within the scope in order to analyze the switch examples

                            // note:  recall during the Analyzer, letter and number could have been initialized Environment.NIL,
                            //        the types are what we are concerned with in the Analyzer and not the evaluation of what is stored within the variables.
                            scope.defineVariable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y'));
                            scope.defineVariable("number", "number", Environment.Type.INTEGER, true, Environment.create(new BigInteger("1")));
                        }
                )
        );
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Condition Value Type Match",
                        // SWITCH letter CASE 'y': print("yes"); letter = 'n'; DEFAULT print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"letter"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Literal('y')),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "letter"),
                                                                new Ast.Expression.Literal('n')
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))
                                                )
                                        )
                                )
                        ),
                        new Ast.Statement.Switch(
                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(init(new Ast.Expression.Literal('y'), ast -> ast.setType(Environment.Type.CHARACTER))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("yes"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        ),
                                                        new Ast.Statement.Assignment(
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                                                init(new Ast.Expression.Literal('n'), ast -> ast.setType(Environment.Type.CHARACTER))
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                Arguments.of("Condition Value Type Mismatch",
                        // SWITCH number CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"number"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Literal('y')),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "letter"),
                                                                new Ast.Expression.Literal('n')))),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no")))))))),
                        null
                ),
                Arguments.of("More than 1 default",
                        // SWITCH number CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"letter"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Literal('y')),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "letter"),
                                                                new Ast.Expression.Literal('n')))),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no")))))),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no")))))))),
                        null
                ),
                Arguments.of("Only Default",
                        // SWITCH number CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"letter"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no")))))))),
                        new Ast.Statement.Switch(init(new Ast.Expression.Access(Optional.empty(), "letter"), args -> args.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                                Arrays.asList(new Ast.Statement.Case(
                                                        Optional.empty(),
                                                        Arrays.asList(
                                                                new Ast.Statement.Expression(
                                                                        init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                                ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))))))
                ),
                Arguments.of("Default has Value",
                        // SWITCH number CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"letter"),
                                Arrays.asList(new Ast.Statement.Case(Optional.of(new Ast.Expression.Literal('y')),  Arrays.asList()))),
                        null
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testWhileStatement(String test, Ast.Statement.While ast, Ast.Statement.While expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        new Ast.Statement.While(new Ast.Expression.Literal(true), Arrays.asList()),
                        new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)), Arrays.asList())
                ),
                Arguments.of("Invalid Condition",
                        new Ast.Statement.While(new Ast.Expression.Literal(BigInteger.ZERO), Arrays.asList()),
                        null
                ),
                Arguments.of("Valid Condition with statement",
                        new Ast.Statement.While(new Ast.Expression.Literal(true), Arrays.asList(
                                new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty())
                        )),
                        new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)), Arrays.asList(
                              init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        }))
                        ))
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpression(String test, Ast.Expression.Literal ast, Ast.Expression.Literal expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean",
                        // TRUE
                        new Ast.Expression.Literal(true),
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Character",
                        // 'c'
                        new Ast.Expression.Literal('c'),
                        init(new Ast.Expression.Literal('c'), ast -> ast.setType(Environment.Type.CHARACTER))
                ),
                Arguments.of("String",
                        // "string"
                        new Ast.Expression.Literal("string"),
                        init(new Ast.Expression.Literal("string"), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("Null",
                        // null
                        new Ast.Expression.Literal(null),
                        init(new Ast.Expression.Literal(null), ast -> ast.setType(Environment.Type.NIL))
                ),
                Arguments.of("Integer Valid",
                        // 2147483647
                        new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Invalid",
                        // 9223372036854775807
                        new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                        null
                ),
                Arguments.of("Integer Invalid - Too Small",
                        // 9223372036854775807
                        new Ast.Expression.Literal(BigInteger.valueOf(Long.MIN_VALUE)),
                        null
                ),
                Arguments.of("Decimal Valid",
                        // double max
                        new Ast.Expression.Literal(BigDecimal.valueOf(Double.MAX_VALUE)),
                        init(new Ast.Expression.Literal(BigDecimal.valueOf(Double.MAX_VALUE)), ast -> ast.setType(Environment.Type.DECIMAL))
                ),
                Arguments.of("Decimal Invalid",
                        //larger than double max
                        new Ast.Expression.Literal(new BigDecimal("1.8E308")),
                        null
                ),
                Arguments.of("Decimal Invalid - Too small",
                        //larger than double max
                        new Ast.Expression.Literal(new BigDecimal("-1.8E308")),
                        null
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testGroupExpression(String test, Ast.Expression.Group ast, Ast.Expression.Group expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("(1)",
                        // TRUE && FALSE
                        new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        null
                ),

                Arguments.of("(1 + 10)",
                    // TRUE && FALSE
                    new Ast.Expression.Group(new Ast.Expression.Binary("+",
                            new Ast.Expression.Literal(BigInteger.ONE),
                            new Ast.Expression.Literal(BigInteger.TEN)
                    )),
                    init(new Ast.Expression.Group(
                            init(new Ast.Expression.Binary("+",
                                    init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                    init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                            ), ast -> ast.setType(Environment.Type.INTEGER))
                    ), ast -> ast.setType(Environment.Type.INTEGER))),
                Arguments.of("(1 < 10)",
                        // TRUE && FALSE
                        new Ast.Expression.Group(new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        init(new Ast.Expression.Group(
                                init(new Ast.Expression.Binary("<",
                                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                                ), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))),
                Arguments.of("(1 < 10 || TRUE)",
                        // TRUE && FALSE
                        new Ast.Expression.Group(new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Literal(BigInteger.ONE),
                                        new Ast.Expression.Literal(BigInteger.TEN)),
                                        new Ast.Expression.Literal(true)
                        )),

                        init(new Ast.Expression.Group(
                                init(new Ast.Expression.Binary("||",
                                        init(new Ast.Expression.Binary("<",
                                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
                                        ), ast -> ast.setType(Environment.Type.BOOLEAN))),
                                ast -> ast.setType(Environment.Type.BOOLEAN)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpression(String test, Ast.Expression.Binary ast, Ast.Expression.Binary expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("comparable", "comparable", Environment.Type.COMPARABLE, true, Environment.NIL);
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Logical AND Valid",
                        // TRUE && FALSE
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal(Boolean.FALSE)
                        ),
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(Boolean.FALSE), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Logical AND Invalid",
                        // TRUE && "FALSE"
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal("FALSE")
                        ),
                        null
                ),
                Arguments.of("Logical AND Invalid Type",
                        // TRUE && 1
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        null
                ),
                Arguments.of("Less than Valid",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("<",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Less than invalid",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal('a')
                        ),
                        null
                ),
                Arguments.of("Greater than Valid",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Literal(BigDecimal.ONE),
                                new Ast.Expression.Literal(BigDecimal.TEN)
                        ),
                        init(new Ast.Expression.Binary(">",
                                init(new Ast.Expression.Literal(BigDecimal.ONE), ast -> ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal(BigDecimal.TEN), ast -> ast.setType(Environment.Type.DECIMAL))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Greater than Valid",
                        new Ast.Expression.Binary(">",
                                new Ast.Expression.Access(Optional.empty(), "comparable"),
                                new Ast.Expression.Access(Optional.empty(), "comparable")
                        ),
                        init(new Ast.Expression.Binary(">",
                                init(new Ast.Expression.Access(Optional.empty(), "comparable"), ast->ast.setVariable(new Environment.Variable("comparable", "comparable", Environment.Type.COMPARABLE, true, Environment.NIL))),
                                init(new Ast.Expression.Access(Optional.empty(), "comparable"), ast->ast.setVariable(new Environment.Variable("comparable", "comparable", Environment.Type.COMPARABLE, true, Environment.NIL)))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Equals than Valid",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal("one"),
                                new Ast.Expression.Literal("two")
                        ),
                        init(new Ast.Expression.Binary("==",
                                init(new Ast.Expression.Literal("one"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal("two"), ast -> ast.setType(Environment.Type.STRING))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Equals than invalid",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal("one"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        null
                ),
                Arguments.of("Not equals than Valid",
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal('a'),
                                new Ast.Expression.Literal('b')
                        ),
                        init(new Ast.Expression.Binary("!=",
                                init(new Ast.Expression.Literal('a'), ast -> ast.setType(Environment.Type.CHARACTER)),
                                init(new Ast.Expression.Literal('b'), ast -> ast.setType(Environment.Type.CHARACTER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("invalid comparison",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal('a'),
                                new Ast.Expression.Literal("b")
                        ),
                        null
                ),
                Arguments.of("String Concatenation",
                        // "Ben" + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("Ben"),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("String Concatenation other side",
                        // 4 + "twenty"
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.valueOf(4)),
                                new Ast.Expression.Literal("twenty")
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.valueOf(4)), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal("twenty"), ast -> ast.setType(Environment.Type.STRING))
                        ), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("Integer Addition",
                        // 1 + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Decimal Addition",
                        // 1 + 1.0
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigDecimal.ONE)
                        ),
                        null
                ),
                Arguments.of("Integer subtraction",
                        // 1 - 1.0
                        new Ast.Expression.Binary("-",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("-",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Decimal multiplication",
                        // 1 * 1.0
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigDecimal.ONE),
                                new Ast.Expression.Literal(BigDecimal.TEN)
                        ),
                        init(new Ast.Expression.Binary("*",
                                init(new Ast.Expression.Literal(BigDecimal.ONE), ast -> ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal(BigDecimal.TEN), ast -> ast.setType(Environment.Type.DECIMAL))
                        ), ast -> ast.setType(Environment.Type.DECIMAL))
                ),
                Arguments.of("Decimal Division",
                        // 1 / 1.0
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(BigDecimal.ONE),
                                new Ast.Expression.Literal(BigDecimal.TEN)
                        ),
                        init(new Ast.Expression.Binary("/",
                                init(new Ast.Expression.Literal(BigDecimal.ONE), ast -> ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal(BigDecimal.TEN), ast -> ast.setType(Environment.Type.DECIMAL))
                        ), ast -> ast.setType(Environment.Type.DECIMAL))
                ),
                Arguments.of("Illegal multiplication of Int and Decimal",
                        // 1 + 1.0
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal(BigDecimal.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        null
                ),
                Arguments.of("Illegal multiplication of char and string",
                        // 'a' * 10
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Literal('a'),
                                new Ast.Expression.Literal("BigInteger.TEN")
                        ),
                        null
                ),
                Arguments.of("Exponentiation",
                        // 1 ^ 10
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigInteger.TEN),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("^",
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Exponentiation with wrong types",
                        // 1 ^ 10
                        new Ast.Expression.Binary("^",
                                new Ast.Expression.Literal(BigDecimal.TEN),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAccessExpression(String test, Ast.Expression ast, Ast.Expression.Access expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL);
            scope.defineVariable("list", "list", Environment.Type.INTEGER, true, Environment.NIL);
        }));
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL)))
                ),
                Arguments.of("List",
                        // variable
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.ONE)), "list"),
                        init(new Ast.Expression.Access(Optional.of(init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))), "list"), ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.INTEGER, true, Environment.NIL)))
                ),
                Arguments.of("List Invalid",
                        // variable
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigDecimal.ONE)), "variable"),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpression(String test, Ast.Expression.Function ast, Ast.Expression.Function expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineFunction("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL);
            scope.defineFunction("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL);
        }));
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        // function()
                        new Ast.Expression.Function("function", Arrays.asList()),
                        init(new Ast.Expression.Function("function", Arrays.asList()), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Function(1)",
                        // function(1)
                        new Ast.Expression.Function("function", Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Expression.Function("function", Arrays.asList(init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)))),
                                ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(Environment.Type.INTEGER), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Function(1.0) invalid",
                        // function()
                        new Ast.Expression.Function("function", Arrays.asList(new Ast.Expression.Literal(BigDecimal.ONE))),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Analyzer.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> Analyzer.requireAssignable(target, type));
        }
    }

    private static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
                Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
                Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
                Arguments.of("Integer to Comparable", Environment.Type.COMPARABLE, Environment.Type.INTEGER,  true),
                Arguments.of("Comparable to Comparable", Environment.Type.COMPARABLE, Environment.Type.COMPARABLE,  true),
                Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
                Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false),
                Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
                Arguments.of("Any to Any", Environment.Type.ANY, Environment.Type.ANY, true)
        );
    }

    /**
     * Helper function for tests. If {@param expected} is {@code null}, analysis
     * is expected to throw a {@link RuntimeException}.
     */
    private static <T extends Ast> Analyzer test(T ast, T expected, Scope scope) {
        Analyzer analyzer = new Analyzer(scope);
        if (expected != null) {
            analyzer.visit(ast);
            Assertions.assertEquals(expected, ast);
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(ast));
        }
        return analyzer;
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
