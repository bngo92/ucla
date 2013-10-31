package picojava;

import java.util.*;
import syntaxtree.*;
import visitor.*;

/**
 * Example of a mini class to store type checking expressions. Stores
 * as array, as multiple entries are needed (eg, for sequence of
 * statements).
 *
 */
public class MyType {
    public Vector<String> type_array;

    MyType(String s) {
	type_array = new Vector<String>();
	type_array.addElement(s);
    }

    MyType() {
	type_array = new Vector<String>();
    }

    Boolean checkIdentical(MyType o) {
	if (type_array.size() != o.type_array.size())
	    return false;
	int i;
	for (i = 0; i < type_array.size(); ++i)
	    if (type_array.elementAt(i).compareTo(o.type_array.elementAt(i)) > 0)
		return false;
	return true;
    }
}
