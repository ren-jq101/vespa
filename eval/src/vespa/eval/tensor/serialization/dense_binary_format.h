// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <memory>
namespace vespalib {

class nbostream;

namespace tensor {

class DenseTensor;

/**
 * Class for serializing a dense tensor.
 */
class DenseBinaryFormat
{
public:
    static void serialize(nbostream &stream, const DenseTensor &tensor);
    static std::unique_ptr<DenseTensor> deserialize(nbostream &stream);
};

} // namespace vespalib::tensor
} // namespace vespalib
