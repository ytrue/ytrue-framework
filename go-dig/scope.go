package dig

import (
	"math/rand"
	"reflect"
)

type Scope struct {
	// 该结构体实现了 containerStore 接口。

	// Scope 的名称
	name string

	// providers 是一个从键到构造函数节点的映射，存储了可以为特定键提供值的所有构造函数节点。
	providers map[key][]*constructorNode

	// decorators 是一个从键到装饰器节点的映射，用于存储装饰特定键值的装饰器。
	//decorators map[key]*decoratorNode

	// nodes 存储了直接提供给当前 Scope 的构造函数节点，不包含从父 Scope 继承的节点。
	nodes []*constructorNode

	// decoratedValues 存储通过装饰器在 Scope 中生成的值，使用键来映射对应的值。
	decoratedValues map[key]reflect.Value

	// values 存储在 Scope 中直接生成的值，使用键来映射对应的值。
	values map[key]reflect.Value

	// groups 存储在 Scope 中直接生成的值组，使用键映射一组值。
	groups map[key][]reflect.Value

	// decoratedGroups 存储在 Scope 中通过装饰器生成的值组，使用键映射对应的一组值。
	decoratedGroups map[key]reflect.Value

	// rand 是一个随机源，用于生成随机数。
	rand *rand.Rand

	// isVerifiedAcyclic 标识图是否已检查过循环依赖。
	isVerifiedAcyclic bool

	// deferAcyclicVerification 标记是否在调用时延迟对图的无循环检查。
	deferAcyclicVerification bool

	// recoverFromPanics 标记是否从用户提供的代码中的 panic 恢复，并将其包装成导出的错误类型。
	recoverFromPanics bool

	// invokerFn 是一个调用函数，用于在 Provide 或 Invoke 时传递参数。
	//invokerFn invokerFn

	// gh 是该 Scope 的依赖图，包含影响此 Scope 的所有节点的依赖关系图，而不仅限于直接提供给当前 Scope 的节点。
	//gh *graphHolder

	// parentScope 是该 Scope 的父 Scope。
	parentScope *Scope

	// childScopes 存储该 Scope 的所有子 Scope。
	childScopes []*Scope

	// clockSrc 用于存储时间源，默认使用系统时钟。
	//	clockSrc digclock.Clock
}
