package plc.project;

import org.omg.CORBA.portable.ValueInputStream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.spec.EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        List<Ast.Global> globals = ast.getGlobals();
        List<Ast.Function> functions = ast.getFunctions();
        for(Ast.Global global : globals)
            visit(global);
        for(Ast.Function function : functions)
            visit(function);
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        Optional value = ast.getValue();
        //If there is a value, visit value and make sure returned value type is same as declaration type
        if(value.isPresent()) {
            //if list, assign type beforehand so PLC list function can check values within the list
            if(value.get() instanceof Ast.Expression.PlcList)
                ((Ast.Expression.PlcList) value.get()).setType(getType(ast.getTypeName()));
            visit(ast.getValue().get());
            if (!ast.getTypeName().equals(ast.getValue().get().getType().getName()))
                throw new RuntimeException("Value type does not match declared variable type");
        }
        Environment.Type type = getType(ast.getTypeName());
        Environment.Variable var = new Environment.Variable(ast.getName(), ast.getName(), type, ast.getMutable() ,Environment.NIL);
        scope.defineVariable(ast.getName(), ast.getName(), type, ast.getMutable(), Environment.NIL);
        ast.setVariable(var);
        return null;
    }
    private Environment.Type getType(String TypeName)
    {
        if(TypeName.equals("Integer"))
            return Environment.Type.INTEGER;
        else if(TypeName.equals("String"))
            return Environment.Type.STRING;
        else if(TypeName.equals("Boolean"))
            return Environment.Type.BOOLEAN;
        else if(TypeName.equals("Character"))
            return Environment.Type.CHARACTER;
        else if(TypeName.equals("Any"))
            return Environment.Type.ANY;
        else if(TypeName.equals("Nil"))
            return Environment.Type.NIL;
        else if(TypeName.equals("Comparable"))
            return Environment.Type.COMPARABLE;
        else if(TypeName.equals("Decimal"))
            return Environment.Type.DECIMAL;
        else throw new RuntimeException("Illegal variable type");
    }
    @Override
    public Void visit(Ast.Function ast) {
        List<Ast.Statement> statements = ast.getStatements();
        List<String> params  = ast.getParameters();
        List<String> paramTypesString = ast.getParameterTypeNames();
        List<Environment.Type> paramTypes = new ArrayList<>();
        String name = ast.getName();
        Environment.Type returnType = Environment.Type.NIL;
        for(int i = 0; i < params.size(); i++) {
            paramTypes.add(getType(paramTypesString.get(i)));
            scope.defineVariable(params.get(i), params.get(i), paramTypes.get(i), true, Environment.NIL);
        }
        if(ast.getReturnTypeName().isPresent())
             returnType = getType(ast.getReturnTypeName().get());
        //public Function(String name, List<String> parameters, List<String> parameterTypeNames, Optional<String> returnTypeName, List<Statement> statements
        //public Environment.Function defineFunction(String name, String jvmName, List<Environment.Type> parameterTypes, Environment.Type returnType, java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> function) {
        scope.defineFunction(name, name, paramTypes, returnType, args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(name, ast.getParameters().size()));
        scope = new Scope(scope);
        for(Ast.Statement statement : statements)
        {
            visit(statement);
            if(statement instanceof Ast.Statement.Return && !((Ast.Statement.Return) statement).getValue().getType().equals(returnType)) {
                throw new RuntimeException("Return type does not match function return type");
            }
        }
        scope = scope.getParent();
        return null;
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
        Optional value = ast.getValue();
        Environment.Type type;
        //let var = value
        if(value.isPresent() && !ast.getTypeName().isPresent()) {
            visit(ast.getValue().get());
            type = (ast.getValue().get().getType());
        }
        //let var : type
        else if(!value.isPresent() && ast.getTypeName().isPresent())
            type = getType(ast.getTypeName().get());
        //let var : type = value
        else if(value.isPresent() && ast.getTypeName().isPresent())
        {
            visit(ast.getValue().get());
            if(!ast.getTypeName().get().equals(ast.getValue().get().getType().getName()))
                throw new RuntimeException("Returned value is not same as declared type");
            type = getType(ast.getTypeName().get());
        }
        //let var
        else
            throw new RuntimeException("Missing type and assignment");
        Environment.Variable var = new Environment.Variable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        ast.setVariable(var);
        scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        return null;

    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if(!(ast.getReceiver() instanceof Ast.Expression.Access))
            throw new RuntimeException("Receiver is not access");
        //initialize the receiver and value types
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if(ast.getThenStatements().isEmpty())
            throw new RuntimeException("Then statement is empty for if");
        List<Ast.Statement> thenStatements = ast.getThenStatements();
        List<Ast.Statement> elseStatements = ast.getElseStatements();
        Scope curr = scope;
        scope = new Scope(curr);
        for(Ast.Statement statements : thenStatements)
            visit(statements);
        scope = curr;
        if(!elseStatements.isEmpty())
        {
            scope = new Scope(curr);
            for(Ast.Statement statements : elseStatements)
                visit(statements);
        }
        scope = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        Ast.Expression condition  = ast.getCondition();
        visit(condition);
        List<Ast.Statement.Case> cases = ast.getCases();
        int i = 0;
        for(; i < cases.size() -1 ;i++)
        {
            if(cases.get(i).getValue().isPresent())
            {
                //check the type
                visit(cases.get(i).getValue().get());
                if(!cases.get(i).getValue().get().getType().equals(condition.getType()))
                    throw new RuntimeException("Case type is not same as switch statement type");
                //if type correct, execute the case
                visit(cases.get(i));
            }
            else
                throw new RuntimeException("Illegal Default case not at end of Switch");
        }
        //If last cases has value, error
        if(cases.get(i).getValue().isPresent())
            throw new RuntimeException("Illegal switch without default");
        else visit(cases.get(i));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        List<Ast.Statement> statements = ast.getStatements();
        for(Ast.Statement statement : statements)
            visit(statement);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        scope = new Scope(scope);
        for(Ast.Statement statements : ast.getStatements())
            visit(statements);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //saved return type in variable to be used in function.
        visit(ast.getValue());
        return null;
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
        Environment.Variable var = scope.lookupVariable(ast.getName());
        Optional optional = ast.getOffset();
        if (optional.isPresent()) {
            visit(ast.getOffset().get());
            //require offset isnt an int, throw error
            //requireAssignable(Environment.Type.INTEGER, optional.get().)
            requireAssignable(Environment.Type.INTEGER, ast.getOffset().get().getType());
        }
        ast.setVariable(var);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function fun = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        //List<Ast.Expression> arguments = ast.getArguments();
        //check every argument type matches parameter
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            requireAssignable(fun.getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }
        ast.setFunction(fun);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> vals = ast.getValues();
        //ast.setType();
        for (Ast.Expression val : vals) {
            visit(val);
            requireAssignable(ast.getType(), val.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(Environment.Type.ANY))
        {}
        else if(target.equals(Environment.Type.COMPARABLE))
        {
            if(!(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING) || type.equals(Environment.Type.COMPARABLE)))
                throw new RuntimeException("Illegal comparison of comparable");
        }
        else if(!target.equals(type))
            throw new RuntimeException("Types do not match. Target is of type: " + target.getName() + ". RHS is of type: " + type.getName());
    }

}
