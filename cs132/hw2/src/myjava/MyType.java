package myjava;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MyType {
    public static MyType TRUE = new MyType();
    public static MyType ARRAY = new MyType("Array");
    public static MyType BOOLEAN = new MyType("Boolean");
    public static MyType INTEGER = new MyType("Integer");
    public String name;
    public MyType parent;
    public boolean found;
    public HashMap<String, MyType> vars;
    public HashMap<String, Method> methods;

    private MyType() {
    }

    public MyType(String name) {
        this.name = name;
        found = true;
        vars = new HashMap<String, MyType>();
        methods = new HashMap<String, Method>();
    }

    public Method addMethod(String method, MyType returnType) {
        if (methods.containsKey(method))
            return null;
        Method ret = new Method(method, returnType);
        methods.put(method, ret);
        return ret;
    }

    Method getMethod(String method) {
        MyType type = this;
        while (type != null) {
            Method ret = type.methods.get(method);
            if (ret != null)
                return ret;
            type = type.parent;
        }
        return null;
    }

    public MyType getMethodType(String method) {
        Method ret = getMethod(method);
        if (ret == null)
            return null;
        return ret.returnType;
    }

    public Collection<MyType> getMethodArgs(String method) {
        Method ret = getMethod(method);
        if (ret == null)
            return null;
        return ret.args.values();
    }

    public class Method {
        final String name;
        final MyType returnType;
        final LinkedHashMap<String, MyType> args;
        final LinkedHashMap<String, MyType> vars;

        Method(String name, MyType returnType) {
            this.name = name;
            this.returnType = returnType;
            args = new LinkedHashMap<String, MyType>();
            vars = new LinkedHashMap<String, MyType>();
        }

        public boolean addArg(String arg, MyType type) {
            if (args.containsKey(arg))
                return false;
            args.put(arg, type);
            return true;
        }

        public boolean addVar(String var, MyType type) {
            if (vars.containsKey(var))
                return false;
            vars.put(var, type);
            return true;
        }

        public MyType getVarType(String var) {
            MyType ret = vars.get(var);
            if (ret != null)
                return ret;
            return args.get(var);
        }
    }
}
