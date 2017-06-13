// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once


#include <vespa/config/subscription/sourcespec.h>
#include <vespa/config/common/timingvalues.h>
#include "ifetchercallback.h"

namespace config {

class IConfigContext;

/**
 * A config fetcher subscribes to a config and notifies a callback when done
 */
template <typename ConfigType>
class ConfigGetter
{
public:
    static std::unique_ptr<ConfigType> getConfig(int64_t &generation, const std::string & configId, const SourceSpec & spec = ServerSpec());
    static std::unique_ptr<ConfigType> getConfig(int64_t &generation, const std::string & configId, const std::shared_ptr<IConfigContext> & context, uint64_t subscribeTimeout = DEFAULT_SUBSCRIBE_TIMEOUT);
    static std::unique_ptr<ConfigType> getConfig(const std::string & configId, const SourceSpec & spec = ServerSpec());
    static std::unique_ptr<ConfigType> getConfig(const std::string & configId, const std::shared_ptr<IConfigContext> & context, uint64_t subscribeTimeout = DEFAULT_SUBSCRIBE_TIMEOUT);
};

} // namespace config


