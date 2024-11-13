package dig

import "reflect"

// paramList 存储构造函数的所有参数作为 params。
//
// 注意：不能直接调用 paramList 的 Build() 方法，
// 而是必须调用 BuildList 方法来构建参数列表。
type paramList struct {
	// ctype 是构造函数的类型。
	ctype reflect.Type

	// Params 是构造函数的参数列表。
	//Params []param
}
