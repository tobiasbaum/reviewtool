package de.setsoftware.reviewtool.ordering;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

/**
 * Key for a method, consisting of its name and its number of arguments.
 */
class MethodKey {

    private final String name;
    private final int argCount;

    public MethodKey(MethodDeclaration m) {
        this.name = m.getNameAsString();
        this.argCount = m.getParameters().size();
    }

    public MethodKey(MethodCallExpr c) {
        this.name = c.getNameAsString();
        this.argCount = c.getArguments().size();
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.argCount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MethodKey)) {
            return false;
        }
        final MethodKey k = (MethodKey) o;
        return this.name.equals(k.name)
            && this.argCount == k.argCount;
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder(this.name);
        ret.append('(');
        if (this.argCount > 0) {
            ret.append('_');
        }
        for (int i = 1; i < this.argCount; i++) {
            ret.append(",_");
        }
        ret.append(')');
        return ret.toString();
    }

}
