package dig

import (
	"fmt"
	"io"
	"strings"
)

const (
	// _groupTag 表示组的标签常量。
	_groupTag = "group"
)

// group 类型表示一个组的结构，包含组的名称、是否扁平化以及是否软绑定的选项。
type group struct {
	// Name 是组的名称。
	Name string
	// Flatten 表示组是否需要扁平化。
	Flatten bool
	// Soft 表示组是否为软绑定。
	Soft bool
}

// errInvalidGroupOption 代表一个无效的组选项错误。
type errInvalidGroupOption struct {
	// Option 是无效的组选项。
	Option string
}

// 确保 errInvalidGroupOption 实现了 digError 接口。
//var _ digError = errInvalidGroupOption{}

// Error 实现了 errInvalidGroupOption 的 Error 方法，用于返回错误信息。
func (e errInvalidGroupOption) Error() string {
	return fmt.Sprint(e)
}

// writeMessage 实现了 digError 接口中的方法，用于格式化错误信息。
func (e errInvalidGroupOption) writeMessage(w io.Writer, v string) {
	// 输出无效选项的错误消息。
	fmt.Fprintf(w, "无效的选项 %q", e.Option)
}

// Format 实现了 fmt.Formatter 接口，用于格式化错误信息。
func (e errInvalidGroupOption) Format(w fmt.State, c rune) {
	// formatError(e, w, c)
}

// parseGroupString 解析一个包含组名称和选项的字符串，并返回一个 group 结构体。
// 格式为 "name,option1,option2"，其中 option 是 "flatten" 或 "soft"。
func parseGroupString(s string) (group, error) {
	// 使用逗号分割输入字符串，得到组件。
	components := strings.Split(s, ",")
	// 设置组的名称。
	g := group{Name: components[0]}
	// 遍历组件，解析选项。
	for _, c := range components[1:] {
		switch c {
		case "flatten":
			// 设置扁平化选项。
			g.Flatten = true
		case "soft":
			// 设置软绑定选项。
			g.Soft = true
		default:
			// 如果选项无效，返回错误。
			return g, errInvalidGroupOption{Option: c}
		}
	}
	// 返回解析后的组和无错误。
	return g, nil
}
