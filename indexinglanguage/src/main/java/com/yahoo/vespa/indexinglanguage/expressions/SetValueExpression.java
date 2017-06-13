// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.indexinglanguage.expressions;

import com.yahoo.document.DataType;
import com.yahoo.document.DocumentType;
import com.yahoo.document.datatypes.FieldValue;
import com.yahoo.document.datatypes.LongFieldValue;
import com.yahoo.document.datatypes.StringFieldValue;
import com.yahoo.text.StringUtilities;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class SetValueExpression extends Expression {

    private final FieldValue value;

    public SetValueExpression(FieldValue value) {
        value.getClass(); // throws NullPointerException
        this.value = value;
    }

    public FieldValue getValue() {
        return value;
    }

    @Override
    protected void doExecute(ExecutionContext ctx) {
        ctx.setValue(value);
    }

    @Override
    protected void doVerify(VerificationContext context) {
        context.setValue(value.getDataType());
    }

    @Override
    public DataType requiredInputType() {
        return null;
    }

    @Override
    public DataType createdOutputType() {
        return value.getDataType();
    }

    @Override
    public String toString() {
        if (value instanceof StringFieldValue) {
            return "\"" + StringUtilities.escape(value.toString(), '"') + "\"";
        }
        if (value instanceof LongFieldValue) {
            return value.toString() + "L";
        }
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SetValueExpression)) {
            return false;
        }
        SetValueExpression rhs = (SetValueExpression)obj;
        if (!value.equals(rhs.value)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + value.hashCode();
    }
}
