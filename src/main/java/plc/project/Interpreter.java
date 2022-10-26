package plc.project;

import javax.management.relation.RelationTypeNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.spec.EncodedKeySpec;
import java.util.*;
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
        List<Ast.Global> globals = ast.getGlobals();
        List<Ast.Function> functions = ast.getFunctions();
        for (Ast.Global x : globals) {
            visit(x);
        }
        int count = 0;
        for (Ast.Function y : functions) {
            if (count != 0) {
                visit(y);
            }
        }

        return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
        //return visit(ast.getFunctions().get(0));
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject initial;
        if(ast.getValue().isPresent())
            initial = visit(ast.getValue().get());
        else initial = Environment.NIL;
        scope.defineVariable(ast.getName(), ast.getMutable(), initial);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        //scope.defineFunction(ast.getName(), ast.getParameters().size(), );
        Scope s = new Scope(scope);
        List<String> p = ast.getParameters();
        //make variables with the values of arguments
        for (String name : p) {
            s.defineVariable(name, true, scope.lookupVariable(name).getValue());
        }
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement stat : statements) {
            throw new Return(visit(stat));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional<Ast.Expression> optional = ast.getValue();
        Boolean present = optional.isPresent();
        if (present) {
            Ast.Expression expr = optional.get();

            scope.defineVariable(ast.getName(), true, visit(expr));

        }
        else {
            //
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        Ast.Expression.Access receiver = requireType(Ast.Expression.Access.class, Environment.create(ast.getReceiver()));
        Environment.Variable var = getScope().lookupVariable(receiver.getName());
        if(!var.getMutable())
            throw new RuntimeException("Illegal reassignment of const val");
        //Case where receiver is a var
        if(!receiver.getOffset().isPresent()) {
            var.setValue(visit(ast.getValue()));
        }
        //case where receiver is a list
        else {
            Optional optional = receiver.getOffset();
            BigInteger offset = requireType(BigInteger.class, Environment.create(((Ast.Expression.Literal)optional.get()).getLiteral()));
            List<Object> temp = (List<Object>)(var.getValue().getValue());
            if(offset.intValue() < 0 || offset.intValue() >= temp.size())
                throw new RuntimeException("Illegal access operation, out of bounds exception");
            temp.set(offset.intValue(), visit(ast.getValue()).getValue());
            var.setValue(Environment.create(temp));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        // if condition is true, execute then statements, else execute elseStatements
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        if (condition) {
            //make a new scope, s, with scope as parent scope
            Scope s = new Scope(scope);
            //evaluate the thenStatements one by one
            List<Ast.Statement> statements = ast.getThenStatements();
            for (Ast.Statement x : statements) {
                visit(x);
            }
        }
        else {
            //make a new scope, s, with scope as parent scope
            Scope s = new Scope(scope);
            //evaluate the elseStatements one by one
            List<Ast.Statement> statements = ast.getElseStatements();
            for (Ast.Statement x : statements) {
                visit(x);
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {

        Scope s = new Scope(scope);
        Ast.Expression condition = ast.getCondition();
        List<Ast.Statement.Case> cases = ast.getCases();

        for (Ast.Statement.Case c : cases) {
            if (c.getValue().isPresent() && c.getValue().get().equals(condition)) {
                visit(c);
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement x : statements) {
            visit(x);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        // im attempting to do this idk if its correct
        // check for while condition
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        Scope s = new Scope(scope);
        while (condition) {
            List<Ast.Statement> statements = ast.getStatements();
            for (Ast.Statement x : statements) {
                visit(x);
            }
            //re-evaluate condition
            condition = requireType(Boolean.class, visit(ast.getCondition()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        Ast.Expression value = ast.getValue();
        throw new Return(visit(value));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null)
            return Environment.create(Environment.NIL.getValue());
         return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return Environment.create(visit(ast.getExpression()).getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        Ast.Expression left = ast.getLeft();
        Ast.Expression right = ast.getRight();

        if (op == "&&") {
            Boolean lhs = requireType(Boolean.class, visit(left));
            Boolean rhs = requireType(Boolean.class, visit(right));
            if (lhs && rhs) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        else if (op == "||") {
            Boolean lhs = requireType(Boolean.class, visit(left));
            if (lhs) {
                return Environment.create(true);
            }
            else {
                Boolean rhs = requireType(Boolean.class, visit(right));
                if (rhs) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            }
        }
        else if (op == "<" || op == ">") {
            Comparable lhs = (Comparable) visit(left).getValue();
            Comparable rhs = (Comparable) visit(right).getValue();
            if (lhs.getClass().equals(rhs.getClass())) {
                int value = rhs.compareTo(lhs);
                if (value < 0) {
                    return Environment.create(false);
                }
                else if (value > 0) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            }
            throw new UnsupportedOperationException();
        }
        else if (op == "==" || op == "!=") {
            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs.equals(rhs)) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        else if (op == "+") {

            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs instanceof String) {

                return Environment.create((String) lhs + rhs);
            }
            else {
                if (lhs instanceof BigInteger) {
                    if (rhs instanceof BigInteger) {
                        return Environment.create(((BigInteger) visit(left).getValue()).add((BigInteger) visit(right).getValue()));
                    }
                }
                else if (lhs instanceof BigDecimal) {
                    if (rhs instanceof BigDecimal) {
                        return Environment.create(((BigDecimal) visit(left).getValue()).add((BigDecimal) visit(right).getValue()));
                    }
                }
                throw new UnsupportedOperationException();
            }
        }
        else if (op == "-") {
            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs instanceof BigInteger) {
                if (rhs instanceof BigInteger) {
                    return Environment.create(((BigInteger) visit(left).getValue()).subtract((BigInteger) visit(right).getValue()));
                }
            }
            else if (lhs instanceof BigDecimal) {
                if (rhs instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) visit(left).getValue()).subtract((BigDecimal) visit(right).getValue()));
                }
            }
            throw new UnsupportedOperationException();
        }
        else if (op == "*") {
            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs instanceof BigInteger) {
                if (rhs instanceof BigInteger) {
                    return Environment.create(((BigInteger) visit(left).getValue()).multiply((BigInteger) visit(right).getValue()));
                }
            }
            else if (lhs instanceof BigDecimal) {
                if (rhs instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) visit(left).getValue()).multiply((BigDecimal) visit(right).getValue()));
                }
            }
            throw new UnsupportedOperationException();
        }
        else if (op == "/") {
            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs instanceof BigInteger) {
                if (rhs instanceof BigInteger) {
                    if (rhs.equals(0)) {
                        throw new UnsupportedOperationException();
                    }
                    return Environment.create(((BigInteger) visit(left).getValue()).divide((BigInteger) visit(right).getValue()));
                }
            }
            else if (lhs instanceof BigDecimal) {
                if (rhs instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) visit(left).getValue()).divide((BigDecimal) visit(right).getValue(), RoundingMode.HALF_EVEN));
                }
            }
            throw new UnsupportedOperationException();
        }
        else if (op == "^") {
            Object lhs = visit(left).getValue();
            Object rhs = visit(right).getValue();
            if (lhs instanceof BigInteger) {
                if (rhs instanceof BigInteger) {
                    if (rhs.equals(0)) {
                        throw new UnsupportedOperationException();
                    }
                    return Environment.create(((BigInteger) visit(left).getValue()).pow((int) visit(right).getValue()));
                }
            }
            throw new UnsupportedOperationException();
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
        Environment.Variable var = getScope().lookupVariable(ast.getName());
        Optional optional = ast.getOffset();
        if(optional.isPresent())
        {
            BigInteger offset = requireType(BigInteger.class, Environment.create(((Ast.Expression.Literal)optional.get()).getLiteral()));
            List<Object> temp = (List<Object>)(var.getValue().getValue());
            if(offset.intValue() < 0 || offset.intValue() >= temp.size())
                throw new RuntimeException("Illegal access operation, out of bounds exception");
            return Environment.create(temp.get(offset.intValue()));
        }
        return Environment.create(var.getValue().getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.PlcObject> visitedArgs = new ArrayList<>();
        for (Ast.Expression a : arguments) {
            visitedArgs.add(visit(a));
        }
        //throw new UnsupportedOperationException();
        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(visitedArgs);

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> vals = ast.getValues();
        List res = new ArrayList<>();
        for(int i =0; i < vals.size(); i++)
            res.add(((Ast.Expression.Literal)vals.get(i)).getLiteral());
        return Environment.create(res);
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
    public static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
