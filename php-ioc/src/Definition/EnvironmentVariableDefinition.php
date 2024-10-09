<?php

namespace Ioc\Definition;

use Override;
use const PHP_EOL;

/**
 * 环境变量定义类
 *
 * 该类用于表示环境变量的定义，包括变量名、是否可选、默认值等信息。
 * 它实现了 Definition 接口，并提供了相应的 getter 和 setter 方法。
 */
class EnvironmentVariableDefinition implements Definition
{
    private string $name = '';

    /**
     * 构造函数
     *
     * @param string $variableName 环境变量的名称
     * @param bool $isOptional 是否可选
     * @param mixed $defaultValue 默认值，如果变量不是必需的
     */
    public function __construct(
        private readonly string $variableName,
        private readonly bool   $isOptional = false,
        private mixed           $defaultValue = null,
    )
    {
    }

    /**
     * 获取环境变量名称
     *
     * @return string 环境变量名称
     */
    public function getVariableName(): string
    {
        return $this->variableName;
    }

    /**
     * 检查变量是否可选
     *
     * @return bool 如果可选则返回 true，否则返回 false
     */
    public function isOptional(): bool
    {
        return $this->isOptional;
    }

    /**
     * 获取默认值
     *
     * @return mixed 默认值
     */
    public function getDefaultValue(): mixed
    {
        return $this->defaultValue;
    }

    /**
     * 获取名称
     *
     * @return string 定义的名称
     */
    #[Override] public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置名称
     *
     * @param string $name 要设置的名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 替换嵌套的定义
     *
     * 该方法接受一个回调函数，用于替换默认值中的嵌套定义。
     *
     * @param callable $replacer 替换回调函数
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 使用替换回调函数替换默认值
        $this->defaultValue = $replacer($this->defaultValue);
    }

    /**
     * 将对象转换为字符串
     *
     * @return string 以字符串形式返回环境变量定义
     */
    #[Override] public function __toString(): string
    {
        $str = '    variable = ' . $this->variableName . PHP_EOL
            . '    optional = ' . ($this->isOptional ? 'yes' : 'no');

        // 如果是可选的，添加默认值
        if ($this->isOptional) {
            if ($this->defaultValue instanceof Definition) {
                // 如果默认值是一个定义对象，调用其字符串表示
                $nestedDefinition = (string)$this->defaultValue;
                $defaultValueStr = str_replace(PHP_EOL, PHP_EOL . '    ', $nestedDefinition);
            } else {
                // 使用 var_export 获取默认值的字符串表示
                $defaultValueStr = var_export($this->defaultValue, true);
            }

            $str .= PHP_EOL . '    default = ' . $defaultValueStr;
        }

        return sprintf('Environment variable (' . PHP_EOL . '%s' . PHP_EOL . ')', $str);
    }
}
