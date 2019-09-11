// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "enumstore.h"
#include "enumcomparator.h"

#include <vespa/vespalib/util/exceptions.h>
#include <vespa/vespalib/util/hdr_abort.h>
#include <vespa/vespalib/btree/btreenode.hpp>
#include <vespa/vespalib/btree/btreenodestore.hpp>
#include <vespa/vespalib/btree/btreenodeallocator.hpp>
#include <vespa/vespalib/btree/btreeiterator.hpp>
#include <vespa/vespalib/btree/btreeroot.hpp>
#include <vespa/vespalib/btree/btreebuilder.hpp>
#include <vespa/vespalib/btree/btree.hpp>
#include <vespa/vespalib/datastore/unique_store.hpp>
#include <vespa/vespalib/datastore/unique_store_string_allocator.hpp>
#include <vespa/vespalib/util/array.hpp>
#include <vespa/vespalib/util/bufferwriter.h>
#include <vespa/searchcommon/common/compaction_strategy.h>

namespace search {

template <typename EntryT>
void EnumStoreT<EntryT>::freeUnusedEnum(Index idx, IndexSet& unused)
{
    const auto& entry = get_entry_base(idx);
    if (entry.get_ref_count() == 0) {
        unused.insert(idx);
        _store.get_allocator().hold(idx);
    }
}

template <typename EntryT>
ssize_t
EnumStoreT<EntryT>::load_unique_values_internal(const void* src,
                                                size_t available,
                                                IndexVector& idx)
{
    size_t left = available;
    const char* p = static_cast<const char*>(src);
    Index idx1;
    while (left > 0) {
        ssize_t sz = load_unique_value(p, left, idx1);
        if (sz < 0) {
            return sz;
        }
        p += sz;
        left -= sz;
        idx.push_back(idx1);
    }
    return available - left;
}

template <class EntryT>
ssize_t
EnumStoreT<EntryT>::load_unique_value(const void* src, size_t available, Index& idx)
{
    if (available < sizeof(EntryType)) {
        return -1;
    }
    const auto* value = static_cast<const EntryType*>(src);
    Index prev_idx = idx;
    idx = _store.get_allocator().allocate(*value);

    if (prev_idx.valid()) {
        auto cmp = make_comparator(*value);
        assert(cmp(prev_idx, Index()));
    }
    return sizeof(EntryType);
}

template <typename EntryT>
EnumStoreT<EntryT>::EnumStoreT(bool has_postings)
    : _store(),
      _dict(),
      _cached_values_memory_usage(),
      _cached_values_address_space_usage(0, 0, (1ull << 32))
{
    _store.set_dictionary(make_enum_store_dictionary(*this, has_postings,
                                                     (has_string_type() ?
                                                      std::make_unique<FoldedComparatorType>(_store.get_data_store()) :
                                                      std::unique_ptr<datastore::EntryComparator>())));
    _dict = static_cast<IEnumStoreDictionary*>(&_store.get_dictionary());
}

template <typename EntryT>
EnumStoreT<EntryT>::~EnumStoreT() = default;

template <typename EntryT>
vespalib::AddressSpace
EnumStoreT<EntryT>::getAddressSpaceUsage() const
{
    return _store.get_address_space_usage();
}

template <typename EntryT>
void
EnumStoreT<EntryT>::transferHoldLists(generation_t generation)
{
    _store.transferHoldLists(generation);
}

template <typename EntryT>
void
EnumStoreT<EntryT>::trimHoldLists(generation_t firstUsed)
{
    // remove generations in the range [0, firstUsed>
    _store.trimHoldLists(firstUsed);
}

template <typename EntryT>
ssize_t
EnumStoreT<EntryT>::load_unique_values(const void* src, size_t available, IndexVector& idx)
{
    ssize_t sz = load_unique_values_internal(src, available, idx);
    if (sz >= 0) {
        _dict->build(idx);
    }
    return sz;
}

template <typename EntryT>
bool
EnumStoreT<EntryT>::getValue(Index idx, EntryT& value) const
{
    if (!idx.valid()) {
        return false;
    }
    value = _store.get(idx);
    return true;
}

template <typename EntryT>
EnumStoreT<EntryT>::NonEnumeratedLoader::~NonEnumeratedLoader() = default;

template <typename EntryT>
void
EnumStoreT<EntryT>::BatchUpdater::insert(EntryType value)
{
    auto cmp = _store.make_comparator(value);
    auto result = _store._dict->add(cmp, [this, &value]() -> EntryRef { return _store._store.get_allocator().allocate(value); });
    if (result.inserted()) {
        _possibly_unused.insert(result.ref());
    }
}

template <class EntryT>
void
EnumStoreT<EntryT>::write_value(BufferWriter& writer, Index idx) const
{
    writer.write(&_store.get(idx), sizeof(EntryType));
}

template <class EntryT>
bool
EnumStoreT<EntryT>::foldedChange(const Index &idx1, const Index &idx2) const
{
    auto cmp = make_folded_comparator();
    assert(!cmp(idx2, idx1));
    return cmp(idx1, idx2);
}

template <typename EntryT>
bool
EnumStoreT<EntryT>::findEnum(EntryType value, IEnumStore::EnumHandle &e) const
{
    auto cmp = make_comparator(value);
    Index idx;
    if (_dict->findFrozenIndex(cmp, idx)) {
        e = idx.ref();
        return true;
    }
    return false;
}

template <typename EntryT>
std::vector<IEnumStore::EnumHandle>
EnumStoreT<EntryT>::findFoldedEnums(EntryType value) const
{
    auto cmp = make_folded_comparator(value);
    return _dict->findMatchingEnums(cmp);
}

template <typename EntryT>
bool
EnumStoreT<EntryT>::findIndex(EntryType value, Index &idx) const
{
    auto cmp = make_comparator(value);
    return _dict->findIndex(cmp, idx);
}

template <typename EntryT>
void
EnumStoreT<EntryT>::freeUnusedEnums()
{
    auto cmp = make_comparator();
    _dict->freeUnusedEnums(cmp);
}

template <typename EntryT>
void
EnumStoreT<EntryT>::freeUnusedEnums(const IndexSet& toRemove)
{
    auto cmp = make_comparator();
    _dict->freeUnusedEnums(toRemove, cmp);
}

template <typename EntryT>
IEnumStore::Index
EnumStoreT<EntryT>::insert(EntryType value)
{
    return _store.add(value).ref();
}

template <typename EntryT>
vespalib::MemoryUsage
EnumStoreT<EntryT>::update_stat()
{
    auto &store = _store.get_allocator().get_data_store();
    _cached_values_memory_usage = store.getMemoryUsage();
    _cached_values_address_space_usage = store.getAddressSpaceUsage();
    auto retval = _cached_values_memory_usage;
    retval.merge(_dict->get_memory_usage());
    return retval;
}

namespace {

// minimum dead bytes in enum store before consider compaction
constexpr size_t DEAD_BYTES_SLACK = 0x10000u;
constexpr size_t DEAD_ADDRESS_SPACE_SLACK = 0x10000u;

}
template <typename EntryT>
std::unique_ptr<IEnumStore::EnumIndexRemapper>
EnumStoreT<EntryT>::consider_compact(const CompactionStrategy& compaction_strategy)
{
    size_t used_bytes = _cached_values_memory_usage.usedBytes();
    size_t dead_bytes = _cached_values_memory_usage.deadBytes();
    size_t used_address_space = _cached_values_address_space_usage.used();
    size_t dead_address_space = _cached_values_address_space_usage.dead();
    bool compact_memory = ((dead_bytes >= DEAD_BYTES_SLACK) &&
                           (used_bytes * compaction_strategy.getMaxDeadBytesRatio() < dead_bytes));
    bool compact_address_space = ((dead_address_space >= DEAD_ADDRESS_SPACE_SLACK) &&
                                  (used_address_space * compaction_strategy.getMaxDeadAddressSpaceRatio() < dead_address_space));
    if (compact_memory || compact_address_space) {
        return compact_worst(compact_memory, compact_address_space);
    }
    return std::unique_ptr<IEnumStore::EnumIndexRemapper>();
}

template <typename EntryT>
std::unique_ptr<IEnumStore::EnumIndexRemapper>
EnumStoreT<EntryT>::compact_worst(bool compact_memory, bool compact_address_space)
{
    return _store.compact_worst(compact_memory, compact_address_space);
}

template <typename EntryT>
std::unique_ptr<IEnumStore::Enumerator>
EnumStoreT<EntryT>::make_enumerator() const
{
    return std::make_unique<Enumerator>(*_dict, _store.get_data_store());
}

}
