package dig

import (
	"bytes"
	"errors"
	"fmt"
	"github.com/ytrue/dig/internal/digreflect"
	"io"
)

// cycleErrPathEntry 表示在依赖循环中每个路径条目的信息。
// 它包含了依赖的键以及相关函数。
type cycleErrPathEntry struct {
	// Key 是依赖的键。
	Key key
	// Func 是提供该依赖的函数。
	Func *digreflect.Func
}

// errCycleDetected 表示检测到依赖循环的错误。
// 它记录了发生循环的路径和作用域信息。
type errCycleDetected struct {
	// Path 记录了依赖循环的路径信息。
	Path []cycleErrPathEntry
	// scope 是当前作用域。
	scope *Scope
}

// 确保 errCycleDetected 实现了 digError 接口。
//var _ digError = errCycleDetected{}

// Error 实现了 errCycleDetected 的 Error 方法，返回错误的详细信息。
// 错误信息包括发生依赖循环的作用域以及依赖链。
func (e errCycleDetected) Error() string {
	// 返回的错误信息类似如下：
	//
	//   [scope "foo"]
	//   func(*bar) *foo 提供者是 "path/to/package".NewFoo (path/to/file.go:42)
	//   	依赖于 func(*baz) *bar 提供者是 "another/package".NewBar (somefile.go:1)
	//   	依赖于 func(*foo) baz 提供者是 "somepackage".NewBar (anotherfile.go:2)
	//   	依赖于 func(*bar) *foo 提供者是 "path/to/package".NewFoo (path/to/file.go:42)
	//
	b := new(bytes.Buffer)

	// 如果作用域名称存在，打印作用域名称。
	if name := e.scope.name; len(name) > 0 {
		fmt.Fprintf(b, "[scope %q]\n", name)
	}
	// 遍历路径并打印每个依赖项的信息。
	for i, entry := range e.Path {
		if i > 0 {
			// 如果不是第一个依赖，添加 "depends on"。
			b.WriteString("\n\tdepends on ")
		}
		// 打印当前依赖项的键和提供该依赖的函数。
		fmt.Fprintf(b, "%v provided by %v", entry.Key, entry.Func)
	}
	return b.String()
}

// writeMessage 实现了 digError 接口中的方法，用于格式化错误信息。
func (e errCycleDetected) writeMessage(w io.Writer, v string) {
	// 打印错误信息。
	fmt.Fprint(w, e.Error())
}

// Format 实现了 fmt.Formatter 接口，用于格式化错误信息。
func (e errCycleDetected) Format(w fmt.State, c rune) {
	// formatError(e, w, c)
}

// IsCycleDetected 返回一个布尔值，指示提供的错误是否表示容器图中检测到循环依赖。
func IsCycleDetected(err error) bool {
	// 使用 errors.As 检查错误是否为 errCycleDetected 类型。
	return errors.As(err, &errCycleDetected{})
}
