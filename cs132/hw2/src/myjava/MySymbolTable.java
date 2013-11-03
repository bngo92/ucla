package myjava;

import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.*;

public class MySymbolTable extends GJDepthFirst<Boolean, MyType> {
    HashMap<String, MyType> typeTable;
    MyType type = null;
    String method = null;

    public MySymbolTable() {
        typeTable = new HashMap<String, MyType>();
        typeTable.put(MyType.ARRAY.type, MyType.ARRAY);
        typeTable.put(MyType.BOOLEAN.type, MyType.ARRAY);
        typeTable.put(MyType.INTEGER.type, MyType.INTEGER);
    }

    public MyType isVar(String var) {
        MyType ret;
        MyType node = type;
        while (node != null) {
            ret = node.methodVars.get(method).get(var);
            if (ret != null)
                return ret;
            ret = node.methodArgs.get(method).get(var);
            if (ret != null)
                return ret;
            ret = node.vars.get(var);
            if (ret != null)
                return ret;
            node = node.parent;
        }
        return null;
    }

    public void setType(String type) {
        this.type = typeTable.get(type);
    }

    public void clearType() {
        this.type = null;
    }

    public MyType getMethodType(MyType type, String method) {
        MyType ret;
        MyType node = type;
        while (node != null) {
            ret = node.methodTypes.get(method);
            if (ret != null)
                return ret;
            node = node.parent;
        }
        return null;
    }

    public Collection<MyType> getMethodArgs(MyType type, String method) {
        LinkedHashMap<String, MyType> ret;
        MyType node = type;
        while (node != null) {
            ret = node.methodArgs.get(method);
            if (ret != null)
                return ret.values();
            node = node.parent;
        }
        return null;
    }

    public boolean isSubclass(MyType child, MyType parent){
        MyType node = child;
        while (node != parent) {
            node = node.parent;
            if (node == null)
                return false;
        }
        return true;
    }

    public boolean hasOverload() {
        for (MyType type : typeTable.values()) {
            MyType node = type.parent;
            while (node != null) {
                for (String method : type.methodArgs.keySet()) {
                    if (!node.methodArgs.containsKey(method))
                        continue;
                    Iterator<MyType> iterator = node.methodArgs.get(method).values().iterator();
                    for (MyType arg : type.methodArgs.get(method).values()) {
                        if (!iterator.hasNext() || arg != iterator.next())
                            return true;
                    }
                    if (iterator.hasNext())
                        return true;
                }
                node = node.parent;
            }
        }
        return false;
    }

    MyType addType(String type) {
        MyType node = typeTable.get(type);
        if (node == null) {
            node = new MyType(type);
            typeTable.put(type, node);
        } else if (!node.found)
            node.found = true;
        else
            return null;
        return node;
    }

    MyType addType(String child, String parent) {
        MyType node = typeTable.get(child);
        if (node == null) {
            node = new MyType(child);
            typeTable.put(child, node);
        } else if (!node.found)
            node.found = true;
        else
            return null;
        node.parent = typeTable.get(parent);
        if (node.parent == null) {
            node.parent = new MyType(parent);
            node.parent.found = false;
        }
        typeTable.put(child, node);
        return node;
    }

    boolean addMethod(MyType type, String method, MyType methodType) {
        if (type.methodTypes.containsKey(method))
            return false;
        type.methodTypes.put(method, methodType);
        type.methodArgs.put(method, new LinkedHashMap<String, MyType>());
        type.methodVars.put(method, new LinkedHashMap<String, MyType>());
        return true;
    }

    boolean addVar(String var, MyType classType, MyType type) {
        if (method == null) {
            if (classType.vars.containsKey(var))
                return false;
            classType.vars.put(var, type);
        } else {
            if (classType.methodVars.containsKey(var))
                return false;
            classType.methodVars.get(method).put(var, type);
        }
        return true;
    }

    boolean addArg(String var, MyType classType, MyType type) {
        LinkedHashMap<String, MyType> methodArgs = classType.methodArgs.get(method);
        if (methodArgs.containsKey(var))
            return false;
        methodArgs.put(var, type);
        return true;
    }

    @Override
    public Boolean visit(Goal n, MyType argu) {
        if (n.f0.accept(this, argu) == null)
            return null;
        if (n.f1.accept(this, argu) == null)
            return null;
        if (hasOverload())
            return null;
        for (MyType type : typeTable.values())
            if (!type.found)
                return null;
        return true;
    }

    @Override
    public Boolean visit(MainClass n, MyType argu) {
        this.type = addType(n.f1.f0.tokenImage);
        method = "main";
        addMethod(this.type, method, MyType.TRUE);
        Boolean ret = n.f14.accept(this, type);
        method = null;
        clearType();
        return ret;
    }

    @Override
    public Boolean visit(TypeDeclaration n, MyType argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public Boolean visit(ClassDeclaration n, MyType argu) {
        MyType type = addType(n.f1.f0.tokenImage);
        if (type == null)
            return null;

        // var declaration
        if (n.f3.accept(this, type) == null)
            return null;

        return n.f4.accept(this, type);
    }

    @Override
    public Boolean visit(ClassExtendsDeclaration n, MyType argu) {
        MyType type = addType(n.f1.f0.tokenImage, n.f3.f0.tokenImage);
        if (type == null)
            return null;

        // var declaration
        if (n.f5.accept(this, type) == null)
            return null;

        return n.f6.accept(this, type);
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
                MyType ret = typeTable.get(typename);
                if (ret == null) {
                    ret = new MyType(typename);
                    ret.found = false;
                    typeTable.put(typename, ret);
                }
                return ret;
            default:
                return null;
        }
    }

    @Override
    public Boolean visit(VarDeclaration n, MyType argu) {
        if (!addVar(n.f1.f0.tokenImage, argu, type(n.f0)))
            return null;
        return true;
    }

    @Override
    public Boolean visit(MethodDeclaration n, MyType argu) {
        if (!addMethod(argu, n.f2.f0.tokenImage, type(n.f1)))
            return null;
        method = n.f2.f0.tokenImage;
        if (n.f4.accept(this, argu) == null)
            return null;
        Boolean ret = n.f7.accept(this, argu);
        method = null;
        return ret;
    }

    @Override
    public Boolean visit(FormalParameterList n, MyType argu) {
        if (n.f0.accept(this, argu) == null)
            return null;
        return n.f1.accept(this, argu);
    }

    @Override
    public Boolean visit(FormalParameter n, MyType argu) {
        if (!addArg(n.f1.f0.tokenImage, argu, type(n.f0)))
            return null;
        return true;
    }

    @Override
    public Boolean visit(FormalParameterRest n, MyType argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public Boolean visit (NodeList n, MyType argu) {
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
            if (e.nextElement().accept(this, argu) == null)
                return null;
        }
        return true;
    }

    @Override
    public Boolean visit (NodeListOptional n, MyType argu) {
        if (n.present()) {
            for ( Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
                if (e.nextElement().accept(this, argu) == null)
                    return null;
            }
        }
        return true;
    }

    @Override
    public Boolean visit(NodeOptional n, MyType argu) {
        if (n.present())
            return n.node.accept(this, argu);
        return true;
    }
}
