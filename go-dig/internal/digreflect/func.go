package digreflect

import (
	"fmt"
	"net/url"
	"reflect"
	"runtime"
	"strings"
)

// Func 包含一个函数的运行时信息。
type Func struct {
	// Name 是函数的名称。
	Name string

	// Package 是定义此函数的包名。
	Package string

	// File 是定义此函数的文件路径。
	File string

	// Line 是此函数在文件中的行号。
	Line int
}

// String 返回该函数的字符串表示。
func (f *Func) String() string {
	return fmt.Sprint(f)
}

// Format 实现了 fmt.Formatter 接口，用于 Func 的格式化输出。
// 当使用 %+v 时，返回多行格式；否则返回单行格式。
func (f *Func) Format(w fmt.State, c rune) {
	if w.Flag('+') && c == 'v' {
		// 例子:
		// "path/to/package".MyFunction
		// 	path/to/file.go:42
		fmt.Fprintf(w, "%q.%v", f.Package, f.Name)
		fmt.Fprintf(w, "\n\t%v:%v", f.File, f.Line)
	} else {
		// 例子:
		// "path/to/package".MyFunction (path/to/file.go:42)
		fmt.Fprintf(w, "%q.%v (%v:%v)", f.Package, f.Name, f.File, f.Line)
	}
}

// InspectFunc 检查并返回给定函数的运行时信息。
// 参数 function 是任意函数，用于提取其运行时信息。
func InspectFunc(function interface{}) *Func {
	fptr := reflect.ValueOf(function).Pointer()
	return InspectFuncPC(fptr)
}

// InspectFuncPC 根据程序计数器地址 pc 检查并返回函数的运行时信息。
// 参数 pc 是函数的地址，用于提取其相关信息。
func InspectFuncPC(pc uintptr) *Func {
	f := runtime.FuncForPC(pc)
	if f == nil {
		return nil
	}
	pkgName, funcName := splitFuncName(f.Name())
	fileName, lineNum := f.FileLine(pc)
	return &Func{
		Name:    funcName,
		Package: pkgName,
		File:    fileName,
		Line:    lineNum,
	}
}

const _vendor = "/vendor/"

// splitFuncName 将完整的函数名称分为包名和函数名两部分。
// 参数 function 是完整的函数名称，例如 "path/to/pkg.MyFunction"。
// 返回值 pname 是包名，fname 是函数名。
func splitFuncName(function string) (pname string, fname string) {
	if len(function) == 0 {
		return
	}

	// 提取包名和函数名分界的索引
	idx := 0
	if i := strings.LastIndex(function, "/"); i >= 0 {
		idx = i
	}
	if i := strings.Index(function[idx:], "."); i >= 0 {
		idx += i
	}
	pname, fname = function[:idx], function[idx+1:]

	// 如果包名中包含 vendored 路径，则删除 vendored 路径前缀
	if i := strings.Index(pname, _vendor); i > 0 {
		pname = pname[i+len(_vendor):]
	}

	// 对包名解码，防止其中包含特殊字符（如 ".git"）
	if unescaped, err := url.QueryUnescape(pname); err == nil {
		pname = unescaped
	}

	return
}
