// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.component.chain.model;

import com.yahoo.component.ComponentSpecification;

/**
 * Maps component specifications to matching instances.
 *
 * @author tonytv
 */
public interface Resolver<T> {
    T resolve(ComponentSpecification componentSpecification);
}
