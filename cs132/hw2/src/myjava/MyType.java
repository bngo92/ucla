package myjava;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class MyType {
    public static MyType TRUE = new MyType();
    public static MyType ARRAY = new MyType("Array");
    public static MyType BOOLEAN = new MyType("Boolean");
    public static MyType INTEGER = new MyType("Integer");
    public String type;
    public MyType parent;
    public boolean found;
    public HashMap<String, MyType> vars;
    public HashMap<String, MyType> methodTypes;
    public HashMap<String, LinkedHashMap<String, MyType>> methodArgs;
    public HashMap<String, LinkedHashMap<String, MyType>> methodVars;
    MyType() {}
    public MyType(String type) {
        this.type = type;
        found = true;
        vars = new HashMap<String, MyType>();
        methodTypes = new HashMap<String, MyType>();
        methodArgs = new HashMap<String, LinkedHashMap<String, MyType>>();
        methodVars = new HashMap<String, LinkedHashMap<String, MyType>>();
    }
}
