package myjava;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

public class MySymbolTable extends GJNoArguDepthFirst<Boolean> {
    final HashMap<String, MyType> classTable;
    MyType classScope = null;
    MyType.Method methodScope = null;

    public MySymbolTable() {
        classTable = new HashMap<String, MyType>();
        classTable.put(MyType.ARRAY.name, MyType.ARRAY);
        classTable.put(MyType.BOOLEAN.name, MyType.ARRAY);
        classTable.put(MyType.INTEGER.name, MyType.INTEGER);
    }

    public void setClassScope(String classScope) {
        this.classScope = classTable.get(classScope);
    }

    public void clearClassScope() {
        this.classScope = null;
    }

    public MyType getVarType(String var) {
        MyType ret;
        MyType scope = classScope;
        while (scope != null) {
            ret = scope.methods.get(methodScope.name).getVarType(var);
            if (ret != null)
                return ret;

            ret = scope.vars.get(var);
            if (ret != null)
                return ret;

            scope = scope.parent;
        }
        return null;
    }

    public boolean isSubclass(MyType child, MyType parent) {
        while (child != parent) {
            child = child.parent;
            if (child == null)
                return false;
        }
        return true;
    }

    MyType addClass(String typename) {
        MyType type = classTable.get(typename);
        if (type == null) {
            type = new MyType(typename);
            classTable.put(typename, type);
        } else if (!type.found)
            type.found = true;
        else
            return null;
        return type;
    }

    MyType addClass(String typename, String parent) {
        MyType type = addClass(typename);
        if (type == null)
            return null;

        type.parent = classTable.get(parent);
        if (type.parent == null) {
            type.parent = new MyType(parent);
            type.parent.found = false;
            classTable.put(parent, type.parent);
        }
        return type;
    }

    boolean addMethod(String method, MyType returnType) {
        return classScope.addMethod(method, returnType) != null;
    }

    boolean addVar(String var, MyType varType) {
        if (methodScope != null)
            return methodScope.addVar(var, varType);
        if (classScope.vars.containsKey(var))
            return false;
        classScope.vars.put(var, varType);
        return true;
    }

    boolean addArg(String arg, MyType type) {
        return methodScope.addArg(arg, type);
    }

    boolean hasOverload() {
        for (MyType child : classTable.values()) {
            MyType parent = child.parent;
            while (parent != null) {
                for (MyType.Method childMethod : child.methods.values()) {
                    MyType.Method parentMethod = parent.methods.get(childMethod.name);
                    if (parentMethod != null) {
                        Iterator<MyType> parentArgs = parentMethod.args.values().iterator();
                        for (MyType childArg : childMethod.args.values()) {
                            if (!parentArgs.hasNext() || childArg != parentArgs.next())
                                return true;
                        }
                        if (parentArgs.hasNext())
                            return true;
                        if (parent.methods.get(childMethod.name).returnType != childMethod.returnType)
                            return true;
                    }
                }
                parent = parent.parent;
            }
        }
        return false;
    }

    @Override
    public Boolean visit(Goal n) {
        if (n.f0.accept(this) == null)
            return null;
        if (n.f1.accept(this) == null)
            return null;
        if (hasOverload())
            return null;
        for (MyType type : classTable.values())
            if (!type.found)
                return null;
        return true;
    }

    MyType type(Type type) {
        switch (type.f0.which) {
            case 0:
                return MyType.ARRAY;
            case 1:
                return MyType.BOOLEAN;
            case 2:
                return MyType.INTEGER;
            case 3:
                String typename = ((Identifier) type.f0.choice).f0.tokenImage;
                MyType ret = classTable.get(typename);
                if (ret == null) {
                    ret = new MyType(typename);
                    ret.found = false;
                    classTable.put(typename, ret);
                }
                return ret;
            default:
                return null;
        }
    }

    @Override
    public Boolean visit(MainClass n) {
        classScope = addClass(n.f1.f0.tokenImage);
        methodScope = classScope.addMethod("main", MyType.TRUE);
        methodScope.addArg(n.f11.f0.tokenImage, MyType.TRUE);
        Boolean ret = n.f14.accept(this);
        methodScope = null;
        clearClassScope();
        return ret;
    }

    @Override
    public Boolean visit(TypeDeclaration n) {
        return n.f0.accept(this);
    }

    @Override
    public Boolean visit(ClassDeclaration n) {
        MyType type = addClass(n.f1.f0.tokenImage);
        if (type == null)
            return null;

        classScope = type;
        if (n.f3.accept(this) == null)
            return null;

        Boolean ret = n.f4.accept(this);
        classScope = null;
        return ret;
    }

    @Override
    public Boolean visit(ClassExtendsDeclaration n) {
        MyType type = addClass(n.f1.f0.tokenImage, n.f3.f0.tokenImage);
        if (type == null)
            return null;

        classScope = type;
        if (n.f5.accept(this) == null)
            return null;

        Boolean ret = n.f6.accept(this);
        return ret;
    }

    @Override
    public Boolean visit(VarDeclaration n) {
        if (addVar(n.f1.f0.tokenImage, type(n.f0)))
            return true;
        return null;
    }

    @Override
    public Boolean visit(MethodDeclaration n) {
        if (!addMethod(n.f2.f0.tokenImage, type(n.f1)))
            return null;
        methodScope = classScope.getMethod(n.f2.f0.tokenImage);
        if (n.f4.accept(this) == null)
            return null;
        Boolean ret = n.f7.accept(this);
        methodScope = null;
        return ret;
    }

    @Override
    public Boolean visit(FormalParameterList n) {
        if (n.f0.accept(this) == null)
            return null;
        return n.f1.accept(this);
    }

    @Override
    public Boolean visit(FormalParameter n) {
        if (addArg(n.f1.f0.tokenImage, type(n.f0)))
            return true;
        return null;
    }

    @Override
    public Boolean visit(FormalParameterRest n) {
        return n.f1.accept(this);
    }

    @Override
    public Boolean visit(NodeList n) {
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            if (e.nextElement().accept(this) == null)
                return null;
        }
        return true;
    }

    @Override
    public Boolean visit(NodeListOptional n) {
        if (n.present()) {
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
                if (e.nextElement().accept(this) == null)
                    return null;
            }
        }
        return true;
    }

    @Override
    public Boolean visit(NodeOptional n) {
        if (n.present())
            return n.node.accept(this);
        return true;
    }
}
