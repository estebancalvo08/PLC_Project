package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Hyphen, Underscore and Number within Token", "Legal1-_", true),
                Arguments.of("Starting @", "@hello", true),
                Arguments.of("Not WhiteSpace", "one\"\\\"\\\\n\\\"\"two", false),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Underscore", "_five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Space", " ", false),
                Arguments.of("@ not at start", "Hello@hello", false),
                Arguments.of("Two Identifers", "Two Tokens", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Trailing Zero", "100000000000", true),
                Arguments.of("Trailing Zeros and negative number", "-100000000000", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Decimal", "1.1", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Leading Lone Zero", "0.01", true),
                Arguments.of("Negative Lone Zero", "-0.01", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Extra Zero at front", "00.1", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Negative Leading Decimal", "-.5", false),
                Arguments.of("Integer", "1", false),
                Arguments.of("Extra Decimal", "1.1.1", false),
                Arguments.of("Extra Decimal", "1..1", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Backslash", "\'\\\\\'", true),
                Arguments.of("Invalid Backslash", "\'\\\'", false),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Invalid ' within char", "\''\'", false),
                Arguments.of("Invalid Escapes", "\'\\e\'", false),
                Arguments.of("Undetermined", "\'a", false),
                Arguments.of("Newline Escape", "\'\n\'", false),
                Arguments.of("Newline Escape", "\'\r\'", false),
                Arguments.of("Undetermined", "a'", false),
                Arguments.of("Illegal End", "\'a\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("String with Spaces", "\"Hello\\\\There\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("String within String", "\"\\\"Hello There\\\"\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Unterminated", "\"illegal newline\r\"", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Valid Escape", "\"\\\\b\"", true),
                Arguments.of("Long String", "\"\\'hel\\'lo \\b\\\"\\n\\\"\\'\\n\\'\"", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Double &", "&&", true),
                Arguments.of("Double |", "||", true),
                Arguments.of("Triple &", "&&&", false),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "char x = \'c\';", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "char", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 5),
                        new Token(Token.Type.OPERATOR, "=", 7),
                        new Token(Token.Type.CHARACTER, "\'c\'", 9),
                        new Token(Token.Type.OPERATOR, ";", 12)
                        )),
                Arguments.of("Example 4", "1.1.1;", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.1", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "1", 4),
                        new Token(Token.Type.OPERATOR, ";", 5)
                )),
                Arguments.of("Example 4", "1.toString();", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.IDENTIFIER, "toString", 2),
                        new Token(Token.Type.OPERATOR, "(", 10),
                        new Token(Token.Type.OPERATOR, ")", 11),
                        new Token(Token.Type.OPERATOR, ";", 12)
                )),

                Arguments.of("Example 5", "one\"\\b\"two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.STRING, "\"\\b\"", 3),
                        new Token(Token.Type.IDENTIFIER, "two", 7)
                )),
                Arguments.of("Example 6", "one\\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 5)
                )),
                Arguments.of("Example 7", "0001hello", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.INTEGER, "0", 1),
                        new Token(Token.Type.INTEGER, "0", 2),
                        new Token(Token.Type.INTEGER, "1", 3),
                        new Token(Token.Type.IDENTIFIER, "hello", 4)
                )),
                Arguments.of("Example 8", "-five", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.IDENTIFIER, "five", 1)
                )),
                Arguments.of("Example 9", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                )),
                Arguments.of("Example 10", "-0", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.INTEGER, "0", 1)
                        )),
                Arguments.of("Example 11", "@Identifer@two-!", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "@Identifer", 0),
                        new Token(Token.Type.IDENTIFIER, "@two-", 10),
                        new Token(Token.Type.OPERATOR, "!", 15)
                        )),
                Arguments.of("Example 12", "-+!@$%", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.OPERATOR, "+", 1),
                        new Token(Token.Type.OPERATOR, "!", 2),
                        new Token(Token.Type.IDENTIFIER, "@", 3),
                        new Token(Token.Type.OPERATOR, "$", 4),
                        new Token(Token.Type.OPERATOR, "%", 5)
                )),

                Arguments.of("Example 13", "-1.1.1;", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-1.1", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.INTEGER, "1", 5),
                        new Token(Token.Type.OPERATOR, ";", 6)
                )),
                Arguments.of("Example 14", "1 = 1", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, "=", 2),
                        new Token(Token.Type.INTEGER, "1", 4)
                )),
                Arguments.of("Example 15", "-1 = -1", Arrays.asList(
                        new Token(Token.Type.INTEGER, "-1", 0),
                        new Token(Token.Type.OPERATOR, "=", 3),
                        new Token(Token.Type.INTEGER, "-1", 5)
                )),
                Arguments.of("Example 16", "-1 \"is negative one\"", Arrays.asList(
                        new Token(Token.Type.INTEGER, "-1", 0),
                        new Token(Token.Type.STRING, "\"is negative one\"", 3)
                )),
                Arguments.of("Example 17", "-1 != 1", Arrays.asList(
                        new Token(Token.Type.INTEGER, "-1", 0),
                        new Token(Token.Type.OPERATOR, "!=", 3),
                        new Token(Token.Type.INTEGER, "1", 6)
                )),
                Arguments.of("Example 18", "for (x < 1)", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "for", 0),
                        new Token(Token.Type.OPERATOR, "(", 4),
                        new Token(Token.Type.IDENTIFIER, "x", 5),
                        new Token(Token.Type.OPERATOR, "<", 7),
                        new Token(Token.Type.INTEGER, "1", 9),
                        new Token(Token.Type.OPERATOR, ")", 10)
                )),
                Arguments.of("Example 19", "0(00)", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.OPERATOR, "(", 1),
                        new Token(Token.Type.INTEGER, "0", 2),
                        new Token(Token.Type.INTEGER, "0", 3),
                        new Token(Token.Type.OPERATOR, ")", 4)
                )),
                Arguments.of("Example 20", "\'a\' \" is the first letter in the alphabet\"", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "\'a\'", 0),
                        new Token(Token.Type.STRING, "\" is the first letter in the alphabet\"", 4)
                )),
                Arguments.of("Example 21", "01.1", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.DECIMAL, "1.1", 1)
                )),
                Arguments.of("Example 22", "\'\\n\'", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "\'\\n\'", 0)
                ))
        );
    }


    private static Stream<Arguments> testExceptions() {
        return Stream.of(
                Arguments.of("Example 1", "\"unterminated", 13),
                Arguments.of("Example 2", "\"unt\\erminated", 5),
                Arguments.of("Example 3", "\"unt\n\"", 4),
                Arguments.of("Example 4", "\"unt\r\"", 4),
                Arguments.of("Example 5", "\'\n\'", 1),
                Arguments.of("Example 6", "\''\'", 1),
                Arguments.of("Example 7", "\'abc\'", 2),
                Arguments.of("Example 8", "\'\'", 1),
                Arguments.of("Example 9", "\'a", 2)
                );
    }

    private static void testParseExceptions(String input, int index) {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer(input).lex());
        Assertions.assertEquals(index, exception.getIndex());
    }
    @ParameterizedTest
    @MethodSource
    void testExceptions(String test, String input, int index) {
        testParseExceptions(input, index);
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

}
