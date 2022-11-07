package plc.project;

import org.omg.CORBA.portable.ValueInputStream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        if(!(ast.getExpression() instanceof  Ast.Expression.Function))
            throw new RuntimeException("Statement Expression is not a function");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() instanceof Boolean)
            ast.setType(Environment.Type.BOOLEAN);
        else if(ast.getLiteral() instanceof Character)
            ast.setType(Environment.Type.CHARACTER);
        else if(ast.getLiteral() instanceof String)
            ast.setType(Environment.Type.STRING);
        else if(ast.getLiteral() == null)
            ast.setType(Environment.Type.NIL);
        else if(ast.getLiteral() instanceof BigInteger)
        {
            BigInteger integer = (BigInteger) ast.getLiteral();
            if(integer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 || integer.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0)
                throw new RuntimeException("Integer out of bounds exception");
            ast.setType(Environment.Type.INTEGER);
        }
        else if(ast.getLiteral() instanceof BigDecimal)
        {
            BigDecimal decimal = (BigDecimal) ast.getLiteral();
            if(decimal.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 || decimal.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0)
                throw new RuntimeException("Integer out of bounds exception");
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if(!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("Illegal grouping. Not binary expression");
        else
            ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Ast.Expression LHS = ast.getLeft();
        Ast.Expression RHS = ast.getRight();
        //Assign values before starting the Binary operations
        visit(RHS);
        visit(LHS);
        if(operator.equals("&&") || operator.equals("||"))
        {
            requireAssignable(Environment.Type.BOOLEAN, LHS.getType());
            requireAssignable(Environment.Type.BOOLEAN, RHS.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(operator.equals("<") || operator.equals(">") || operator.equals("==") || operator.equals("!="))
        {
            //check that LHS and RHS of the same type
            requireAssignable(LHS.getType(), RHS.getType());
            if(LHS.getType().equals(Environment.Type.INTEGER) || LHS.getType().equals(Environment.Type.DECIMAL) || LHS.getType().equals(Environment.Type.STRING) || LHS.getType().equals(Environment.Type.CHARACTER))
                ast.setType(Environment.Type.BOOLEAN);
            else throw new RuntimeException("Illegal comparison of Non-Comparable types");
        }
        else if(operator.equals("+"))
        {
            //check that LHS and RHS of the same type
            if(LHS.getType().equals(Environment.Type.STRING) || RHS.getType().equals(Environment.Type.STRING))
                ast.setType(Environment.Type.STRING);
            else{
                //Check that both sides are the same type
                requireAssignable(LHS.getType(), RHS.getType());
                if(LHS.getType().equals(Environment.Type.INTEGER))
                    ast.setType(Environment.Type.INTEGER);
                else if(LHS.getType().equals(Environment.Type.DECIMAL))
                    ast.setType(Environment.Type.DECIMAL);
                else{
                    throw new RuntimeException("Illegal addition of types");
                }
            }
        }
        else if(operator.equals("-") || operator.equals("*") || operator.equals("/"))
        {
            //check that LHS and RHS of the same type
            requireAssignable(LHS.getType(), RHS.getType());
            if(LHS.getType().equals(Environment.Type.INTEGER))
                ast.setType(Environment.Type.INTEGER);
            else if(LHS.getType().equals(Environment.Type.DECIMAL))
                ast.setType(Environment.Type.DECIMAL);
            else{
                throw new RuntimeException("Illegal " + operator + " of types");
            }
        }
        else if(operator.equals("^"))
        {
            //check that LHS and RHS of the same type
            requireAssignable(Environment.Type.INTEGER, LHS.getType());
            requireAssignable(Environment.Type.INTEGER, RHS.getType());
            ast.setType(Environment.Type.INTEGER);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(!target.equals(type))
            throw new RuntimeException("Types do not match. Target is of type: " + target.getName() + ". RHS is of type: " + type.getName());
    }

}
