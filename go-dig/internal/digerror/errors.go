package digerror

import "fmt"

// BugPanicf 会触发 panic，并输出一个格式化的消息，提示用户前往 GitHub 提交 bug。
//
// 消息中会包含指示用户提交问题的链接，以及传入的错误信息。任何额外的参数都会被格式化并加入到消息中。
//
// 示例用法：
//
//	BugPanicf("发生了错误: %v", err)
//
// 参数：
//   - msg: 包含格式化信息的字符串，作为 panic 消息的一部分。
//   - args: 一个可变参数列表，用于格式化消息（可选）。
//
// 该函数会调用 panic()，并附带详细的消息，提示用户在 GitHub 仓库页面报告 bug。
func BugPanicf(msg string, args ...interface{}) {
	panic(fmt.Sprintf("看起来你发现了 dig 库中的一个 bug。"+
		"请在 https://github.com/uber-go/dig/issues/new 提交一个问题，并提供以下信息："+
		msg, args...))
}
