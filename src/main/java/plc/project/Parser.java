package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        //Read in all the Global Objects first
        while(tokens.has(0))
        {
            if(peek("LIST") || peek("VAR") || peek("VAL"))
                globals.add(parseGlobal());
            else break;
        }
        //Then read in all the Function Objects
        while(tokens.has(0))
        {
            if(peek("FUN"))
                functions.add(parseFunction());
            //Should only be functions or globals, if not then throw error
            else {
                throwError("Illegal Function or Global Declaration");
                return null;
            }
        }
        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if(match("List"))
        {
            return parseList();
        }
        else if(match("VAR"))
        {
            return parseMutable();
        }
        else if(match("VAL"))
        {
            return parseImmutable();
        }
        return null;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        //List<Ast.Expression> expressions = new ArrayList<>();
        Ast.Expression expression;
        if(match(Token.Type.IDENTIFIER, "=" ,"["))
        {
            String name = tokens.get(-3).getLiteral(); // -3 to get Identifier
            expression = parseExpression();
            //Need to Figure out how to add more expressions to the list
            /*while(match(",") && tokens.has(0))
            {
                expressions.add(parseExpression());
            }*/
            if(!match("]"))
            {
                throwError("Illegal end of list");
                return null;
            }
            else return new Ast.Global(name, true, Optional.of(expression));
        }
        else {
            throwError("Illegal token as list Identifier");
            return null;
        }
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
            if (match("LET")) {
                return parseDeclarationStatement();
            } else if (match("SWITCH")) {
                return parseSwitchStatement();
            } else if (match("IF")) {
                return parseIfStatement();
            } else if (match("WHILE")) {
                return parseWhileStatement();
            } else if (match("RETURN")) {
                return parseReturnStatement();
            } else {
                Ast.Expression left = parseExpression();
                if (match("=")) {
                    Ast.Expression right = parseExpression();
                    if (match(";")) return new Ast.Statement.Assignment(left, right);
                    else throwError("Illegal Assignment statement");
                }
                if (match(";")) return new Ast.Statement.Expression(left);
                else throwError("Illegal Assignment statement");
            }
            return null;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        if(tokens.has(0)) return parseLogicalExpression();
        else throw new ParseException("Parse Expression contains Illegal token index", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression left = parseComparisonExpression();
        Ast.Expression right;
        while(match("&&") || match("||"))
        {
            String op = tokens.get(-1).getLiteral();
            right = parseComparisonExpression();
            if(!(peek("&&") || peek("||")))
                return new Ast.Expression.Binary(op, left, right);
            else left = new Ast.Expression.Binary(op, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();
        Ast.Expression right;
        while(match("<") || match(">") || match("==") || match("!="))
        {
            String op = tokens.get(-1).getLiteral();
            right = parseAdditiveExpression();
            if(!(peek("<") || peek(">") || peek("==") || peek("!=")))
                return new Ast.Expression.Binary(op, left, right);
            else left = new Ast.Expression.Binary(op, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();
        Ast.Expression right;
        while(match("+") || match("-"))
        {
            String op = tokens.get(-1).getLiteral();
            right = parseMultiplicativeExpression();
            if(!(peek("+") || peek("-")))
                return new Ast.Expression.Binary(op, left, right);
            else left = new Ast.Expression.Binary(op, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();
        Ast.Expression right;
        while(match("*") || match("/") || match("^"))
        {
            String op = tokens.get(-1).getLiteral();
            right = parsePrimaryExpression();
            if(!(peek("*") || peek("/") || peek("^")))
                return new Ast.Expression.Binary(op, left, right);
            else left = new Ast.Expression.Binary(op, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if(match("TRUE"))
            return new Ast.Expression.Literal(true);
        else if(match("FALSE"))
            return new Ast.Expression.Literal(false);
        else if(match("NIL"))
            return new Ast.Expression.Literal(null);
        //Because the match advances the index by 1, want to get(-1) to go back to token that was read
        else if(match(Token.Type.INTEGER))
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        else if(match(Token.Type.DECIMAL))
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        else if(match(Token.Type.CHARACTER))
        {
            String toReturn = tokens.get(-1).getLiteral();
            toReturn = toReturn.replace("'", "");
            return new Ast.Expression.Literal(new Character(toReturn.charAt(0)));
        }
        else if(match(Token.Type.STRING))
        {
            String toReturn = tokens.get(-1).getLiteral();
            toReturn = toReturn.replace("\"", "");
            toReturn = toReturn.replaceAll("\\\\b", "\b");
            toReturn = toReturn.replaceAll("\\\\n", "\n");
            toReturn = toReturn.replaceAll("\\\\r", "\r");
            toReturn = toReturn.replaceAll("\\\\t", "\t");
            return new Ast.Expression.Literal(new String(toReturn));
        }
        else if(match("("))
        {

            Ast.Expression expression = parseExpression();
            if(match(")"))
                return new Ast.Expression.Group(expression);
            else throwError("Illegal Grouping of Expression");
        }
        else if(match(Token.Type.IDENTIFIER))
        {
            String Name = tokens.get(-1).getLiteral();
            if(match("(")){
                List<Ast.Expression> params = new ArrayList<>();
                while(tokens.has(0) && !peek(")"))
                {
                    params.add(parseExpression());
                    if(peek(",", ")")) throw new ParseException("Illegal end of function call", tokens.get(0).getIndex() + 1);
                    if(!match(",") ) break;
                }
                if(match(")"))
                    return new Ast.Expression.Function(Name, params);
                else throwError("Illegal End of Function Call");
            }
            if(match("["))
            {
                Ast.Expression expression = parseExpression();
                if(match("]"))
                    return new Ast.Expression.Access(Optional.of(expression), Name);
                else throwError("Illegal Closing of Access Operator");
            }
            return new Ast.Expression.Access(Optional.empty(), Name);
        }
        throwError("Illegal Operator as expression");
        return null;
    }

    private void throwError(String error) throws ParseException
    {
        if(tokens.has(0))
            throw new ParseException(error, tokens.get(0).getIndex());
        else throw new ParseException(error, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }


    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
