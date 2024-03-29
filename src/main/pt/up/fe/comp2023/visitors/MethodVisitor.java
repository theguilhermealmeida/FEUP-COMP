package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.handlers.IdentifierHandler;
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;

public class MethodVisitor extends AJmmVisitor<String, String> {
    private JmmNode node;
    private String modifier;
    private String name;
    private String extension;
    private String returnType;
    private boolean isStatic;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;

    public MethodVisitor(JmmNode node, MySymbolTable symbolTable, ArrayList<Report> reports, String extension) {
        this.modifier = node.get("modifier");
        this.name = node.get("name");
        this.isStatic = node.hasAttribute("isStatic");
        this.extension = extension;

        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        return splitImport[splitImport.length - 1];
    }

    private void addReport(String line, String col, String message) {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), message
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
        StatementVisitor visitor = new StatementVisitor(this.name, this.extension, this.isStatic, this.symbolTable, this.reports);

        return visitor.visit(node, "");
    }

    private String dealWithMethodStatement(JmmNode node, String s) {
        StatementVisitor visitor = new StatementVisitor(this.name, this.extension, this.isStatic, this.symbolTable, this.reports);

        return visitor.visit(node, "");
    }

    private String dealWithVarDeclaration(JmmNode node, String s) {
        String id = node.hasAttribute("var") ? node.get("var") : node.get("parameter");

        IdentifierHandler handler = new IdentifierHandler(id, this.name, this.extension, this.isStatic, this.symbolTable);

        JmmNode child = node.getJmmChild(0);

        if (child.getKind().equals("CustomType")) {
            String typename = child.get("name");

            boolean exists = false;

            if (typename.equals(this.symbolTable.getClassName())) {
                exists = true;
            }

            for (String imp : this.symbolTable.getImports()) {
                if (typename.equals(this.parseImport(imp))) {
                    exists = true;

                    break;
                }
            }

            if (!exists) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "Unknown type in variable declaration! (method: dealWithVarDeclaration)");

                return null;
            }
        }

        if (handler.getType() == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The handler type is NULL! (method: dealWithVarDeclaration)");
        }

        return null;
    }

    private String dealWithReturnType(JmmNode node, String s) {
        JmmNode returnTypeNode = node.getJmmChild(0);

        if (returnTypeNode.getKind().equals("Literal")) {
            this.returnType = returnTypeNode.get("keyword");
        }

        if (returnTypeNode.getKind().equals("CustomType")) {
            this.returnType = returnTypeNode.get("name");
        }

        return this.returnType;
    }
}
