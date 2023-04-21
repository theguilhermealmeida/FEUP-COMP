package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.handlers.VariableHandler;

import java.util.ArrayList;

public class MethodVisitor extends AJmmVisitor<String, String> {
    private JmmNode node;
    private String modifier;
    private String name;
    private String extension;
    private String returnType;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;

    public MethodVisitor(JmmNode node, MySymbolTable symbolTable, ArrayList<Report> reports, String extension) {
        this.modifier = node.get("modifier");
        this.name = node.get("name");
        this.extension = extension;

        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    private void addReport() {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, -1, -1, ""
        ));

    }

    @Override
    protected void buildVisitor() {
        addVisit("Method", this::dealWithMethod);
        addVisit("ReturnType", this::dealWithReturnType);
        addVisit("Argument", this::dealWithVarDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("MethodStatement", this::dealWithMethodStatement);
        addVisit("ReturnStatement", this::dealWithReturnStatement);
    }

    private String dealWithMethod(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithReturnStatement(JmmNode node, String s) {
        StatementVisitor visitor = new StatementVisitor(this.name, this.extension, this.symbolTable, this.reports);

        return visitor.visit(node, "");
    }

    private String dealWithMethodStatement(JmmNode node, String s) {
        StatementVisitor visitor = new StatementVisitor(this.name, this.extension, this.symbolTable, this.reports);

        return visitor.visit(node, "");
    }

    private String dealWithVarDeclaration(JmmNode node, String s) {
        VariableHandler handler = new VariableHandler(node, this.symbolTable);

        if (handler.getType() == null) {
            System.out.println("I've found a null handler!");

            this.addReport();
        }

        return null;
    }

    private String dealWithReturnType(JmmNode node, String s) {
        JmmNode returnTypeNode = node.getJmmChild(0);

        this.returnType = returnTypeNode.get("keyword");

        return this.returnType;
    }
}
