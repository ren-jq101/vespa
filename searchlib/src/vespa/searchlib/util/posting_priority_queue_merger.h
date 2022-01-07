// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "posting_priority_queue.h"

namespace search {

/*
 * Provide priority queue semantics for a set of posting readers with
 * merging to a posting writer.
 */
template <class Reader, class Writer>
class PostingPriorityQueueMerger : public PostingPriorityQueue<Reader>
{
public:
    using Parent = PostingPriorityQueue<Reader>;
    using Vector = typename Parent::Vector;
    using Parent::_vec;
    using Parent::adjust;
    using Parent::empty;
    using Parent::lowest;
    using Parent::sort;

    void mergeHeap(Writer& writer, const IFlushToken& flush_token) __attribute__((noinline));
    static void mergeOne(Writer& writer, Reader& reader, const IFlushToken &flush_token) __attribute__((noinline));
    static void mergeTwo(Writer& writer, Reader& reader1, Reader& reader2, const IFlushToken& flush_token) __attribute__((noinline));
    static void mergeSmall(Writer& writer, typename Vector::iterator ib, typename Vector::iterator ie, const IFlushToken &flush_token) __attribute__((noinline));
    void merge(Writer& writer, uint32_t heapLimit, const IFlushToken& flush_token) __attribute__((noinline));
};

}
