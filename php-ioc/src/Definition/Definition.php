<?php

namespace Ioc\Definition;

use Ioc\Factory\RequestedEntry;
use Stringable;

/**
 * 定义一个接口 Definition，用于描述容器中的服务定义
 * 实现了 RequestedEntry 和 Stringable 接口
 */
interface Definition extends RequestedEntry, Stringable
{

    /**
     * 获取定义的名称
     *
     * @return string 返回定义的名称
     */
    public function getName(): string;

    /**
     * 设置定义的名称
     *
     * @param string $name 定义的名称
     */
    public function setName(string $name): void;

    /**
     * 替换定义中的嵌套定义
     *
     * @param callable $replacer 用于替换嵌套定义的回调函数
     */
    public function replaceNestedDefinitions(callable $replacer): void;

    /**
     * 将定义转换为字符串
     *
     * @return string 定义的字符串表示
     */
    public function __toString(): string;
}
