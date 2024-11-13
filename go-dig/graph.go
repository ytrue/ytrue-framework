package dig

import "github.com/ytrue/dig/internal/graph"

// graphNode 是依赖图中的单个节点。每个节点封装了一个值。
// 该节点对应着容器中某个具体的依赖项。
type graphNode struct {
	// 节点中封装的实际对象
	Wrapped interface{}
}

// graphHolder 是容器的依赖图，保存构造函数节点和按参数分组的节点。
// 它实现了 internal/graph 中定义的 graph 接口。
// 它与表示图的 Scope 对应。
type graphHolder struct {
	// 图中所有节点的列表。
	nodes []*graphNode

	// 当前图所代表的 Scope。
	s *Scope

	// 图的快照，表示上一次捕获的节点数。
	// -1 表示尚未捕获任何快照。
	snap int
}

// 确保 graphHolder 实现了 graph.Graph 接口
var _ graph.Graph = (*graphHolder)(nil)

// 创建一个新的 graphHolder 实例，传入 Scope 作为参数。
func newGraphHolder(s *Scope) *graphHolder {
	return &graphHolder{s: s, snap: -1}
}

// Order 返回图中节点的数量，即图的规模。
func (gh *graphHolder) Order() int {
	return len(gh.nodes)
}

// EdgesFrom 返回节点 u 的依赖项的索引。
// 对于构造函数节点，返回构造函数参数的索引。
// 对于分组值节点，返回该分组中的所有提供者的索引。
func (gh *graphHolder) EdgesFrom(u int) []int {
	var orders []int
	//// 查找节点 u
	//switch w := gh.Lookup(u).(type) {
	//case *constructorNode: // 如果是构造函数节点
	//	for _, param := range w.paramList.Params { // 获取构造函数的参数列表
	//		// 获取每个参数的依赖关系
	//		orders = append(orders, getParamOrder(gh, param)...)
	//	}
	//case *paramGroupedSlice: // 如果是按参数分组的值节点
	//	// 获取该分组的所有提供者
	//	providers := gh.s.getAllGroupProviders(w.Group, w.Type.Elem())
	//	for _, provider := range providers {
	//		// 获取每个提供者的顺序并添加到依赖项列表中
	//		orders = append(orders, provider.Order(gh.s))
	//	}
	//}
	return orders
}

// NewNode 向图中添加一个新节点，并返回该节点的顺序（即其在节点列表中的位置）。
func (gh *graphHolder) NewNode(wrapped interface{}) int {
	// 获取新节点的顺序（即当前节点列表的长度）
	order := len(gh.nodes)
	// 将新节点添加到节点列表中
	gh.nodes = append(gh.nodes, &graphNode{
		Wrapped: wrapped, // 将传入的值封装为节点
	})
	return order
}

// Lookup 根据节点的顺序索引返回该节点的实际值。
// 如果索引无效，会导致 panic。
func (gh *graphHolder) Lookup(i int) interface{} {
	// 返回对应索引的节点的值
	return gh.nodes[i].Wrapped
}

// Snapshot 捕获当前图的快照，用于之后的回滚操作。
// 只允许捕获一个快照，多次调用会覆盖之前的快照。
func (gh *graphHolder) Snapshot() {
	// 保存当前图的节点数作为快照
	gh.snap = len(gh.nodes)
}

// Rollback 回滚到之前捕获的快照状态。
// 如果没有捕获快照，则此操作为无效。
func (gh *graphHolder) Rollback() {
	if gh.snap < 0 {
		// 如果没有捕获快照，直接返回
		return
	}

	// 图的节点是一个只追加的列表。为了回滚，只需要丢弃多余的节点。
	gh.nodes = gh.nodes[:gh.snap]
	// 重置快照的标记
	gh.snap = -1
}
