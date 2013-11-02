import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class Parse {
    public static Lex.TokenType[][][] rules = {
            {{Lex.TokenType.A}, {Lex.TokenType.NUM, Lex.TokenType.LPAREN, Lex.TokenType.LVALUE, Lex.TokenType.INCROP}, {Lex.TokenType.B}},
            {{Lex.TokenType.B}, {Lex.TokenType.NUM, Lex.TokenType.LPAREN, Lex.TokenType.LVALUE, Lex.TokenType.INCROP}, {Lex.TokenType.C, Lex.TokenType.E, Lex.TokenType.D, Lex.TokenType.B_}},
            {{Lex.TokenType.B_}, {Lex.TokenType.NUM, Lex.TokenType.LPAREN, Lex.TokenType.LVALUE}, {Lex.TokenType.SPACE, Lex.TokenType.E, Lex.TokenType.D, Lex.TokenType.B_}},
            {{Lex.TokenType.B_}, {Lex.TokenType.BINOP}, {Lex.TokenType.BINOP, Lex.TokenType.C, Lex.TokenType.E, Lex.TokenType.D, Lex.TokenType.B_}},
            {{Lex.TokenType.B_}, {Lex.TokenType.RPAREN, Lex.TokenType.EOF}, null},
            {{Lex.TokenType.C}, {Lex.TokenType.INCROP}, {Lex.TokenType.INCROP, Lex.TokenType.C}},
            {{Lex.TokenType.C}, {Lex.TokenType.NUM, Lex.TokenType.LPAREN, Lex.TokenType.LVALUE}, null},
            {{Lex.TokenType.D}, {Lex.TokenType.INCROP}, {Lex.TokenType.INCROP, Lex.TokenType.D}},
            {{Lex.TokenType.D}, {Lex.TokenType.NUM, Lex.TokenType.BINOP, Lex.TokenType.LPAREN, Lex.TokenType.RPAREN, Lex.TokenType.LVALUE, Lex.TokenType.EOF}, null},
            {{Lex.TokenType.E}, {Lex.TokenType.NUM}, {Lex.TokenType.NUM}},
            {{Lex.TokenType.E}, {Lex.TokenType.LPAREN}, {Lex.TokenType.LPAREN, Lex.TokenType.B, Lex.TokenType.RPAREN}},
            {{Lex.TokenType.E}, {Lex.TokenType.LVALUE}, {Lex.TokenType.LVALUE, Lex.TokenType.G, Lex.TokenType.E}},
            {{Lex.TokenType.G}, {Lex.TokenType.INCROP}, {Lex.TokenType.INCROP, Lex.TokenType.G}},
            {{Lex.TokenType.G}, {Lex.TokenType.NUM, Lex.TokenType.LPAREN, Lex.TokenType.LVALUE}, null},
    };
    public static HashMap<Lex.TokenType, HashMap<Lex.TokenType, ArrayList<Lex.TokenType>>> ruleTable = new HashMap<Lex.TokenType, HashMap<Lex.TokenType, ArrayList<Lex.TokenType>>>();

    static ArrayList<Lex.Token> parse(ArrayList<Lex.Token> tokens) throws Exception {
        ArrayList<Lex.Token> tokens1 = new ArrayList<Lex.Token>();
        Stack<Lex.Token> opStack = new Stack<Lex.Token>();

        Stack<Lex.TokenType> tokenStack = new Stack<Lex.TokenType>();
        tokenStack.push(Lex.TokenType.A);
        while (!tokenStack.isEmpty()) {
            Lex.TokenType lhs = tokenStack.pop();
            Lex.Token token = tokens.get(0);
            Lex.lineNumber = token.lineNumber;
            ArrayList<Lex.TokenType> rhs = null;
            if (lhs != Lex.TokenType.RPAREN)
                rhs = new ArrayList<Lex.TokenType>(ruleTable.get(lhs).get(token.type));

            while (lhs == Lex.TokenType.RPAREN || (!rhs.isEmpty() && ruleTable.get(rhs.get(0)) == null)) {
                if ((lhs == Lex.TokenType.RPAREN && token.type == Lex.TokenType.RPAREN) || rhs.remove(0) != Lex.TokenType.SPACE)
                    tokens.remove(0);
                else
                    token = new Lex.Token(Lex.lineNumber, Lex.TokenType.SPACE);

                if (token.type == Lex.TokenType.NUM) {
                    tokens1.add(token);
                } else {
                    if (token.type == Lex.TokenType.INCROP) {
                        if (lhs == Lex.TokenType.C) {
                            token.value = token.value + "_";
                        } else if (lhs == Lex.TokenType.G) {
                            token.type = Lex.TokenType.LVALUE;
                            token.value = token.value + "_";
                        } else if (lhs == Lex.TokenType.D) {
                            token.type = Lex.TokenType.POSTOP;
                            token.value = "_" + token.value;
                        }
                    }

                    if (token.type == Lex.TokenType.LPAREN) {
                        opStack.push(token);
                    } else if (token.type == Lex.TokenType.RPAREN) {
                        while (opStack.peek().type != Lex.TokenType.LPAREN)
                            tokens1.add(opStack.pop());
                        opStack.pop();
                        break;
                    } else if (token.type != Lex.TokenType.EOF) {
                        while (!opStack.isEmpty() &&
                                (((token.type == Lex.TokenType.BINOP || token.type == Lex.TokenType.SPACE || token.type == Lex.TokenType.POSTOP) && token.type.ordinal() == opStack.peek().type.ordinal()) ||
                                        (token.type.ordinal() > opStack.peek().type.ordinal() && token.type != Lex.TokenType.LVALUE && opStack.peek().type != Lex.TokenType.LPAREN)))
                            tokens1.add(opStack.pop());
                        opStack.push(token);
                    }
                }
            }

            if (rhs != null) {
                Collections.reverse(rhs);
                for (Lex.TokenType ruleType : rhs)
                    tokenStack.push(ruleType);
            }
        }
        while (!opStack.isEmpty())
            tokens1.add(opStack.pop());
        return tokens1;
    }

    public static void main(String[] args) throws IOException {
        Lex.init();
        init();

        try {
            InputStream in = System.in;
            ArrayList<Lex.Token> tokens = Lex.lex(in);
            tokens = parse(tokens);

            StringBuilder stringBuilder = new StringBuilder();
            String delimiter = "";
            for (Lex.Token token : tokens) {
                stringBuilder.append(delimiter);
                delimiter = " ";
                stringBuilder.append(token);
            }
            System.out.println(stringBuilder);
            System.out.println("Expression parsed successfully");
        } catch (Exception e) {
            System.out.printf("Parse error in line %d\n", Lex.lineNumber);
        }
    }

    private static void init() {
        // Initialize parser table
        for (Lex.TokenType[][] rule : rules) {
            HashMap<Lex.TokenType, ArrayList<Lex.TokenType>> h1 = ruleTable.get(rule[0][0]);
            if (h1 == null) {
                h1 = new HashMap<Lex.TokenType, ArrayList<Lex.TokenType>>();
                ruleTable.put(rule[0][0], h1);
            }

            ArrayList<Lex.TokenType> a;
            if (rule[2] != null)
                a = new ArrayList<Lex.TokenType>(Arrays.asList(rule[2]));
            else
                a = new ArrayList<Lex.TokenType>();
            for (Lex.TokenType input : rule[1])
                h1.put(input, a);
        }
    }
}
