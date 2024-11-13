package dig

import (
	"container/list"
	"fmt"
	"reflect"
	"strconv"
)

var (
	_noValue    reflect.Value
	_errType    = reflect.TypeOf((*error)(nil)).Elem()
	_inPtrType  = reflect.TypeOf((*In)(nil))
	_inType     = reflect.TypeOf(In{})
	_outPtrType = reflect.TypeOf((*Out)(nil))
	_outType    = reflect.TypeOf(Out{})
)

// digSentinel类型作为占位符，表示dig.In和dig.Out结构体的特殊性质。
// 如果没有这个类型，它们会变成普通的空结构体。
type digSentinel struct{}

// In 是一个可嵌入的类型，用于标识dig包的输入参数。
// 通过嵌入该类型，结构体的字段将会被认为是依赖项，而不是结构体本身。
// 具体实现上，通过dig.In结构体，我们能够在构造函数中指定这些字段作为依赖项。
type In struct{ _ digSentinel } // 空结构体，作为标识用

// Out 是一个可嵌入的类型，用于标识dig包的输出结果。
// 通过嵌入该类型，结构体的字段将会被认为是构造函数的返回值，而不是结构体本身。
// 类似dig.In，dig.Out用来自动将返回的字段作为构造函数的结果注入。
type Out struct{ _ digSentinel } // 空结构体，作为标识用

// isError函数判断给定的类型是否实现了error接口
// 如果类型t实现了error接口，返回true，否则返回false
func isError(t reflect.Type) bool {
	return t.Implements(_errType)
}

// IsIn 函数检查给定的结构体是否是dig.In类型。
// 结构体通过嵌入dig.In类型或其父类型来实现dig.In标记。
// 只有符合这个条件的结构体字段才能被dig正确处理。
func IsIn(o interface{}) bool {
	return embedsType(o, _inType) // 判断给定的接口类型o是否包含dig.In类型
}

// IsOut 函数检查给定的结构体是否是dig.Out类型。
// 结构体通过嵌入dig.Out类型或其父类型来实现dig.Out标记。
// 只有符合这个条件的结构体字段才能被dig正确处理。
func IsOut(o interface{}) bool {
	return embedsType(o, _outType) // 判断给定的接口类型o是否包含dig.Out类型
}

// embedsType函数检查一个类型t是否嵌入了某个类型e。
// 该函数通过广度优先搜索结构体中的所有嵌入字段，查找是否存在目标类型e。
// 用于判断结构体是否包含指定类型，通常用于判断结构体是否嵌入了dig.In或dig.Out类型。
func embedsType(i interface{}, e reflect.Type) bool {
	// 如果接口为nil，直接返回false
	if i == nil {
		return false
	}

	// 将输入的接口转换为reflect.Type类型
	t, ok := i.(reflect.Type)
	if !ok {
		// 如果输入参数不是reflect.Type，则获取其类型
		t = reflect.TypeOf(i)
	}

	// 使用一个链表来实现广度优先搜索结构体中的所有字段类型
	types := list.New() // 创建一个空链表，用于存储待检查的类型
	types.PushBack(t)   // 将初始类型t添加到链表

	// 循环检查链表中的每个类型
	for types.Len() > 0 {
		// 从链表中取出第一个类型
		t := types.Remove(types.Front()).(reflect.Type)

		// 如果当前类型等于目标类型e，返回true
		if t == e {
			return true
		}

		// 如果当前类型是结构体类型，检查其嵌入字段
		if t.Kind() != reflect.Struct {
			continue
		}

		// 遍历当前结构体的每个字段，如果该字段是匿名字段，则将其类型加入链表
		for i := 0; i < t.NumField(); i++ {
			f := t.Field(i)
			if f.Anonymous { // 判断字段是否是匿名字段
				types.PushBack(f.Type) // 将匿名字段的类型加入链表，进行后续检查
			}
		}
	}

	// 如果遍历完所有嵌入字段都没有找到目标类型e，返回false
	return false
}

// isFieldOptional函数检查In结构体的字段是否为可选的。
// 通过检查字段的标签，判断该字段是否为可选依赖。
// 如果字段的标签中有"optional"并且其值为true，则该字段为可选字段。
func isFieldOptional(f reflect.StructField) (bool, error) {
	tag := f.Tag.Get(_optionalTag) // 获取字段的optional标签
	if tag == "" {
		return false, nil // 如果没有设置optional标签，则返回false
	}

	// 如果标签值不为空，尝试将其转换为布尔值
	optional, err := strconv.ParseBool(tag)
	if err != nil {
		// 如果标签值无法转换为布尔值，返回错误
		err = newErrInvalidInput(
			fmt.Sprintf("invalid value %q for %q tag on field %v", tag, _optionalTag, f.Name), err)
	}

	return optional, err // 返回字段是否可选以及可能出现的错误
}
