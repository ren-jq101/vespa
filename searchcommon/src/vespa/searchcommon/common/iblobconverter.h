// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/util/buffer.h>
#include <memory>

namespace search {
namespace common {

class BlobConverter
{
public:
    using SP = std::shared_ptr<BlobConverter>;
    using UP = std::unique_ptr<BlobConverter>;
    using ConstBufferRef = vespalib::ConstBufferRef;
    virtual ~BlobConverter() { }
    ConstBufferRef convert(const ConstBufferRef & src) const { return onConvert(src); }
private:
    virtual ConstBufferRef onConvert(const ConstBufferRef & src) const = 0;
};

}
}

