package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        int curr = indent;
        indent++;
        print("public class Main {");
        newline(0);
        if(!ast.getGlobals().isEmpty()) {
            for (Ast.Global global : ast.getGlobals()) {
                newline(indent);
                print(global);
            }
            newline(0);
        }
        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent+1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");
        for(Ast.Function function : ast.getFunctions()) {
            newline(0);
            newline(indent);
            print(function);
        }
        newline(0);
        newline(0);
        print("}");
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(!ast.getMutable())
            print("final ");
        if(ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList)
            print(ast.getVariable().getType().getJvmName(), "[] ", ast.getVariable().getJvmName());
        else
            print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if(ast.getValue().isPresent())
            print(" = ", ast.getValue().get());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        int curr = indent;
        indent++;
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName() , "(");
        for(int i = 0; i < ast.getParameters().size(); i++){
            if(i == 0)
                print(getJvm(ast.getParameterTypeNames().get(i)), " ", ast.getParameters().get(i));
            else
                print(",", getJvm(ast.getParameterTypeNames().get(i)), " ", ast.getParameters().get(i));
        }
        print(") {");
        for(Ast.Statement statement : ast.getStatements()){
            newline(indent);
            print(statement);
        }
        if(!ast.getStatements().isEmpty())
            newline(curr);
        print("}");
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent())
            print(ast.getValue().get().getType().getJvmName(), " ", ast.getVariable().getJvmName() , " = ", ast.getValue().get(), ";");
        else
            print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName() , ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        int curr = indent;
        indent++;
        print("if (", ast.getCondition(), ") {");
        for(Ast.Statement statement : ast.getThenStatements())
        {
            newline(indent);
            visit(statement);
        }
        newline(curr);
        print("}");
        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            for(Ast.Statement statement : ast.getElseStatements())
            {
                newline(indent);
                visit(statement);
            }
            newline(curr);
            print("}");
        }
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        int curr = indent;
        indent++;
        print("switch (", ast.getCondition(), ") {");
        if(ast.getCases().isEmpty())
            print("}");
        else{
            newline(indent);
            for(Ast.Statement.Case Case : ast.getCases())
                visit(Case);
            newline(curr);
            print("}");
        }
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        int curr = indent;
        indent++;
        if(ast.getValue().isPresent()){
            print("case ", ast.getValue().get(), ":");
            for(Ast.Statement statement : ast.getStatements()){
                newline(indent);
                print(statement);
            }
            newline(curr);
        }
        else{
            print("default:");
            for(Ast.Statement statement : ast.getStatements()){
                newline(indent);
                print(statement);
            }
        }
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        int curr = indent;
        indent++;
        print("while (", ast.getCondition(), ") {");
        if(ast.getStatements().isEmpty())
            print("}");
        else{
            for(Ast.Statement statement : ast.getStatements()) {
                newline(indent);
                print(statement);
            }
            newline(curr);
            print("}");
        }
        indent = curr;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getType().equals(Environment.Type.STRING))
            print("\"",ast.getLiteral().toString(),"\"");
        else if(ast.getType().equals(Environment.Type.CHARACTER))
            print("'",ast.getLiteral().toString(),"'");
        else if(ast.getType().equals(Environment.Type.INTEGER))
            print(new BigInteger(ast.getLiteral().toString()));
        else if(ast.getType().equals(Environment.Type.DECIMAL))
            print(new BigDecimal(ast.getLiteral().toString()));
        else if(ast.getType().equals(Environment.Type.BOOLEAN))
            print(ast.getLiteral().toString());
        else if(ast.getType().equals(Environment.Type.NIL))
            print("null");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator().equals("^"))
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");
        else{
            print(ast.getLeft(), " " ,ast.getOperator(), " ", ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        String JvmName = ast.getVariable().getJvmName();
        if(ast.getOffset().isPresent())
            print(JvmName, "[", ast.getOffset().get(), "]");
        else
            print(JvmName);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName(), "(");
        for(int i = 0; i < ast.getArguments().size(); i++) {
            if(i == 0)
                print(ast.getArguments().get(i));
            else
                print(",", ast.getArguments().get(i));
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        for(int i = 0; i < ast.getValues().size(); i++){
            if(i == 0)
                print(ast.getValues().get(i));
            else
                print(", ", ast.getValues().get(i));
        }
        print("}");
        return null;
    }

    private String getJvm(Ast.Expression RHS){
        Environment.Type type = RHS.getType();
        return type.getJvmName();
    }

    private String getJvm(String name){
        if(name.equals("String"))
            return "String";
        if(name.equals("Any"))
            return "Object";
        if(name.equals("Nil"))
            return "Void";
        if(name.equals("Comparable"))
            return "Comparable";
        if(name.equals("Boolean"))
            return "boolean";
        if(name.equals("Integer"))
            return "int";
        if(name.equals("Decimal"))
            return "double";
        if(name.equals("Character"))
            return "char";
        throw new RuntimeException("None of the allowed types");
    }


}
