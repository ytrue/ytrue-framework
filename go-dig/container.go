package dig

import "reflect"

// Container 是一个有向无环图（DAG），用于存储类型及其依赖关系。
// Container 代表了顶级作用域的依赖图，
// 是整个依赖关系树的根节点。
type Container struct {
	// scope 是依赖关系树的根作用域，表示根的 Scope。
	// Scope 是管理类型实例及其生命周期的单元，
	// 在容器中通过作用域来隔离不同的依赖关系。
	scope *Scope
}

// Option 用于配置 Container。
type Option interface {
	// applyOption 方法将配置应用到 Container 实例。
	applyOption(*Container)
}

// New 构造一个新的 Container（容器）。
// 该函数接收可变参数 opts，用于传递配置选项，以便在创建 Container 时应用。
// 通过应用这些选项，可以在创建时对 Container 的行为进行定制。
func New(opts ...Option) *Container {
	//// 创建一个新的作用域（scope），表示依赖关系的上下文环境。
	//s := newScope()
	//
	//// 初始化一个 Container 实例，并将 scope 赋值给该实例的 scope 字段。
	//c := &Container{scope: s}
	//
	//// 遍历每个传入的配置选项并应用到 Container 上，
	//// 使这些选项能够影响 Container 的配置或初始化过程。
	//for _, opt := range opts {
	//	opt.applyOption(c)
	//}
	//
	//// 返回构造完成并配置好的 Container 实例。
	//return c
	return nil
}

// key 用于唯一标识依赖图中的对象。
type key struct {
	// t 表示对象的类型。
	t reflect.Type

	// name 表示对象的名称，用于标识特定命名的依赖。
	// group 表示对象所属的分组名，主要用于标识一组相同类型的依赖。
	// name 和 group 中只有一个会被设置，以确保唯一标识。
	name  string
	group string
}
