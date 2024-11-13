package digclock

import (
	"time"
)

// Clock 定义了 dig 如何访问时间。
type Clock interface {
	// Now 返回当前时间。
	Now() time.Time
	// Since 返回自给定时间以来经过的持续时间。
	Since(time.Time) time.Duration
}

// System 是基于真实时间的 Clock 的默认实现。
var System Clock = systemClock{}

// systemClock 是基于系统时间的 Clock 实现。
type systemClock struct{}

// Now 返回系统当前时间。
func (systemClock) Now() time.Time {
	return time.Now()
}

// Since 返回自给定时间以来经过的时间。
func (systemClock) Since(t time.Time) time.Duration {
	return time.Since(t)
}

// Mock 是一个假的时间源。
// 它实现了标准的时间操作，但允许用户控制时间的流逝。
//
// 使用 [Mock.Add] 方法可以推进时间。
//
// 注意：该实现不支持并发安全。
type Mock struct {
	// now 存储当前时间。
	now time.Time
}

// 确保 Mock 类型实现了 Clock 接口。
var _ Clock = (*Mock)(nil)

// NewMock 创建一个新的 Mock 时钟，并将当前时间设置为系统当前时间。
func NewMock() *Mock {
	return &Mock{now: time.Now()}
}

// Now 返回 Mock 时钟的当前时间。
func (m *Mock) Now() time.Time {
	return m.now
}

// Since 返回自给定时间以来经过的时间。
func (m *Mock) Since(t time.Time) time.Duration {
	return m.Now().Sub(t)
}

// Add 推进时间按给定的持续时间。
// 如果给定的持续时间为负数，程序将会 panic。
func (m *Mock) Add(d time.Duration) {
	if d < 0 {
		panic("不能添加负的持续时间")
	}
	m.now = m.now.Add(d)
}
