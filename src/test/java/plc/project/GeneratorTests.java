package plc.project;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, Ast.Source ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                ),
                Arguments.of("Multiple Functions",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        new Ast.Source(
                                Arrays.asList(),

                                Arrays.asList(
                                        init(new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)))))
                                        , ast -> ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))),
                                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                        ),
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int fun() {",
                                "        return 1;",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )),
                        Arguments.of("Multiple Globals",
                                //Global(String name, String typeName, boolean mutable, Optional<Ast.Expression> value)
                                //Variable(String name, String jvmName, Type type, boolean mutable, PlcObject value)
                                new Ast.Source(
                                        Arrays.asList(init(new Ast.Global("x", "Integer", true, Optional.empty()),
                                                ast->ast.setVariable(new Environment.Variable("x", "x", Environment.Type.INTEGER, true, Environment.NIL))),
                                                init(new Ast.Global("y", "Integer", true, Optional.of(init(new Ast.Expression.Literal(BigInteger.TEN), ast->ast.setType(Environment.Type.INTEGER)))),
                                                        ast->ast.setVariable(new Environment.Variable("y", "y", Environment.Type.INTEGER, true, Environment.NIL)))),

                                        Arrays.asList(
                                                init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(BigInteger.ZERO), ast -> ast.setType(Environment.Type.INTEGER)))
                                                )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL))))
                                ),
                                String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    int x;",
                                "    int y = 10;",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                                )
                        )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGlobal(String test, Ast.Global ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                //Global(String name, String typeName, boolean mutable, Optional<Ast.Expression> value)
                Arguments.of("List",
                        // LIST list: Decimal = [1.0, 2.0];
                        init(new Ast.Global("list", "Decimal", true,
                                        Optional.of(init(new Ast.Expression.PlcList(Arrays.asList(init(new Ast.Expression.Literal(new BigDecimal("1.0")), ast ->ast.setType(Environment.Type.DECIMAL)), init(new Ast.Expression.Literal(new BigDecimal("2.0")), ast -> ast.setType(Environment.Type.DECIMAL)))),
                                                ast -> ast.setType(Environment.Type.DECIMAL)))),
                                ast ->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double[] list = {1.0, 2.0};"
                ),
                Arguments.of("List with no values",
                        // LIST list: Decimal = []; make sure you're using jvm name
                        init(new Ast.Global("list", "Integer", true,
                                        Optional.of(init(new Ast.Expression.PlcList(Arrays.asList()),
                                                ast -> ast.setType(Environment.Type.INTEGER)))),
                                ast ->ast.setVariable(new Environment.Variable("list", "nums", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int[] nums = {};"
                ),
                Arguments.of("No declaration - mutable",
                        // Var x: String;
                        init(new Ast.Global("x", "String", true, Optional.empty()),
                                ast ->ast.setVariable(new Environment.Variable("x", "x", Environment.Type.STRING, true, Environment.NIL))),
                        "String x;"
                ),
                Arguments.of("Declaration - immutable",
                        // Val y : Boolean = TRUE && FALSE
                        init(new Ast.Global("y", "Boolean", false, Optional.of(init(new Ast.Expression.Binary("&&",
                                        init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                        init(new Ast.Expression.Literal(false), ast->ast.setType(Environment.Type.BOOLEAN))),
                                        ast->ast.setType(Environment.Type.BOOLEAN)))),
                                ast ->ast.setVariable(new Environment.Variable("y", "y", Environment.Type.BOOLEAN, true, Environment.NIL))),
                        "final boolean y = true && false;"
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunction(String test, Ast.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                Arguments.of("area",
//                double area(double radius) {
//                    return 3.14 * radius * radius;
//                }
                        init(new Ast.Function("area", Arrays.asList("radius"), Arrays.asList("Decimal"), Optional.of("Decimal"), Arrays.asList(
                                        new Ast.Statement.Return(
                                                init(new Ast.Expression.Binary("*",
                                                        init(new Ast.Expression.Binary("*",
                                                                init(new Ast.Expression.Access(Optional.empty(), "radius"), ast ->  ast.setVariable(new Environment.Variable("radius", "radius", Environment.Type.DECIMAL, true, Environment.NIL))),
                                                                init(new Ast.Expression.Access(Optional.empty(), "radius"), ast ->  ast.setVariable(new Environment.Variable("radius", "radius", Environment.Type.DECIMAL, true, Environment.NIL)))),
                                                                ast->ast.setType(Environment.Type.DECIMAL)),
                                                        init(new Ast.Expression.Literal(new BigDecimal("3.14")), ast -> ast.setType(Environment.Type.DECIMAL))
                                                ), ast -> ast.setType(Environment.Type.DECIMAL))
                                        ))),
                                ast -> ast.setFunction(new Environment.Function("area", "area", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL, args -> Environment.NIL))),
                        String.join(System.lineSeparator(),
                                "double area(double radius) {",
                                "    return radius * radius * 3.14;",
                                "}"
                        )

                ),
                Arguments.of("empty statement",
                        init(new Ast.Function("area", Arrays.asList("radius"), Arrays.asList("Decimal"), Optional.of("Decimal"), Arrays.asList()),
                                ast -> ast.setFunction(new Environment.Function("area", "area", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.DECIMAL, args -> Environment.NIL))),
                        "double area(double radius) {}"
                ),
                Arguments.of("multiple statements",
                        init(new Ast.Function("fun", Arrays.asList("x","y","z"), Arrays.asList("Integer", "Decimal", "String"), Optional.empty(), Arrays.asList(
                                new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                        init(new Ast.Expression.Access(Optional.empty(), "x"), ast->ast.setVariable(
                                                new Environment.Variable("x","x", Environment.Type.INTEGER, true, Environment.NIL)
                                        ))
                                )), ast->ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args->Environment.NIL)))),

                                new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                        init(new Ast.Expression.Access(Optional.empty(), "y"), ast->ast.setVariable(
                                                new Environment.Variable("y","y", Environment.Type.DECIMAL, true, Environment.NIL)
                                        ))
                                )), ast->ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.DECIMAL), Environment.Type.NIL, args->Environment.NIL)))),

                                new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                        init(new Ast.Expression.Access(Optional.empty(), "z"), ast->ast.setVariable(
                                                new Environment.Variable("z","z", Environment.Type.STRING, true, Environment.NIL)
                                        ))
                                )), ast->ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.STRING), Environment.Type.NIL, args->Environment.NIL)))))),
                                ast -> ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING), Environment.Type.NIL, args -> Environment.NIL))),
                        String.join(System.lineSeparator(),
                                "Void fun(int x, double y, String z) {",
                                "    System.out.println(x);",
                                "    System.out.println(y);",
                                "    System.out.println(z);",
                                "}"
                        )

                ),
                Arguments.of("Indent after switch ",
                        init(new Ast.Function("fun", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
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
                        ), new Ast.Statement.Return(init(new Ast.Expression.Literal(null), ast->ast.setType(Environment.Type.NIL))))),
                                ast -> ast.setFunction(new Environment.Function("test", "fun", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL))),
                String.join(System.lineSeparator(),
                        "Void fun() {",
                        "    switch (letter) {",
                        "        case 'y':",
                        "            System.out.println(\"yes\");",
                        "            letter = 'n';",
                        "            break;",
                        "        default:",
                        "            System.out.println(\"no\");",
                        "    }",
                        "    return null;",
                        "}"
                ))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExpression(String test, Ast.Statement.Expression ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testExpression() {
        return Stream.of(
                Arguments.of("Print Hello World",
                        // print("Hello, World!")
                        new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                init(new Ast.Expression.Literal("Hello World!"), ast->ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL)))),
                        "System.out.println(\"Hello World!\");"
                ),
                Arguments.of("Number",
                        // print("Hello, World!")
                        new Ast.Statement.Expression(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER))),
                        "1;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int name;"
                ),
                Arguments.of("Supertype",
                        // LET val : Integer = 10;
                        init(new Ast.Statement.Declaration("str", Optional.of("Comparable"), Optional.of(
                                init(new Ast.Expression.Literal("string"),ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setVariable(new Environment.Variable("str", "str", Environment.Type.COMPARABLE, true, Environment.NIL))),
                        "Comparable str = \"string\";"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(new BigDecimal("1.0")),ast -> ast.setType(Environment.Type.DECIMAL))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.DECIMAL, true, Environment.NIL))),
                        "double name = 1.0;"
                ),
                Arguments.of("Both type name and value declared",
                        // LET val : Integer = 10;
                        init(new Ast.Statement.Declaration("val", Optional.of("Integer"), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.TEN),ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "val", Environment.Type.INTEGER, true, Environment.NIL))),
                        "int val = 10;"
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAssignment(String test, Ast.Statement.Assignment ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAssignment() {
        return Stream.of(
                //Variable(String name, String jvmName, Type type, boolean mutable, PlcObject value)
                Arguments.of("Variable = \"Hello World\"",
                        // TRUE && FALSE
                        new Ast.Statement.Assignment(init(new Ast.Expression.Access(Optional.empty(), "variable"),
                                ast->ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.STRING, true, Environment.NIL))),
                                init(new Ast.Expression.Literal("Hello World"), ast-> ast.setType(Environment.Type.STRING))),
                        "variable = \"Hello World\";"
                ),
                Arguments.of("JvmName = \"Hello World\"",
                        new Ast.Statement.Assignment(init(new Ast.Expression.Access(Optional.empty(), "variable"),
                                ast->ast.setVariable(new Environment.Variable("variable", "JvmName", Environment.Type.STRING, true, Environment.NIL))),
                                init(new Ast.Expression.Literal("Hello World"), ast-> ast.setType(Environment.Type.STRING))),
                        "JvmName = \"Hello World\";"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF expr DO
                        //     stmt;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt"), ast -> ast.setVariable(new Environment.Variable("stmt", "stmt", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt;",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF expr DO
                        //     stmt1;
                        // ELSE
                        //     stmt2;
                        // END
                        new Ast.Statement.If(
                                init(new Ast.Expression.Access(Optional.empty(), "expr"), ast -> ast.setVariable(new Environment.Variable("expr", "expr", Environment.Type.BOOLEAN, true, Environment.NIL))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt1"), ast -> ast.setVariable(new Environment.Variable("stmt1", "stmt1", Environment.Type.NIL, true, Environment.NIL))))),
                                Arrays.asList(new Ast.Statement.Expression(init(new Ast.Expression.Access(Optional.empty(), "stmt2"), ast -> ast.setVariable(new Environment.Variable("stmt2", "stmt2", Environment.Type.NIL, true, Environment.NIL)))))
                        ),
                        String.join(System.lineSeparator(),
                                "if (expr) {",
                                "    stmt1;",
                                "} else {",
                                "    stmt2;",
                                "}"
                        )
                ),
                Arguments.of("Nested If",
                        new Ast.Statement.If(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.If(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                        Arrays.asList(new Ast.Statement.Return(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)))), Arrays.asList())),
                                Arrays.asList()
                        ),
                        String.join(System.lineSeparator(),
                                "if (true) {",
                                "    if (true) {",
                                "        return true;",
                                "    }",
                                "}"
                        )
                ),
                Arguments.of("Nested else",
                        new Ast.Statement.If(init(new Ast.Expression.Literal(false), ast->ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.Return(init(new Ast.Expression.Literal(false), ast->ast.setType(Environment.Type.BOOLEAN)))),
                                Arrays.asList(new Ast.Statement.If(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                        Arrays.asList(new Ast.Statement.Return(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)))),
                                        Arrays.asList()))
                        ),
                        String.join(System.lineSeparator(),
                                "if (false) {",
                                "    return false;",
                                "} else {",
                                "    if (true) {",
                                "        return true;",
                                "    }",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSwitchStatement(String test, Ast.Statement.Switch ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Switch",
                        // SWITCH letter
                        //     CASE 'y':
                        //         print("yes");
                        //         letter = 'n';
                        //     DEFAULT
                        //         print("no");
                        // END
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
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.NIL))),
                                                                init(new Ast.Expression.Literal('a'), ast -> ast.setType(Environment.Type.CHARACTER))
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.of(init(new Ast.Expression.Literal('n'), ast -> ast.setType(Environment.Type.CHARACTER))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        ),
                                                        new Ast.Statement.Assignment(
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.NIL))),
                                                                init(new Ast.Expression.Literal('b'), ast -> ast.setType(Environment.Type.CHARACTER))
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
                        ),
                        String.join(System.lineSeparator(),
                                "switch (letter) {",
                                "    case 'y':",
                                "        System.out.println(\"yes\");",
                                "        letter = 'a';",
                                "        break;",
                                "    case 'n':",
                                "        System.out.println(\"no\");",
                                "        letter = 'b';",
                                "        break;",
                                "    default:",
                                "        System.out.println(\"no\");",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testWhileStatement(String test, Ast.Statement.While ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("while (true) {}",
                        new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList()),
                        "while (true) {}"
                        ),
                Arguments.of("Multiple Statements",
                        new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.Return(init(new Ast.Expression.Literal(true), ast-> ast.setType(Environment.Type.BOOLEAN))),
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(false), ast-> ast.setType(Environment.Type.BOOLEAN))))),
                        String.join(System.lineSeparator(),
                                "while (true) {",
                                "    return true;",
                                "    return false;",
                                "}"
                        )
                ),
                Arguments.of("Nested While",
                        new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)), Arrays.asList(
                                new Ast.Statement.While(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)), Arrays.asList(
                                        new Ast.Statement.Return(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN))))
                                ))),
                        String.join(System.lineSeparator(),
                                "while (true) {",
                                "    while (true) {",
                                "        return true;",
                                "    }",
                                "}"
                        )
                )
                );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testReturn(String test, Ast.Statement.Return ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testReturn() {
        return Stream.of(
                Arguments.of("return 5 * 10",
                        // print("Hello, World!")
                        new Ast.Statement.Return(init(new Ast.Expression.Binary("*",
                                init(new Ast.Expression.Literal(new BigInteger("5")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("10")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))),
                        "return 5 * 10;"
                ),
                Arguments.of("return null",
                        // print("Hello, World!")
                        new Ast.Statement.Return(init(new Ast.Expression.Literal(null), ast->ast.setType(Environment.Type.NIL))),
                        "return null;"
                ),
                Arguments.of("return true",
                        // print("Hello, World!")
                        new Ast.Statement.Return(init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN))),
                        "return true;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testLiteral(String test, Ast.Expression.Literal ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testLiteral() {
        return Stream.of(
                Arguments.of("Integer",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)),
                        "1"
                ),
                Arguments.of("Decimal",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal(new BigDecimal("1.2")), ast->ast.setType(Environment.Type.DECIMAL)),
                        "1.2"
                ),
                Arguments.of("String",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal("string"), ast->ast.setType(Environment.Type.STRING)),
                        "\"string\""
                ),
                Arguments.of("Character",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal('c'), ast->ast.setType(Environment.Type.CHARACTER)),
                        "'c'"
                ),
                Arguments.of("Nil",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal(null), ast->ast.setType(Environment.Type.NIL)),
                        "null"
                ),
                Arguments.of("Boolean true",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal(true), ast->ast.setType(Environment.Type.BOOLEAN)),
                        "true"
                ),
                Arguments.of("Boolean false",
                        // print("Hello, World!")
                        init(new Ast.Expression.Literal(false), ast->ast.setType(Environment.Type.BOOLEAN)),
                        "false"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testGroupExpression(String test, Ast.Expression.Group ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("(1)",
                        // TRUE && FALSE
                        new Ast.Expression.Group(init(new Ast.Expression.Literal(BigInteger.ONE), ast-> ast.setType(Environment.Type.INTEGER))),
                        "(1)"
                ),
                Arguments.of("(1 + 10)",
                        new Ast.Expression.Group(init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))),
                        "(1 + 10)"
                ),
                Arguments.of("(Math.pow(2, 3) + 1)",
                        new Ast.Expression.Group(init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Binary("^",
                                                init(new Ast.Expression.Literal(new BigInteger("2")), ast->ast.setType(Environment.Type.INTEGER)),
                                                init(new Ast.Expression.Literal(new BigInteger("3")), ast->ast.setType(Environment.Type.INTEGER))),
                                        ast->ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))),
                        "(Math.pow(2, 3) + 1)"
                ),
                Arguments.of("Nested Grouping",
                        new Ast.Expression.Group(new Ast.Expression.Group(init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)))),
                        "((1))"
                )
        );
    }
    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, Ast.Expression.Binary ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING)),
                        "\"Ben\" + 10"
                ),
                Arguments.of("Exponentiation",
                        // 2 ^ 3
                        init(new Ast.Expression.Binary("^",
                                init(new Ast.Expression.Literal(new BigInteger("2")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("3")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "Math.pow(2, 3)"
                ),
                Arguments.of("Subtraction",
                        init(new Ast.Expression.Binary("-",
                                init(new Ast.Expression.Literal(new BigDecimal("10.2")), ast -> ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal(new BigDecimal("3.9")), ast -> ast.setType(Environment.Type.DECIMAL))
                        ), ast -> ast.setType(Environment.Type.DECIMAL)),
                        "10.2 - 3.9"
                ),
                Arguments.of("Multiplication",
                        init(new Ast.Expression.Binary("*",
                                init(new Ast.Expression.Literal(new BigDecimal("10.2")), ast -> ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal(new BigDecimal("3.9")), ast -> ast.setType(Environment.Type.DECIMAL))
                        ), ast -> ast.setType(Environment.Type.DECIMAL)),
                        "10.2 * 3.9"
                ),
                Arguments.of("Division",
                        init(new Ast.Expression.Binary("/",
                                init(new Ast.Expression.Literal(new BigInteger("4485")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("65")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "4485 / 65"
                ),
                Arguments.of("Or",
                        init(new Ast.Expression.Binary("||",
                                init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(false), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN)),
                        "true || false"
                ),
                Arguments.of("Less than",
                        init(new Ast.Expression.Binary("<",
                                init(new Ast.Expression.Literal(new BigInteger("4485")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("65")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "4485 < 65"
                ),
                Arguments.of("Greater Than",
                        init(new Ast.Expression.Binary(">",
                                init(new Ast.Expression.Literal(new BigInteger("4485")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("65")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "4485 > 65"
                ),
                Arguments.of("Equals",
                        init(new Ast.Expression.Binary("==",
                                init(new Ast.Expression.Literal(new BigInteger("4485")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("65")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "4485 == 65"
                ),
                Arguments.of("Not Equals",
                        init(new Ast.Expression.Binary("!=",
                                init(new Ast.Expression.Literal(new BigInteger("4485")), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("65")), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER)),
                        "4485 != 65"
                ),
                Arguments.of("Nested Binary",
                        init(new Ast.Expression.Binary("+",
                        init(new Ast.Expression.Binary("^",
                                        init(new Ast.Expression.Literal(new BigInteger("2")), ast->ast.setType(Environment.Type.INTEGER)),
                                        init(new Ast.Expression.Literal(new BigInteger("3")), ast->ast.setType(Environment.Type.INTEGER))),
                                ast->ast.setType(Environment.Type.INTEGER)),
                        init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                ), ast -> ast.setType(Environment.Type.INTEGER)),
                "Math.pow(2, 3) + 1")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testAccess(String test, Ast.Expression.Access ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testAccess() {
        return Stream.of(
                Arguments.of("variable",
                        // Make sure you are using JvmName
                        init(new Ast.Expression.Access(Optional.empty(), "variable"), ast->ast.setVariable(new Environment.Variable("variable", "Variable", Environment.Type.NIL, true, Environment.NIL))),
                        "Variable"
                ),
                Arguments.of("list[5]",
                        init(new Ast.Expression.Access(Optional.of(init(new Ast.Expression.Literal(new BigInteger("5")), ast->ast.setType(Environment.Type.INTEGER))), "list"), ast->ast.setVariable(new Environment.Variable("list", "list", Environment.Type.NIL, true, Environment.NIL))),
                        "list[5]"
                ),
                Arguments.of("nums[1+2]",
                        init(new Ast.Expression.Access(Optional.of(init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast->ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigInteger("2")), ast->ast.setType(Environment.Type.INTEGER))
                        ), ast->ast.setType(Environment.Type.INTEGER))), "list"), ast->ast.setVariable(new Environment.Variable("list", "nums", Environment.Type.NIL, true, Environment.NIL))),
                        "nums[1 + 2]"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, Ast.Expression.Function ast, String expected) {
        test(ast, expected);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function("print", Arrays.asList(
                                init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))),
                        "System.out.println(\"Hello, World!\")"
                ),
                Arguments.of("No parameters",
                        init(new Ast.Expression.Function("fun", Arrays.asList())
                                , ast -> ast.setFunction(new Environment.Function("fun", "function", Arrays.asList(), Environment.Type.NIL, args -> Environment.NIL))),
                        "function()"
                ),
                Arguments.of("Multiple Parameters",
                        // print("Hello, World!")
                        init(new Ast.Expression.Function("fun", Arrays.asList(
                                init(new Ast.Expression.Literal(new BigInteger("1")), ast->ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(new BigDecimal("2.5")), ast->ast.setType(Environment.Type.DECIMAL)),
                                init(new Ast.Expression.Literal("word"), ast->ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal('c'), ast->ast.setType(Environment.Type.CHARACTER))
                                ))
                                , ast -> ast.setFunction(new Environment.Function("fun", "fun", Arrays.asList(Environment.Type.INTEGER, Environment.Type.DECIMAL, Environment.Type.STRING, Environment.Type.CHARACTER), Environment.Type.NIL, args -> Environment.NIL))),
                        "fun(1, 2.5, \"word\", 'c')"
                )
        );
    }

    @Test
    void testList() {
        // LIST list: Decimal = [1.0, 1.5, 2.0];
        Ast.Expression.Literal expr1 = new Ast.Expression.Literal(new BigDecimal("1.0"));
        Ast.Expression.Literal expr2 = new Ast.Expression.Literal(new BigDecimal("1.5"));
        Ast.Expression.Literal expr3 = new Ast.Expression.Literal(new BigDecimal("2.0"));
        expr1.setType(Environment.Type.DECIMAL);
        expr2.setType(Environment.Type.DECIMAL);
        expr3.setType(Environment.Type.DECIMAL);

        Ast.Global global = new Ast.Global("list", "Decimal", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(expr1, expr2, expr3))));
        Ast.Global astList = init(global, ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.DECIMAL, true, Environment.create(Arrays.asList(new Double(1.0), new Double(1.5), new Double(2.0))))));

        String expected = new String("double[] list = {1.0, 1.5, 2.0};");
        test(astList, expected);
    }
    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static void test(Ast ast, String expected) {
        StringWriter writer = new StringWriter();
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
