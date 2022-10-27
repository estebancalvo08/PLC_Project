package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

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
        //System.out.println("Source scope: " + scope.variables); //Todo Delete
        for(Ast.Global globals : ast.getGlobals())
            visit(globals);
        for(Ast.Function functions : ast.getFunctions())
            visit(functions);
        Environment.Function main = scope.lookupFunction("main",0);
        return main.invoke(new ArrayList<>());
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
        Scope curr = scope;
        //System.out.println("Function scope: " + scope); //Todo Delete
        List<String> params = ast.getParameters();
        scope.defineFunction(ast.getName(), params.size(), args ->
        {
            scope = new Scope(curr);
            for(int i = 0; i < params.size(); i++)
                scope.defineVariable(params.get(i).toString(), true, args.get(i));
            for(Ast.Statement statement: ast.getStatements())
            {
                if(statement instanceof Ast.Statement.Return)
                {
                    try {
                        return visit(statement);
                    }
                    catch(Return e){
                        return e.value;
                    }
                }
                else
                    visit(statement);
            }
            scope = curr.getParent();
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
         visit(ast.getExpression());
         return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional optional = ast.getValue();
        if(optional.isPresent())
            scope.defineVariable(ast.getName(), true, visit((Ast.Expression)optional.get()));
        else
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        //System.out.println("Assignment scope: " + scope.variables); //Todo Delete
        Ast.Expression.Access receiver = requireType(Ast.Expression.Access.class, new Environment.PlcObject(scope, ast.getReceiver()));
        Environment.Variable var = scope.lookupVariable(receiver.getName());
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
        Boolean bool = requireType(Boolean.class, visit(ast.getCondition()));
        List<Ast.Statement> statements;
        scope = new Scope(scope);
        //System.out.println("If scope: " + scope); //Todo Delete
        if(bool)
            statements = ast.getThenStatements();
        else
            statements = ast.getElseStatements();
        for(int i = 0; i < statements.size(); i++)
            visit(statements.get(i));
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        scope = new Scope(scope);
        Object expr = visit(ast.getCondition()).getValue();
        List<Ast.Statement.Case> cases = ast.getCases();
        int i = 0;
        for(; i < cases.size() - 1; i++) {
            if(expr.equals(visit(cases.get(0).getValue().get()).getValue()))
                break;
        }
        scope = scope.getParent();
        return visit(cases.get(i));
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        List<Ast.Statement> expressions = ast.getStatements();
        for(int i = 0; i < expressions.size(); i++)
            visit(expressions.get(i));
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        Boolean conditional = requireType(Boolean.class, visit(ast.getCondition()));
        while(conditional)
        {
            List<Ast.Statement> statements = ast.getStatements();
            for(int i = 0; i < statements.size(); i++)
                visit(statements.get(i));
            //recursion for conditional
            conditional = requireType(Boolean.class, visit(ast.getCondition()));
        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null)
            return Environment.create(Environment.NIL.getValue());
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Ast.Expression left = ast.getLeft(), right = ast.getRight();
        if(operator == "&&")
        {
            Boolean l = requireType(Boolean.class, visit(left));
            Boolean r = requireType(Boolean.class, visit(right));
            return Environment.create(l && r);
        }
        if(operator == "||")
        {
            Boolean l = requireType(Boolean.class, visit(left));
            if(l)
                return Environment.create(true);
            Boolean r = requireType(Boolean.class, visit(right));
            return Environment.create(l || r);
        }
        if(operator == "<" || operator == ">")
        {
            //if less than, compare to will return -1
            int val = operator == "<" ? -1 : 1;
            Environment.PlcObject l = visit(left);
            Environment.PlcObject r = visit(right);
            Boolean type = l.getValue().getClass().equals(r.getValue().getClass());
            if(!type)
                throw new RuntimeException("Illegal Comparison of different classes");
            if(l.getValue().getClass().equals(BigInteger.class))
                return Environment.create(((BigInteger)l.getValue()).compareTo((BigInteger)r.getValue()) == val);
            if(l.getValue().getClass().equals(BigDecimal.class))
                return Environment.create(((BigDecimal)l.getValue()).compareTo((BigDecimal)r.getValue()) == val);
            if(l.getValue().getClass().equals(Character.class))
                return Environment.create(((Character)l.getValue()).compareTo((Character)r.getValue()) == val);
            if(l.getValue().getClass().equals(String.class))
                return Environment.create(((String)l.getValue()).compareTo((String)r.getValue()) == val);
            if(l.getValue().getClass().equals(Boolean.class))
                return Environment.create(((Boolean)l.getValue()).compareTo((Boolean)r.getValue()) == val);
        }
        if(operator == "!=" || operator == "==")
        {
            Boolean equals = operator == "==" ? true : false;
            Environment.PlcObject l = visit(left);
            Environment.PlcObject r = visit(right);
            Boolean type = l.getValue().getClass().equals(r.getValue().getClass());
            if(!type)
                throw new RuntimeException("Illegal Comparison of different classes");
            return equals ? Environment.create(l.equals(r)) : Environment.create(!l.equals(r));
        }
        if(operator == "+")
        {
            Environment.PlcObject l = visit(left);
            Environment.PlcObject r = visit(right);
            if(l.getValue().getClass().equals(String.class) || l.getValue().getClass().equals(String.class))
                return Environment.create(l.getValue().toString() + r.getValue().toString());
            requireType(l.getValue().getClass(), r);
            if(l.getValue().getClass().equals(BigInteger.class))
                return Environment.create(BigInteger.valueOf(((BigInteger)l.getValue()).intValue() + ((BigInteger)r.getValue()).intValue()));
            if(l.getValue().getClass().equals(BigDecimal.class))
                return Environment.create(BigDecimal.valueOf(((BigDecimal)l.getValue()).doubleValue() + ((BigDecimal)r.getValue()).doubleValue()));
            throw new RuntimeException("Illegal addition of Objects");
        }
        if(operator == "-" || operator == "*")
        {
            Boolean mult = operator == "*" ? true : false;
            Environment.PlcObject l = visit(left);
            Environment.PlcObject r = visit(right);
            requireType(l.getValue().getClass(), r);
            if(l.getValue().getClass().equals(BigInteger.class))
                return mult ? Environment.create(BigInteger.valueOf(((BigInteger)l.getValue()).intValue() * ((BigInteger)r.getValue()).intValue())) :
                        Environment.create(BigInteger.valueOf(((BigInteger)l.getValue()).intValue() - ((BigInteger)r.getValue()).intValue()));
            if(l.getValue().getClass().equals(BigDecimal.class))
                return mult ? Environment.create(BigDecimal.valueOf(((BigDecimal)l.getValue()).doubleValue() * ((BigDecimal)r.getValue()).doubleValue())) :
                        Environment.create(BigDecimal.valueOf(((BigDecimal)l.getValue()).doubleValue() - ((BigDecimal)r.getValue()).doubleValue()));
            throw new RuntimeException("Illegal " + (mult ? "multiplication" : "subtraction") + " of objects");
        }
        if(operator == "/")
        {
            Environment.PlcObject l = visit(left);
            Environment.PlcObject r = visit(right);
            requireType(l.getValue().getClass(), r);
            if(l.getValue().getClass().equals(BigInteger.class)) {
                if(r.getValue().equals(BigInteger.ZERO))
                    throw new RuntimeException("Illegal division of Zero");
                return Environment.create(BigInteger.valueOf(((BigInteger) l.getValue()).intValue() / ((BigInteger) r.getValue()).intValue()));
            }
            if(l.getValue().getClass().equals(BigDecimal.class)){
                if(r.getValue().equals(BigDecimal.ZERO))
                    throw new RuntimeException("Illegal division of Zero");
                return Environment.create(BigDecimal.valueOf(((BigDecimal)l.getValue()).doubleValue() / ((BigDecimal)r.getValue()).doubleValue()).setScale(1,BigDecimal.ROUND_HALF_EVEN));
            }
            throw new RuntimeException("Illegal Division of Non-Number objects");
        }
        else {//Case where ^ operator used
            BigInteger l = requireType(BigInteger.class, visit(left));
            BigInteger r = requireType(BigInteger.class, visit(left));
            if((l.longValue() ^ r.longValue()) > (long)Integer.MAX_VALUE)
                return Environment.create(BigInteger.valueOf(Integer.MAX_VALUE));
            return Environment.create(BigInteger.valueOf(l.intValue() ^ r.intValue()));
        }
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        //System.out.println("Access scope: " + scope.variables); //Todo Delete
        Environment.Variable var = scope.lookupVariable(ast.getName());
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
        Environment.Function fun = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.PlcObject> toInvoke = new ArrayList<>();
        for(int i =0; i < args.size(); i++) {
            toInvoke.add(visit(args.get(i)));
        }
        try{
            return fun.invoke(toInvoke);
        }
        catch (Return e)
        {
            return e.value;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> vals = ast.getValues();
        List res = new ArrayList<>();
        for(int i =0; i < vals.size(); i++)
            res.add(requireType(BigInteger.class, visit(vals.get(i))));
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
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }
}