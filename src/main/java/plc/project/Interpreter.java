package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.spec.EncodedKeySpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        // Global variable can be either be a literal or list.
        // if there isn't a value for the global variable to get initialized at declaration, then
        if (!ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
            return Environment.NIL;
        }
        if (ast.getValue().get() instanceof Ast.Expression.Literal) {
            Environment.PlcObject value = Environment.create(((Ast.Expression.Literal) ast.getValue().get()).getLiteral());
            scope.defineVariable(ast.getName(), ast.getMutable(), value);
        }
        else {
            // astValues gets list of Ast.Expression.Literals but you need the Ast.Expression.Literal.getLiteral() (the actual numbers)
            List<Ast.Expression> astValues = ((Ast.Expression.PlcList) ast.getValue().get()).getValues(); //<Ast..., Ast...>
            List<Object> literalValues = new ArrayList<>();
            for (Ast.Expression a : astValues) {
                literalValues.add(((Ast.Expression.Literal) a).getLiteral());
            }
            Environment.PlcObject value = Environment.create(literalValues);
            scope.defineVariable(ast.getName(), ast.getMutable(), value);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        // *** still need to implement for lists? ***
        // first check if receiver is of type Ast.Expression.Access
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException();
        }

        //get variable name from ast
        String name = ((Ast.Expression.Access) ast.getReceiver()).getName();
        // if literal, else list.
        if (ast.getValue() instanceof Ast.Expression.Literal) {
            //lookup and set variable to value in current scope
            // create env, with ast.value [ast.expression.literal=1...] so get literal.
            Environment.PlcObject value = Environment.create(((Ast.Expression.Literal) ast.getValue()).getLiteral());
            scope.lookupVariable(name).setValue(value);
        }
        else {
            //i will finish later
            /*
            // find offset for the list to insert value into
            Ast.Expression offset = (((Ast.Expression.Access) ast.getReceiver()).getOffset()).get();
            //Environment.PlcObject value = Environment.create();

            List<Ast.Expression> astValues = ((Ast.Expression.PlcList) ast.getValue()).getValues(); //<Ast..., Ast...>
            List<Object> literalValues = new ArrayList<>();
            for (Ast.Expression a : astValues) {
                literalValues.add(((Ast.Expression.Literal) a).getLiteral());
            }

             */
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {


        Environment.PlcObject condition = Environment.create(ast.getThenStatements());
        //first parameter is what youre looking to validate

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null)
            return Environment.create(Environment.NIL.getValue());
         return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Ast.Expression left = ast.getLeft(), right = ast.getRight();
        String operator = ast.getOperator();
        if (operator == "&&") {
            if (left instanceof Ast.Expression.Literal && right instanceof Ast.Expression.Literal) {
                boolean l = checkBooleans(left);
                boolean r = checkBooleans(right);
                return Environment.create(l && r);
            }
        } else if (operator == "||") {
            //Neither side is a boolean
            if (!(left instanceof Ast.Expression.Literal) && !(right instanceof Ast.Expression.Literal))
                throw new RuntimeException("Illegal arguments for || operation");
            //Left side is a boolean
            if (left instanceof Ast.Expression.Literal && checkBooleans(left))
                if (checkBooleans(left))
                    return Environment.create(true);
                else throw new RuntimeException("Illegal Left side boolean for || operation");

            //Case Where left hand side was false and right hand side is a literal
            if (right instanceof Ast.Expression.Literal)
                if (checkBooleans(right))
                    return Environment.create(true);
            return Environment.create(false);
        }
        else if(operator == ">" || operator == "<")
        {
            if(left.getClass() == right.getClass() && left instanceof Ast.Expression.Literal)
            {
                String leftName = ((Ast.Expression.Literal) left).getLiteral().getClass().getName();
                if (leftName == "java.math.BigInteger")
                {
                    BigInteger num1 = new BigInteger(((Ast.Expression.Literal) left).getLiteral().toString());
                    BigInteger num2 = new BigInteger(((Ast.Expression.Literal) right).getLiteral().toString());
                    if(operator == "<")
                        return Environment.create(num1.intValue() < num2.intValue());
                    else return Environment.create(num1.intValue() > num2.intValue());
                }
                else if (leftName == "java.math.BigDecimal")
                {
                    BigDecimal num1 = new BigDecimal(((Ast.Expression.Literal) left).getLiteral().toString());
                    BigDecimal num2 = new BigDecimal(((Ast.Expression.Literal) right).getLiteral().toString());
                    if(operator == "<")
                        return Environment.create(num1.doubleValue() < num2.doubleValue());
                    else return Environment.create(num1.doubleValue() > num2.doubleValue());
                }
                //ToDo Strings and Characters might also be comparable. Not sure how to use Comparable method
            }
            else throw new RuntimeException("Types not comparable");
        }
        else if (operator == "==")

            return Environment.create(Objects.equals(left, right));
        else if (operator == "!=")
            return Environment.create(!Objects.equals(left, right));
        else if (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "^") {
            if (left instanceof Ast.Expression.Literal && right instanceof Ast.Expression.Literal) {
                //System.out.println(((Ast.Expression.Literal) left).getLiteral().getClass().getName());
                String leftName = ((Ast.Expression.Literal) left).getLiteral().getClass().getName();
                String rightName = ((Ast.Expression.Literal) right).getLiteral().getClass().getName();
                if (leftName == "java.math.BigInteger") {
                    if (rightName == "java.math.BigInteger") {
                        BigInteger num1 = new BigInteger(((Ast.Expression.Literal) left).getLiteral().toString());
                        BigInteger num2 = new BigInteger(((Ast.Expression.Literal) right).getLiteral().toString());
                        BigInteger res;
                        if(operator == "+")
                            res = BigInteger.valueOf(num1.longValue() + num2.longValue());
                        else if (operator == "-")
                            res = BigInteger.valueOf(num1.longValue() - num2.longValue());
                        else if(operator == "*")
                            res = BigInteger.valueOf(num1.longValue() * num2.longValue());
                        else if(operator == "/") {
                            if (num2.longValue() == 0)
                                throw new RuntimeException("Illegal Division by 0");
                            res = BigInteger.valueOf(num1.longValue() / num2.longValue());
                        }
                        else
                            res = BigInteger.valueOf(num1.longValue() ^ num2.longValue());
                        return Environment.create(res);
                    }
                    else throw new RuntimeException("Adding bigInt to non BigInt object");
                }
                else if (leftName == "java.math.BigDecimal") {
                    if (rightName == "java.math.BigDecimal") {
                        BigDecimal num1 = new BigDecimal(((Ast.Expression.Literal) left).getLiteral().toString());
                        BigDecimal num2 = new BigDecimal(((Ast.Expression.Literal) right).getLiteral().toString());
                        BigDecimal res;
                        if(operator == "+")
                            res = BigDecimal.valueOf(num1.doubleValue() + num2.doubleValue());
                        else if (operator == "-")
                            res = BigDecimal.valueOf(num1.doubleValue() - num2.doubleValue());
                        else if(operator == "*")
                            res = BigDecimal.valueOf(num1.doubleValue() * num2.doubleValue());
                        else if(operator == "/") {
                            if (num2.doubleValue() == 0)
                                throw new RuntimeException("Illegal Division by 0");
                            res = BigDecimal.valueOf(num1.doubleValue() / num2.doubleValue());
                            res = res.setScale(1, RoundingMode.HALF_EVEN);
                        }
                        else
                            throw new RuntimeException("Illegal exponentiation with bigDecimal");
                        return Environment.create(res);
                    }
                    else throw new RuntimeException("Adding BigDecimal to non BigDecimal object");
                }
                else if (leftName == "java.lang.String" && operator == "+") {
                    String LHS = ((Ast.Expression.Literal) left).getLiteral().toString();
                    String RHS = ((Ast.Expression.Literal) right).getLiteral().toString();
                    String res = LHS + RHS;
                    return Environment.create(res);
                };
            }
        }
        throw new UnsupportedOperationException();
    }

    public Boolean checkBooleans(Ast.Expression expr)
    {
        if(((Ast.Expression.Literal) expr).getLiteral().toString() == "true")
            return true;
        else if(((Ast.Expression.Literal) expr).getLiteral().toString() == "false")
            return false;
        else throw new RuntimeException("Illegal Boolean value");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        //if there is offset then it is a list
        /*
        Environment.PlcObject returnedObject;
        if (ast.getOffset().isPresent()) {
            Ast.Expression offset = ast.getOffset().get();
            Environment.PlcObject a = new as;
            returnedObject = scope.lookupVariable(ast.getName()).getValue();

            for (int i = 0; i < 10; i++) {
            }
            return returnedObject.indexof(offset);
        }
        else {
            returnedObject = scope.lookupVariable(ast.getName()).getValue();
        }
        return returnedObject;

         */
        throw new UnsupportedOperationException();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        /*
        List<Ast.Expression> values = ast.getValues();
        List<Object> toReturn = new ArrayList<>();
        for(int i = 0; i < values.size(); i++) {
            if(values.get(i) instanceof Ast.Expression.Literal)
                toReturn.add(((Ast.Expression.Literal) values.get(i)).getLiteral());
        }
        return new Environment.PlcObject(new Scope(null), toReturn);

         */
        //List<Ast.Expression.Literal> values = ((Ast.Expression.Literal) ast.getValues()).getLiteral();

        List<Ast.Expression> astValues = ast.getValues(); //<Ast..., Ast...>
        List<Object> literalValues = new ArrayList<>();
        for (Ast.Expression a : astValues) {
            literalValues.add(((Ast.Expression.Literal) a).getLiteral());
        }
        Environment.PlcObject values = Environment.create(literalValues);
        return values;
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
