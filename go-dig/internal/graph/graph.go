package graph

// Graph 表示一个简单的有向图接口。
// 假设图中的每个节点都用一个递增的正整数唯一标识（例如，1, 2, 3...）。
// 其中，0 值表示一个无效节点，用于错误处理。
type Graph interface {
	// Order 返回图中节点的总数。
	Order() int

	// EdgesFrom 返回一个整数列表，每个整数表示
	// 与节点 u 存在边连接的节点。
	EdgesFrom(u int) []int
}

// IsAcyclic 使用深度优先搜索检查图是否无环。
// 如果检测到环，返回环中的节点顺序列表；
// 否则返回一个空列表。
func IsAcyclic(g Graph) (bool, []int) {
	// cycleStart 是引入环的节点。范围 [1, g.Order()) 表示 g 存在环。
	info := newCycleInfo(g.Order())

	for i := 0; i < g.Order(); i++ {
		// 重置 []cycleNode 所有 OnStack 为 false
		info.Reset()

		cycle := isAcyclic(g, i, info, nil /* 循环路径 */)
		if len(cycle) > 0 {
			return false, cycle
		}
	}

	return true, nil
}

// isAcyclic 从特定节点开始，使用深度优先搜索遍历图。
// 如果检测到环，返回引入该环的最后一个节点。
// 例如，对以下图从节点 1 开始的调用将返回节点 3：
//
//	1 -> 2 -> 3 -> 1
func isAcyclic(g Graph, u int, info cycleInfo, path []int) []int {
	// 如果此节点已访问且无环，直接返回。
	if info[u].Visited {
		return nil
	}
	info[u].Visited = true
	info[u].OnStack = true

	path = append(path, u)
	for _, v := range g.EdgesFrom(u) {
		if !info[v].Visited {
			// 递归检测 v 节点是否形成环。
			if cycle := isAcyclic(g, v, info, path); len(cycle) > 0 {
				return cycle
			}
		} else if info[v].OnStack {
			// 检测到环，截取路径为环节点。
			cycle := path
			for i := len(cycle) - 1; i >= 0; i-- {
				if cycle[i] == v {
					cycle = cycle[i:]
					break
				}
			}

			// 补全环，添加当前节点。
			return append(cycle, v)
		}
	}
	info[u].OnStack = false
	return nil
}

// cycleNode 记录单个节点的循环检测信息。
// Visited 表示该节点是否被访问过。
// OnStack 表示当前节点是否在递归调用栈中。
type cycleNode struct {
	Visited bool
	OnStack bool
}

// cycleInfo 包含每个节点的循环检测信息，用于环检测。
type cycleInfo []cycleNode

// newCycleInfo 初始化并返回一个给定大小的 cycleInfo 实例。
func newCycleInfo(order int) cycleInfo {
	return make(cycleInfo, order)
}

// Reset 清除循环检测中的栈状态，使循环检测重新开始。
func (info cycleInfo) Reset() {
	for i := range info {
		info[i].OnStack = false
	}
}
