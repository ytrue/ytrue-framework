package dig

//
//import (
//	"fmt"
//	"reflect"
//)
//
//// result 接口代表由构造函数生成的结果。
////
//// 以下是实现：
////   resultList    所有由构造函数返回的值。
////   resultSingle  单个由构造函数生成的值。
////   resultObject  dig.Out 结构体，其中每个字段可以是另一个结果。
////   resultGrouped 由构造函数生成的一个值，是某一组值的一部分。
//
//// result 接口，定义了从容器中提取结果并存储的方法。
//type result interface {
//	// Extract 从提供的值中提取这个结果，并将它们存储到提供的容器中。
//	// 如果该结果不消费单一值，可能会发生 panic。
//	//Extract(containerWriter, bool, reflect.Value)
//
//	// DotResult 返回一个 dot.Result 切片。
//	DotResult() []*dot.Result
//}
//
//// 定义了多个 result 类型的实例，验证其实现了 result 接口。
//var (
//	_ result = resultSingle{}
//	_ result = resultObject{}
//	_ result = resultList{}
//	_ result = resultGrouped{}
//)
//
//// resultOptions 是创建结果时的一些配置选项。
//type resultOptions struct {
//	// 如果设置了这个选项，则是关联结果值的名称。
//	//
//	// 对于 Result 对象，字段上的 name:".." 标签优先于此选项。
//	Name  string
//	Group string
//	As    []interface{}
//}
//
//// newResult 根据给定的类型构建一个结果。
//func newResult(t reflect.Type, opts resultOptions) (result, error) {
//	switch {
//	case IsIn(t) || (t.Kind() == reflect.Ptr && IsIn(t.Elem())) || embedsType(t, _inPtrType):
//		// 检查是否可以作为输入类型提供，如果是，则返回错误。
//		return nil, newErrInvalidInput(fmt.Sprintf(
//			"不能提供参数对象: %v 包含 dig.In", t), nil)
//	case isError(t):
//		// 检查返回值是否为错误类型，如果是，则返回错误。
//		return nil, newErrInvalidInput("不能在此返回错误，应该从构造函数返回它", nil)
//	case IsOut(t):
//		// 如果类型是 dig.Out 类型，创建一个结果对象。
//		return newResultObject(t, opts)
//	case embedsType(t, _outPtrType):
//		// 如果嵌入了 *dig.Out 类型，返回错误。
//		return nil, newErrInvalidInput(fmt.Sprintf(
//			"不能通过嵌入 *dig.Out 创建结果对象，应该嵌入 dig.Out: %v 嵌入 *dig.Out", t), nil)
//	case t.Kind() == reflect.Ptr && IsOut(t.Elem()):
//		// 如果类型是指向 dig.Out 类型的指针，返回错误。
//		return nil, newErrInvalidInput(fmt.Sprintf(
//			"不能返回指向结果对象的指针，应该返回值: %v 是指向嵌入 dig.Out 的结构体的指针", t), nil)
//	case len(opts.Group) > 0:
//		// 如果指定了组名，则解析组名。
//		g, err := parseGroupString(opts.Group)
//		if err != nil {
//			return nil, newErrInvalidInput(
//				fmt.Sprintf("无法解析组 %q", opts.Group), err)
//		}
//		// 创建一个分组结果对象。
//		rg := resultGrouped{Type: t, Group: g.Name, Flatten: g.Flatten}
//		// 处理 As 类型。
//		if len(opts.As) > 0 {
//			var asTypes []reflect.Type
//			for _, as := range opts.As {
//				ifaceType := reflect.TypeOf(as).Elem()
//				if ifaceType == t {
//					continue
//				}
//				if !t.Implements(ifaceType) {
//					return nil, newErrInvalidInput(
//						fmt.Sprintf("无效的 dig.As: %v 没有实现 %v", t, ifaceType), nil)
//				}
//				asTypes = append(asTypes, ifaceType)
//			}
//			if len(asTypes) > 0 {
//				rg.Type = asTypes[0]
//				rg.As = asTypes[1:]
//			}
//		}
//		// 检查 soft 标志的使用情况。
//		if g.Soft {
//			return nil, newErrInvalidInput(fmt.Sprintf(
//				"不能对结果值组使用 soft: soft 已在组 %q 中使用", g.Name), nil)
//		}
//		// 检查 flatten 是否适用于切片类型。
//		if g.Flatten {
//			if t.Kind() != reflect.Slice {
//				return nil, newErrInvalidInput(fmt.Sprintf(
//					"flatten 只能应用于切片类型: %v 不是切片", t), nil)
//			}
//			rg.Type = rg.Type.Elem()
//		}
//		return rg, nil
//	default:
//		// 创建一个单一的结果类型。
//		return newResultSingle(t, opts)
//	}
//}
