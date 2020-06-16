// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "dense_simple_expand_function.h"
#include "dense_tensor_view.h"
#include <vespa/vespalib/objects/objectvisitor.h>
#include <vespa/eval/eval/value.h>
#include <vespa/eval/eval/operation.h>
#include <vespa/eval/eval/inline_operation.h>
#include <vespa/vespalib/util/typify.h>
#include <optional>
#include <algorithm>

namespace vespalib::tensor {

using vespalib::ArrayRef;

using eval::Value;
using eval::ValueType;
using eval::TensorFunction;
using eval::TensorEngine;
using eval::TypifyCellType;
using eval::as;

using namespace eval::operation;
using namespace eval::tensor_function;

using Inner = DenseSimpleExpandFunction::Inner;

using op_function = eval::InterpretedFunction::op_function;
using Instruction = eval::InterpretedFunction::Instruction;
using State = eval::InterpretedFunction::State;

namespace {

struct ExpandParams {
    const ValueType &result_type;
    size_t result_size;
    join_fun_t function;
    ExpandParams(const ValueType &result_type_in, size_t result_size_in, join_fun_t function_in)
        : result_type(result_type_in), result_size(result_size_in), function(function_in) {}
};

template <typename LCT, typename RCT, typename Fun, bool rhs_inner>
void my_simple_expand_op(State &state, uint64_t param) {
    using ICT = typename std::conditional<rhs_inner,RCT,LCT>::type;
    using OCT = typename std::conditional<rhs_inner,LCT,RCT>::type;
    using DCT = typename eval::UnifyCellTypes<ICT,OCT>::type;
    using OP = typename std::conditional<rhs_inner,SwapArgs2<Fun>,Fun>::type;
    const ExpandParams &params = *(ExpandParams*)param;
    OP my_op(params.function);
    auto inner_cells = DenseTensorView::typify_cells<ICT>(state.peek(rhs_inner ? 0 : 1));
    auto outer_cells = DenseTensorView::typify_cells<OCT>(state.peek(rhs_inner ? 1 : 0));
    auto dst_cells = state.stash.create_array<DCT>(params.result_size);
    DCT *dst = dst_cells.begin();
    for (OCT outer_cell: outer_cells) {
        apply_op2_vec_num(dst, inner_cells.begin(), outer_cell, inner_cells.size(), my_op);
        dst += inner_cells.size();
    }
    state.pop_pop_push(state.stash.create<DenseTensorView>(params.result_type, TypedCells(dst_cells)));
}

//-----------------------------------------------------------------------------

struct MyGetFun {
    template <typename R1, typename R2, typename R3, typename R4> static auto invoke() {
        return my_simple_expand_op<R1, R2, R3, R4::value>;
    }
};

using MyTypify = TypifyValue<TypifyCellType,TypifyOp2,TypifyBool>;

//-----------------------------------------------------------------------------

std::vector<ValueType::Dimension> strip_trivial(const std::vector<ValueType::Dimension> &dim_list) {
    std::vector<ValueType::Dimension> result;
    std::copy_if(dim_list.begin(), dim_list.end(), std::back_inserter(result),
                 [](const auto &dim){ return (dim.size != 1); });
    return result;
}

std::optional<Inner> detect_simple_expand(const TensorFunction &lhs, const TensorFunction &rhs) {
    std::vector<ValueType::Dimension> a = strip_trivial(lhs.result_type().dimensions());
    std::vector<ValueType::Dimension> b = strip_trivial(rhs.result_type().dimensions());
    if (a.empty() || b.empty()) {
        return std::nullopt;
    } else if (a.back().name < b.front().name) {
        return Inner::RHS;
    } else if (b.back().name < a.front().name) {
        return Inner::LHS;
    } else {
        return std::nullopt;
    }
}

} // namespace vespalib::tensor::<unnamed>

//-----------------------------------------------------------------------------

DenseSimpleExpandFunction::DenseSimpleExpandFunction(const ValueType &result_type,
                                                     const TensorFunction &lhs,
                                                     const TensorFunction &rhs,
                                                     join_fun_t function_in,
                                                     Inner inner_in)
    : Join(result_type, lhs, rhs, function_in),
      _inner(inner_in)
{
}

DenseSimpleExpandFunction::~DenseSimpleExpandFunction() = default;

Instruction
DenseSimpleExpandFunction::compile_self(const TensorEngine &, Stash &stash) const
{
    size_t result_size = result_type().dense_subspace_size();
    const ExpandParams &params = stash.create<ExpandParams>(result_type(), result_size, function());
    auto op = typify_invoke<4,MyTypify,MyGetFun>(lhs().result_type().cell_type(),
                                                 rhs().result_type().cell_type(),
                                                 function(), (_inner == Inner::RHS));
    static_assert(sizeof(uint64_t) == sizeof(&params));
    return Instruction(op, (uint64_t)(&params));
}

const TensorFunction &
DenseSimpleExpandFunction::optimize(const TensorFunction &expr, Stash &stash)
{
    if (auto join = as<Join>(expr)) {
        const TensorFunction &lhs = join->lhs();
        const TensorFunction &rhs = join->rhs();
        if (lhs.result_type().is_dense() && rhs.result_type().is_dense()) {
            if (std::optional<Inner> inner = detect_simple_expand(lhs, rhs)) {
                assert(expr.result_type().dense_subspace_size() ==
                       (lhs.result_type().dense_subspace_size() *
                        rhs.result_type().dense_subspace_size()));
                return stash.create<DenseSimpleExpandFunction>(join->result_type(), lhs, rhs, join->function(), inner.value());
            }
        }
    }
    return expr;
}

} // namespace vespalib::tensor
