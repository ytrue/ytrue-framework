package dot

import (
	"reflect"
)

// ErrorType 是一个构造函数或组更新时的错误类型。
type ErrorType int

const (
	// noError 表示没有错误。
	noError ErrorType = iota
	// rootCause 表示根本原因错误。
	rootCause
	// transitiveFailure 表示由于缺失或错误的依赖导致的传递性失败。
	transitiveFailure
)

// CtorID 是构造函数的唯一数字标识符。
type CtorID uintptr

// Ctor 表示容器中提供给 DOT 图的构造函数信息。
// Ctor 表示一个构造函数节点，包含参数和结果，构造函数可依赖其他节点。
type Ctor struct {
	Name        string    // 构造函数的名称
	Package     string    // 构造函数所在的包
	File        string    // 构造函数所在的文件
	Line        int       // 构造函数所在的行号
	ID          CtorID    // 构造函数的唯一 ID
	Params      []*Param  // 构造函数的参数列表
	GroupParams []*Group  // 构造函数的组参数列表
	Results     []*Result // 构造函数的结果列表
	ErrorType   ErrorType // 错误类型
}

// removeParam 删除提供结果的节点依赖关系，
// 用于修剪链接到已删除构造函数的结果。
func (c *Ctor) removeParam(k nodeKey) {
	var pruned []*Param // 创建一个空的参数列表，用于存储去除依赖关系后的参数
	for _, p := range c.Params {
		if k != p.nodeKey() { // 如果当前参数的节点键不等于要删除的节点键
			pruned = append(pruned, p) // 将该参数加入到新的列表中
		}
	}
	c.Params = pruned // 将修剪后的参数列表重新赋值给构造函数的参数列表
}

// nodeKey 表示一个节点的键，包含类型、名称和组。
type nodeKey struct {
	t     reflect.Type // 类型
	name  string       // 名称
	group string       // 组名
}

// Node 表示图中的一个节点，包含参数和结果。
type Node struct {
	Type  reflect.Type // 节点的类型
	Name  string       // 节点的名称
	Group string       // 节点所属的组
}

// nodeKey 方法生成并返回当前节点的节点键。
func (n *Node) nodeKey() nodeKey {
	return nodeKey{t: n.Type, name: n.Name, group: n.Group} // 返回节点的键
}

// Param 表示图中的参数节点，是构造函数的输入。
type Param struct {
	*Node // 嵌入 Node 类型，继承了 Type, Name 和 Group 字段

	Optional bool // 表示该参数是否是可选的
}

// Result 表示图中的结果节点，是构造函数的输出。
type Result struct {
	*Node // 嵌入 Node 类型，继承了 Type, Name 和 Group 字段

	// GroupIndex 用于区分组内不同的值。
	// 由于分组中的值具有相同的类型和组名，它们的节点和字符串表示是相同的，因此需要使用索引来唯一标识这些值。
	GroupIndex int
}

// Group 表示图中的一个组节点，表示 fx 的值组。
type Group struct {
	Type      reflect.Type // 组内值的类型
	Name      string       // 组的名称
	Results   []*Result    // 组内的结果列表
	ErrorType ErrorType    // 错误类型
}

// nodeKey 方法生成并返回当前组的节点键。
func (g *Group) nodeKey() nodeKey {
	return nodeKey{t: g.Type, group: g.Name} // 返回组的节点键
}

// removeResult 从组中删除一个结果节点。
func (g *Group) removeResult(r *Result) {
	var pruned []*Result // 创建一个空的结果列表，用于存储去除结果后的组结果
	for _, rg := range g.Results {
		if r.GroupIndex != rg.GroupIndex { // 如果结果的索引不同于当前结果的索引
			pruned = append(pruned, rg) // 将当前结果添加到修剪后的列表中
		}
	}
	g.Results = pruned // 将修剪后的结果列表重新赋值给组的结果列表
}

// Graph 表示容器中的 DOT 格式图。
type Graph struct {
	Ctors    []*Ctor            // 所有构造函数的列表
	ctorMap  map[CtorID]*Ctor   // 按照 CtorID 索引的构造函数映射
	Groups   []*Group           // 所有组的列表
	groupMap map[nodeKey]*Group // 按照节点键索引的组映射

	consumers map[nodeKey][]*Ctor // 按节点键索引的消费者构造函数列表

	Failed *FailedNodes // 图中的失败节点
}

// FailedNodes 表示图中失败的节点。
type FailedNodes struct {
	RootCauses         []*Result            // 失败的根本原因
	TransitiveFailures []*Result            // 由于缺失或错误的依赖导致的传递性失败
	ctors              map[CtorID]struct{}  // 失败的构造函数集合
	groups             map[nodeKey]struct{} // 失败的组集合
}

// NewGraph 创建一个新的空图。
func NewGraph() *Graph {
	return &Graph{
		ctorMap:   make(map[CtorID]*Ctor),
		groupMap:  make(map[nodeKey]*Group),
		consumers: make(map[nodeKey][]*Ctor),
		Failed: &FailedNodes{
			ctors:  make(map[CtorID]struct{}),
			groups: make(map[nodeKey]struct{}),
		},
	}
}

// NewGroup 根据组的节点键创建一个新的组。
func NewGroup(k nodeKey) *Group {
	return &Group{
		Type: k.t,     // 设置组的类型
		Name: k.group, // 设置组的名称
	}
}
