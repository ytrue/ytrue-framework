package dig

import "time"

// CallbackInfo 结构体包含了 Dig 在执行提供的函数或装饰器时的信息，
// 该结构体会传递给通过 [WithProviderCallback] 或 [WithDecoratorCallback] 注册的 [Callback]。
type CallbackInfo struct {
	// Name 字段表示函数的名称，格式为：<package_name>.<function_name>
	Name string

	// Error 字段包含 [Callback] 关联的函数返回的错误（如果有）。
	// 在与 [RecoverFromPanics] 一起使用时，如果函数发生 panic，则会设置为 [PanicError]。
	Error error

	// Runtime 字段表示关联函数执行所花费的时间。
	Runtime time.Duration
}

// Callback 是一种可以注册到提供的函数或装饰器中的函数，
// 通过 [WithCallback] 注册后会在函数或装饰器执行结束后被调用。
type Callback func(CallbackInfo)

// WithProviderCallback 返回一个 [ProvideOption]，Dig 会在对应的构造函数执行完毕后调用传入的 [Callback]。
//
// 例如，以下代码在 "myConstructor" 执行完毕后打印一条完成消息，包括可能的错误信息：
//
//	c := dig.New()
//	myCallback := func(ci CallbackInfo) {
//		var errorAdd string
//		if ci.Error != nil {
//			errorAdd = fmt.Sprintf("with error: %v", ci.Error)
//		}
//		fmt.Printf("%q finished%v", ci.Name, errorAdd)
//	}
//	c.Provide(myConstructor, WithProviderCallback(myCallback)),
//
// 也可以使用 [WithDecoratorCallback] 为装饰器指定回调函数。
//
// 有关传递给 [Callback] 的信息的详细信息，请参阅 [CallbackInfo]。
func WithProviderCallback(callback Callback) ProvideOption {
	return withCallbackOption{
		callback: callback,
	}
}

// WithDecoratorCallback 返回一个 [DecorateOption]，Dig 会在对应的装饰器执行完毕后调用传入的 [Callback]。
//
// 例如，以下代码在 "myDecorator" 执行完毕后打印一条完成消息，包括可能的错误信息：
//
//	c := dig.New()
//	myCallback := func(ci CallbackInfo) {
//		var errorAdd string
//		if ci.Error != nil {
//			errorAdd = fmt.Sprintf("with error: %v", ci.Error)
//		}
//		fmt.Printf("%q finished%v", ci.Name, errorAdd)
//	}
//	c.Decorate(myDecorator, WithDecoratorCallback(myCallback)),
//
// 也可以使用 [WithProviderCallback] 为构造函数指定回调函数。
//
// 有关传递给 [Callback] 的信息的详细信息，请参阅 [CallbackInfo]。
func WithDecoratorCallback(callback Callback) DecorateOption {
	return withCallbackOption{
		callback: callback,
	}
}

// withCallbackOption 结构体用于包装 Callback 函数，使其可以在提供的函数或装饰器执行结束后被调用。
type withCallbackOption struct {
	callback Callback
}

// 声明 withCallbackOption 实现了 ProvideOption 和 DecorateOption 接口
var (
	_ ProvideOption  = withCallbackOption{}
	_ DecorateOption = withCallbackOption{}
)

// applyProvideOption 方法将回调函数应用到 provideOptions 中，使其在提供的构造函数执行结束后调用。
func (o withCallbackOption) applyProvideOption(po *provideOptions) {
	po.Callback = o.callback
}

// apply 方法将回调函数应用到 decorateOptions 中，使其在装饰器执行结束后调用。
func (o withCallbackOption) apply(do *decorateOptions) {
	do.Callback = o.callback
}
