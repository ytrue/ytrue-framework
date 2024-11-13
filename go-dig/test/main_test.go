package main

func main() {

	//container := dig.New()
	//
	//container.Provide(func() {})

}

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
