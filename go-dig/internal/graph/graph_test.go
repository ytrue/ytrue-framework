package graph

import (
	"fmt"
	"testing"
)

// mockGraph 实现了 Graph 接口，用于创建测试图。
type mockGraph struct {
	nodes int
	edges map[int][]int
}

func (g *mockGraph) Order() int {
	return g.nodes
}

func (g *mockGraph) EdgesFrom(u int) []int {
	return g.edges[u]
}

// TestIsAcyclic 测试 IsAcyclic 函数。
func TestIsAcyclic(t *testing.T) {
	// 定义无环图。
	acyclicGraph := &mockGraph{
		nodes: 100,
		edges: map[int][]int{
			0: {1},
			1: {2},
			2: {3},
			3: {1},
		},
	}

	// 测试无环图。
	isAcyclic, cycle := IsAcyclic(acyclicGraph)

	fmt.Println(isAcyclic)
	fmt.Println(cycle)

	//// 定义有环图。
	//cyclicGraph := &mockGraph{
	//	nodes: 3,
	//	edges: map[int][]int{
	//		0: {1},
	//		1: {2},
	//		2: {0}, // 形成环
	//	},
	//}
	//
	//// 测试有环图。
	//isAcyclic, cycle = IsAcyclic(cyclicGraph)
	//if isAcyclic || cycle == nil {
	//	t.Error("Expected cycle, got acyclic")
	//} else {
	//	t.Logf("Detected cycle: %v", cycle)
	//}
}
