// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

namespace storage {
namespace distributor {

class DelegatedStatusRequest;

class StatusDelegator
{
public:
    virtual ~StatusDelegator() {}

    virtual bool handleStatusRequest(const DelegatedStatusRequest& request) const = 0;
};

} // distributor
} // storage
