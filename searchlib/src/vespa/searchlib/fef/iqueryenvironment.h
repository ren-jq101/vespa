// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "iindexenvironment.h"
#include <vespa/searchcommon/attribute/iattributecontext.h>
#include <vespa/searchlib/fef/objectstore.h>

namespace search {
namespace fef {

class Location;
class Properties;
class ITermData;

/**
 * Abstract view of query related information available to the
 * framework.
 **/
class IQueryEnvironment
{
public:
    /**
     * Convenience typedef.
     **/
    typedef std::shared_ptr<IQueryEnvironment> SP;

    /**
     * Obtain the set of properties associated with this query
     * environment. This set of properties is known through the system
     * as 'rankProperties', and is tagged with the name 'rank' when
     * propagated down through the system.
     *
     * @return properties
     **/
    virtual const Properties &getProperties() const = 0;

    /**
     * Obtain the number of ranked terms in the query. The order of the
     * terms are not yet strongly defined.
     *
     * @return number of ranked terms in the query
     **/
    virtual uint32_t getNumTerms() const = 0;

    /**
     * Obtain information about a single ranked term in the query. If
     * idx is out of bounds, 0 will be returned.
     *
     * TODO: this must return an ordering that corresponds to the connexity of the term data.
     * TODO: any other ordering seems inappropriate when we offer connexity as an attribute of
     * TODO: the term data.
     *
     * @return information about a ranked term
     * @param idx the term we want information about
     **/
    virtual const ITermData *getTerm(uint32_t idx) const = 0;

    /**
     * Obtain the location information associated with this query environment.
     *
     * @return location object.
     **/
    virtual const Location & getLocation() const = 0;

    /**
     * Returns the attribute context for this query.
     *
     * @return attribute context
     **/
    virtual const search::attribute::IAttributeContext & getAttributeContext() const = 0;

    /**
     * Returns a const view of the index environment.
     *
     * @return index environment
     **/
    virtual const IIndexEnvironment & getIndexEnvironment() const = 0;

    /**
     * Virtual destructor to allow safe subclassing.
     **/
    virtual ~IQueryEnvironment() { }

    IObjectStore & getObjectStore() { return _objectStore; }
    const IObjectStore & getObjectStore() const { return _objectStore; }
protected:
    IQueryEnvironment() { }
private:
    ObjectStore _objectStore;
};

} // namespace fef
} // namespace search
