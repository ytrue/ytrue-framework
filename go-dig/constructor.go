package dig

import (
	"github.com/ytrue/dig/internal/digreflect"
	"github.com/ytrue/dig/internal/dot"
	"reflect"
)

const (
	_optionalTag         = "optional"
	_nameTag             = "name"
	_ignoreUnexportedTag = "ignore-unexported"
)

// constructorNode 表示依赖图中的一个节点，用于表示用户提供的构造函数。
//
// constructorNode 可以生成零个或多个值并将它们存储到容器中。
// 在 Provide 路径中，我们会验证 constructorNode 至少生成一个值，否则该函数将永远不会被调用。
type constructorNode struct {
	// ctor 是构造函数，表示用户提供的具体构造逻辑。
	ctor interface{}

	// ctype 是构造函数的反射类型。
	ctype reflect.Type

	// location 记录该构造函数的定义位置，用于调试和错误信息。
	location *digreflect.Func

	// id 用于唯一标识生成该节点的构造函数。
	id dot.CtorID

	// called 标记构造函数是否已经被调用，避免重复调用。
	called bool

	// paramList 包含构造函数的参数类型信息。
	paramList paramList

	// resultList 包含构造函数的返回值类型信息。
	//	resultList resultList

	// orders 存储了该节点在每个 Scope 的 graphHolder 中的顺序位置，用于依赖图的排序。
	orders map[*Scope]int

	// s 表示该节点所在的 Scope。
	s *Scope

	// origS 表示该节点最初被提供的 Scope。
	// 如果 constructor 是使用 ExportOption 提供的，origS 和 s 会有所不同。
	origS *Scope

	// callback 是构造函数执行后的回调函数（如果存在），用于在构造函数完成后执行特定操作。
	callback Callback
}

// constructorOptions 用于存储与构造函数相关的选项配置。
type constructorOptions struct {
	// ResultName 如果指定，所有由该构造函数产生的值将被赋予该名称。
	// 该名称通常用于标识不同的构造函数结果。
	ResultName string

	// ResultGroup 如果指定，所有由该构造函数产生的值将属于指定的值组。
	// 该组用于对值进行分组，便于在不同的上下文中进行管理。
	ResultGroup string

	// ResultAs 该字段用于指定构造函数结果的类型转换。
	// 构造函数返回的值将被认为是实现这些接口之一的类型。
	ResultAs []interface{}

	// Location 存储构造函数的位置信息，通常包含包名、函数名和所在文件的路径及行号。
	// 这对于调试和错误追踪非常有用。
	Location *digreflect.Func

	// Callback 如果指定，当构造函数完成时，会调用此回调函数。
	// 回调函数接收有关构造函数执行的信息，如执行时间、错误等。
	Callback Callback
}
