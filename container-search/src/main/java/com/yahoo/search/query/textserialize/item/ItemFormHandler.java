// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.query.textserialize.item;

import com.yahoo.search.query.textserialize.parser.DispatchFormHandler;

import java.util.List;

/**
 * @author tonytv
 */
public class ItemFormHandler implements DispatchFormHandler{
    @Override
    public Object dispatch(String name, List<Object> arguments, Object dispatchContext) {
        ItemFormConverter executor = ItemExecutorRegistry.getByName(name);
        return executor.formToItem(name, new ItemArguments(arguments), (ItemContext)dispatchContext);
    }
}
