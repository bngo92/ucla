import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

class Lex {
    public static TokenType[] nonTerminalTokenTypes = {
            TokenType.A, TokenType.B, TokenType.C, TokenType.D, TokenType.D_, TokenType.E, TokenType.G,
    };
    public static HashSet<TokenType> nonTerminalSet = new HashSet<TokenType>();
    static int lineNumber;
    static char[] inputs = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-', '(', ')', '$', '#', '\n', ' '};
    static State[][] parse = {
            /*       0        1        2        3        4        5        6        7        8        9        +        -        (        )        $        #        \n       _ */
            /* A */ {State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.B, State.C, State.A, State.A, State.A, State.D, State.A, State.A},
            /* B */ {State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.C, State.A, State.A, State.A, State.D, State.A, State.A},
            /* C */ {State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.A, State.B, State.A, State.A, State.A, State.A, State.D, State.A, State.A},
            /* D */ {State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.D, State.A, State.D},
    };
    static HashMap<State, HashMap<Character, State>> parseTable = new HashMap<State, HashMap<Character, State>>();

    static void init() {
        // Initialize parse table
        for (int i = 0; i < State.values().length; ++i) {
            HashMap<Character, State> h = new HashMap<Character, State>();
            for (int j = 0; j < inputs.length; ++j)
                h.put(inputs[j], parse[i][j]);
            parseTable.put(State.values()[i], h);
        }

        // Cache token types
        Lex.nonTerminalSet.addAll(Arrays.asList(Lex.nonTerminalTokenTypes));

    }

    static ArrayList<Token> lex(InputStream in) throws Exception {
        ArrayList<Token> tokens = new ArrayList<Token>();
        State state = State.A;
        lineNumber = 1;

        int c;
        while ((c = in.read()) != -1) {
            if (Character.isWhitespace(c) && c != '\n')
                c = ' ';
            if (c == '\n')
                ++lineNumber;

            if (state == State.B) {
                if (c == '+')
                    tokens.add(new Token(lineNumber, TokenType.INCROP, "++"));
                else {
                    tokens.add(new Token(lineNumber, TokenType.BINOP, "+"));
                    Token token = getTerminalToken((char) c);
                    if (token != null)
                        tokens.add(token);
                }
            } else if (state == State.C) {
                if (c == '-')
                    tokens.add(new Token(lineNumber, TokenType.INCROP, "--"));
                else {
                    tokens.add(new Token(lineNumber, TokenType.BINOP, "-"));
                    Token token = getTerminalToken((char) c);
                    if (token != null)
                        tokens.add(token);
                }
            } else if (state == State.A) {
                Token token = getTerminalToken((char) c);
                if (token != null)
                    tokens.add(token);
            }
            State nextState = parseTable.get(state).get((char) c);
            if (nextState != null)
                state = nextState;
            else if (state != State.D)
                throw new Exception();
        }
        if (state == State.B) {
            tokens.add(new Token(lineNumber, TokenType.BINOP, "+"));
        } else if (state == State.C) {
            tokens.add(new Token(lineNumber, TokenType.BINOP, "-"));
        }
        tokens.add(new Token(lineNumber, TokenType.EOF));
        return tokens;
    }

    private static Token getTerminalToken(char c) {
        if (c >= '0' && c <= '9')
            return new Token(lineNumber, TokenType.NUM, Character.toString(c));
        else if (c == '(')
            return new Token(lineNumber, TokenType.LPAREN);
        else if (c == ')')
            return new Token(lineNumber, TokenType.RPAREN);
        else if (c == '$')
            return new Token(lineNumber, TokenType.LVALUE, "$");
        return null;
    }

    enum State {
        A, // create object when transitioning to this state
        B, // +
        C, // -
        D, // comment
    }
    enum TokenType {
        A, B, B_, C, D, D_, E, G,
        LPAREN, RPAREN, LVALUE, POSTOP, INCROP, BINOP, NUM, SPACE, EOF
    }

    public static class Token {
        TokenType type;
        int lineNumber;
        String value;

        Token(int lineNumber, TokenType type) {
            this.lineNumber = lineNumber;
            this.type = type;
            if (type != TokenType.SPACE)
                this.value = "";
            else
                this.value = "_";
        }

        Token(int lineNumber, TokenType type, String value) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            if (!value.equals(""))
                return value;
            return type.name();
        }
    }
}
