package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                ),
                Arguments.of("Variable",
                        Arrays.asList(
                                //expr;
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, ";", 4)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(),"expr"))
                ),
                Arguments.of("Missing Semicolon",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "f", 0)
                        ),
                        null
                ),
                Arguments.of("Function",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "func", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("func", Arrays.asList()))
                ),
                Arguments.of("? Exception",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "?", 0)
                        ),
                        null
                ),
                Arguments.of("(expr not closed off", Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        null
                ),
                Arguments.of("(expr]", Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, "]", 5)
                        ),
                        null)
                ,
                Arguments.of("; with no expression", Arrays.asList(
                                new Token(Token.Type.OPERATOR, ";", 0)
                        ),
                        null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                ),
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = ;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.OPERATOR, ";", 7)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\b'", 0)),
                        new Ast.Expression.Literal('\b')
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\''", 0)),
                        new Ast.Expression.Literal('\'')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                ),
                Arguments.of("NIL Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expression.Literal(null)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Grouped Function",
                        Arrays.asList(
                            new Token(Token.Type.OPERATOR, "(", 0),
                            new Token(Token.Type.IDENTIFIER, "func", 1),
                            new Token(Token.Type.OPERATOR, "(", 5),
                            new Token(Token.Type.IDENTIFIER, "expr1", 6),
                            new Token(Token.Type.OPERATOR, ",", 11),
                            new Token(Token.Type.IDENTIFIER, "expr2", 12),
                            new Token(Token.Type.OPERATOR, ",", 17),
                            new Token(Token.Type.IDENTIFIER, "expr3", 18),
                            new Token(Token.Type.OPERATOR, ")", 23),
                            new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Function("func", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))))
                ,
                //Tests the expression ( func(expr1, expr2, expr3[func2(NIL, expr4)] ) )
                Arguments.of("Test All",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "func", 1),
                                new Token(Token.Type.OPERATOR, "(", 5),
                                new Token(Token.Type.IDENTIFIER, "expr1", 6),
                                new Token(Token.Type.OPERATOR, ",", 11),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18),
                                new Token(Token.Type.OPERATOR, "[", 23),
                                new Token(Token.Type.IDENTIFIER, "func2", 24),
                                new Token(Token.Type.OPERATOR, "(", 29),
                                new Token(Token.Type.IDENTIFIER, "NIL", 30),
                                new Token(Token.Type.OPERATOR, ",", 33),
                                new Token(Token.Type.IDENTIFIER, "expr4", 34),
                                new Token(Token.Type.OPERATOR, ",", 39),
                                new Token(Token.Type.IDENTIFIER, "TRUE", 40),
                                new Token(Token.Type.OPERATOR, ",", 44),
                                new Token(Token.Type.CHARACTER, "\'c\'", 45),
                                new Token(Token.Type.OPERATOR, ",", 49),
                                new Token(Token.Type.DECIMAL, "1.0", 50),
                                new Token(Token.Type.OPERATOR, ")", 53),
                                new Token(Token.Type.OPERATOR, "]", 54),
                                new Token(Token.Type.OPERATOR, ")", 55),
                                new Token(Token.Type.OPERATOR, ")", 56)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Function("func", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(
                                        Optional.of(new Ast.Expression.Function("func2", Arrays.asList(
                                                new Ast.Expression.Literal(null),
                                                new Ast.Expression.Access(Optional.empty(), "expr4"),
                                                new Ast.Expression.Literal(Boolean.TRUE),
                                                new Ast.Expression.Literal('c'),
                                                new Ast.Expression.Literal(BigDecimal.valueOf(1.0))
                                        ))), "expr3")
                        ))))
                ,
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Missing Operand",
                        Arrays.asList(
                                //expr -
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "-", 5)
                        ),
                        null
                ),
                Arguments.of("Priority == and !=",
                        Arrays.asList(
                                //expr1 * expr2 / expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "!=", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 15)
                        ),
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Binary("==",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        )),
                Arguments.of("Priority && and ||",
                        Arrays.asList(
                                //expr1 * expr2 / expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.OPERATOR, "||", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 15)
                        ),
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Binary("&&",
                                        new Ast.Expression.Access(Optional.empty(), "expr1"),
                                        new Ast.Expression.Access(Optional.empty(), "expr2")
                                ),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Trailing Comma", Arrays.asList(
                        //name(expr,)
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "(", 4),
                        new Token(Token.Type.IDENTIFIER, "expr", 5),
                        new Token(Token.Type.OPERATOR, ",", 9),
                        new Token(Token.Type.OPERATOR, ")", 10)
                ), null)
        );
    }

    private static Stream<Arguments> testExpressionExceptions() {
        return Stream.of(
                Arguments.of( "Illegal Binary Subtraction", Arrays.asList(
                                //expr -
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "-", 5)
                        ), 6),
                Arguments.of( "Illegal function declaration, hanging comma", Arrays.asList(
                        //expr -
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.IDENTIFIER, "expr", 6),
                        new Token(Token.Type.OPERATOR, ",", 10),
                        new Token(Token.Type.OPERATOR, ")", 11)
                ), 11),
                Arguments.of( "Illegal Operator at start of expression", Arrays.asList(
                        //expr -
                        new Token(Token.Type.OPERATOR, "?", 0)
                ), 0),
                Arguments.of("Illegal end of function parenthesis", Arrays.asList(
                        //expr -
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "(", 4),
                        new Token(Token.Type.IDENTIFIER, "expr", 5)),9),
                Arguments.of( "Illegal Access operation", Arrays.asList(
                        //expr -
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "(", 4),
                        new Token(Token.Type.IDENTIFIER, "expr", 5),
                        new Token(Token.Type.OPERATOR, "]", 9)),9),
                Arguments.of( "Illegal end of grouping with missing parenthesis", Arrays.asList(
                        //expr -
                        new Token(Token.Type.OPERATOR, "(", 0),
                        new Token(Token.Type.IDENTIFIER, "expr", 1)),5),
                Arguments.of( "Illegal end of grouping with function inside", Arrays.asList(
                        //expr -
                        new Token(Token.Type.OPERATOR, "(", 0),
                        new Token(Token.Type.IDENTIFIER, "func", 1),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.IDENTIFIER, "expr1", 6),
                        new Token(Token.Type.OPERATOR, ",", 11),
                        new Token(Token.Type.IDENTIFIER, "expr2", 12),
                        new Token(Token.Type.OPERATOR, ",", 17),
                        new Token(Token.Type.IDENTIFIER, "expr3", 18),
                        new Token(Token.Type.OPERATOR, ")", 23)),24),
                Arguments.of( "Illegal end of grouping with ] operator",  Arrays.asList(
                        //expr -
                        new Token(Token.Type.OPERATOR, "(", 0),
                        new Token(Token.Type.IDENTIFIER, "func", 1),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.IDENTIFIER, "expr1", 6),
                        new Token(Token.Type.OPERATOR, ",", 11),
                        new Token(Token.Type.IDENTIFIER, "expr2", 12),
                        new Token(Token.Type.OPERATOR, ",", 17),
                        new Token(Token.Type.IDENTIFIER, "expr3", 18),
                        new Token(Token.Type.OPERATOR, ")", 23),
                        new Token(Token.Type.OPERATOR, "]", 29)),29),
                Arguments.of("Missing Comma in the access operation: (func(expr1,expr2,expr3[func2(NIL, expr4, True \'c\' , 1.0)]))", Arrays.asList(
                    new Token(Token.Type.OPERATOR, "(", 0),
                    new Token(Token.Type.IDENTIFIER, "func", 1),
                    new Token(Token.Type.OPERATOR, "(", 5),
                    new Token(Token.Type.IDENTIFIER, "expr1", 6),
                    new Token(Token.Type.OPERATOR, ",", 11),
                    new Token(Token.Type.IDENTIFIER, "expr2", 12),
                    new Token(Token.Type.OPERATOR, ",", 17),
                    new Token(Token.Type.IDENTIFIER, "expr3", 18),
                    new Token(Token.Type.OPERATOR, "[", 23),
                    new Token(Token.Type.IDENTIFIER, "func2", 24),
                    new Token(Token.Type.OPERATOR, "(", 29),
                    new Token(Token.Type.IDENTIFIER, "NIL", 30),
                    new Token(Token.Type.OPERATOR, ",", 33),
                    new Token(Token.Type.IDENTIFIER, "expr4", 34),
                    new Token(Token.Type.OPERATOR, ",", 39),
                    new Token(Token.Type.IDENTIFIER, "TRUE", 40),
                    new Token(Token.Type.CHARACTER, "\'c\'", 45),
                    new Token(Token.Type.OPERATOR, ",", 49),
                    new Token(Token.Type.DECIMAL, "1.0", 50),
                    new Token(Token.Type.OPERATOR, ")", 53),
                    new Token(Token.Type.OPERATOR, "]", 54),
                    new Token(Token.Type.OPERATOR, ")", 55),
                    new Token(Token.Type.OPERATOR, ")", 56)),45)
        );
    }

    private static void testExpressions(List<Token> tokens, int index) {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Parser(tokens).parseExpression());
        Assertions.assertEquals(index, exception.getIndex());
    }
    @ParameterizedTest
    @MethodSource
    void testExpressionExceptions(String test, List<Token> tokens, int index) {
        testExpressions(tokens, index);
    }


    private static Stream<Arguments> testStatementExceptions() {
        return Stream.of(
                Arguments.of( Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "expr", 0),
                        new Token(Token.Type.OPERATOR, "=", 5),
                        new Token(Token.Type.OPERATOR, ";", 7)
                ), 7),
                Arguments.of( Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "name", 0)
                ), 4),
                Arguments.of( Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "=", 5),
                        new Token(Token.Type.IDENTIFIER, "name2", 7)
                        ), 12),
                Arguments.of( Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "name", 0),
                        new Token(Token.Type.OPERATOR, "=", 5),
                        new Token(Token.Type.IDENTIFIER, "name2", 7),
                        new Token(Token.Type.OPERATOR, "=", 13),
                        new Token(Token.Type.IDENTIFIER, "name3", 15)

                ), 13),
                Arguments.of( Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "f", 0)
                ), 1)
        );
    }
    private static void testStatements(List<Token> tokens, int index) {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Parser(tokens).parseStatement());
        Assertions.assertEquals(index, exception.getIndex());
    }
    @ParameterizedTest
    @MethodSource
    void testStatementExceptions(List<Token> tokens, int index) {
        testStatements(tokens, index);
    }
    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
