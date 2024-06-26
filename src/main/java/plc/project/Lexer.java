package plc.project;
import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while(chars.has(0)){
                //If whiteSpace, advance and restart count for length
               if(peek("[\b\n\r\t ]"))
                   {chars.advance();chars.skip();}
               //If escape character, make sure it is a valid escape character
               else if(peek("\\\\", "[bnrt]"))
                    {chars.advance();chars.advance();chars.skip();}
               //Else, create a new token
                else
                    tokens.add(lexToken());
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if(peek("[0-9-]"))
            return lexNumber();
        else if(peek("'"))
            return lexCharacter();
       else if(peek("[a-zA-Z@]"))
            return lexIdentifier();
        else if(peek("\""))
            return lexString();
        else
            return lexOperator();
    }

    public Token lexIdentifier() {
        //Because initial character can be @, skip the first character
        chars.advance();
        while(chars.has(0))
        {
            if(peek("[A-Za-z0-9_-]"))
                chars.advance();
            else break;
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        //catch bases illegal cases
        if(peek("\\.") || peek("-", "\\."))
            throw new ParseException("Illegal decimal", chars.index);
        if (peek("-")) {
            if(peek(".","0", "\\.") || peek(".","[1-9]")){chars.advance();}
            else return lexOperator();
            //else throw new ParseException("Cannot have negative zero", chars.index);
        }
        //Check cases where leading number is 0, either decimal or int token
        if(peek("0"))
        {
            chars.advance();
            if(peek("\\."))
                return isDecimal();
            else
                return chars.emit(Token.Type.INTEGER);
        }
        //if token is legal
        while(chars.has(0))
        {
            if(peek("[0-9]"))
                chars.advance();
            else if(peek("\\.", "[0-9]"))
                return isDecimal();
            else break;
        }
        return chars.emit(Token.Type.INTEGER);
    }
    public Token isDecimal()
    {
        chars.advance();
        if(peek("[0-9]"))
            while (peek("[0-9]"))
                chars.advance();
        else throw new ParseException("Illegal decimal value", chars.index);
        return chars.emit(Token.Type.DECIMAL);
    }

    public Token lexCharacter() {
        //Move the char counter past the '
        chars.advance();
        //check for special character
        if(peek("\\\\"))
            lexEscape();
        //if not special character, make sure it is allowed character but not the end /'
        else {
            if (peek("[^'\n\r\\\\]"))
                chars.advance();
            //If character is empty or contains lone \ token
            else throw new ParseException("Illegal character in character Token", chars.index);
        }
         //check that the end of the char ends with /'
        if(peek("'"))
            chars.advance();
        else throw new ParseException("Illegal end to character token", chars.index);
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        //Move the char counter past the '
        chars.advance();
        //check for special character "H"e"l"l"o"
        while(chars.has(0) && !peek("\"")) {
            if (peek("\\\\"))
                lexEscape();
            else if(peek("[\n\r]"))
                throw new ParseException("Illegal newline in string", chars.index);
            else chars.advance();
        }
        if(peek("\""))
            chars.advance();
        else throw new ParseException("Illegal End of String", chars.index);
       //throw new ParseException("Illegal End of String", chars.index);
            return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        chars.advance();
        if(peek("[bnrt'\"\\\\]"))
            chars.advance();
        else throw new ParseException("Illegal escape Character", chars.index);
    }

    public Token lexOperator() {
        if(peek("&", "&") || peek("\\|", "\\|") || peek("!", "=") || peek("=","="))
            match(".",".");
        else chars.advance();
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {

        for(int i = 0; i < patterns.length; i++)
        {
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]))
                return false;
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek)
        {
            for(int i = 0; i < patterns.length;i++)
                chars.advance();
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }
        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
