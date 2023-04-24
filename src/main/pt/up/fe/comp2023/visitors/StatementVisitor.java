package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.handlers.IdentifierHandler;
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;

public class StatementVisitor extends AJmmVisitor<String, String> {
    private String method;
    private String extension;
    private boolean isStatic;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;
    public StatementVisitor(String method, String extension, boolean isStatic, MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.method = method;
        this.extension = extension;
        this.isStatic = isStatic;
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    private void addReport(String line, String col, String message) {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), message
        ));
    }

    @Override
    protected void buildVisitor() {
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("Conditional", this::dealWithConditional);
        addVisit("MethodStatement", this::dealWithMethodStatement);
        addVisit("ReturnStatement", this::dealWithReturnStatement);
        addVisit("IfStatement", this::dealWithConditionalStatement);
        addVisit("ElseStatement", this::dealWithConditionalStatement);
        addVisit("While", this::dealWithWhile);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    private String dealWithReturnStatement(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType returnType = visitor.visit(node.getJmmChild(0), "");
        Type stReturnType = this.symbolTable.getReturnType(this.method);

        if (returnType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is null! (returnStatement)");

            return null;
        }

        if (returnType.isImport() || returnType.isExtension()) {
            return null;
        }

        if (returnType.isThis()) {
            if (stReturnType.getName().equals(this.symbolTable.getClassName())) {
                return null;
            }

            if (stReturnType.getName().equals(this.extension)) {
                return null;
            }

            this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is this but it shouldn't! (returnStatement)");
        }

        if (stReturnType.getName().equals(this.symbolTable.getClassName())) {
            if (!returnType.isThis()) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is not this! (returnStatement)");;
            }

            return null;
        }

        if (!(returnType.equals(new MyType(stReturnType.getName(), "", stReturnType.isArray())))) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is not correct! (returnStatement)");;
        }

        return null;
    }

    private String dealWithMethodStatement(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithConditionalStatement(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithArrayAssignment(JmmNode node, String s) {
        String var = node.get("var");

        JmmNode accessExpr = node.getJmmChild(0);
        JmmNode expression = node.getJmmChild(1);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType accessType = visitor.visit(accessExpr);
        MyType exprType = visitor.visit(expression);

        if (accessType == null || exprType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessType or exprType is null! (arrayAssignment)");;

            return null;
        }

        if (!accessType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessType is not int! (arrayAssignment)");;

            return null;
        }

        if (!exprType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "exprType is not int! (arrayAssignment)");;

            return null;
        }

        return null;
    }

    private String dealWithAssignment(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node.get("var"), this.method, this.extension, this.isStatic, this.symbolTable);

        MyType assigneeType = handler.getType();

        if (assigneeType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "assigneeType is null! (assignment)");;

            return null;
        }

        JmmNode expression = node.getJmmChild(0);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType assignedType = visitor.visit(expression, "");

        if (assignedType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "assignedType is null! (assignment)");;

            return null;
        }

        /* the assignee is of type 'extension', assume it's correct */
        if (assigneeType.isExtension() || assignedType.isExtension()) {
            return null;
        }

        if (assigneeType.isImport()) {
            if (assignedType.isImport()) {
                return null;
            }

            if (assignedType.isThis()) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "import was assigned to this! (assignment)");;
            }

            return null;
        }

        if (assigneeType.isThis()) {
            if (!assignedType.isThis() && !assignedType.isExtension()) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "this was assigned to something other than 'this' or extension! (assignment)");;
            }

            return null;
        }

        if (assigneeType.isPrimitive()) {
            if (assignedType.isExtension() && assignedType.isMethod()) {
                return null;
            }

            if (assignedType.isImport() && assignedType.isMethod()) {
                return null;
            }
        }

        if (!assigneeType.equals(assignedType)) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "assigneeType and assignedType are of different types! (assignment)");

            return null;
        }

        return null;
    }

    private String dealWithExprStmt(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        for (JmmNode child : node.getChildren()) {
            visitor.visit(child, "");
        }

        return null;
    }

    private String dealWithWhile(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Condition")) {
                ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

                MyType cond = visitor.visit(node.getJmmChild(0));

                if (cond == null) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "cond is null! (while)");;

                    return null;
                }

                if (cond.isExtension() || cond.isImport()) {
                    return null;
                }

                if (!cond.isBoolean()) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "cond is not boolean! (while)");
                }
            }
            else {
                visit(child, "");
            }
        }

        return null;
    }

    private String dealWithConditional(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            /* condition is of type 'expression' */
            if (child.getKind().equals("Condition")) {
                ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

                visitor.visit(child);
            }
            else {
               visit(child, "");
            }
        }

        return null;
    }

    private String dealWithCodeBlock(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }
}
