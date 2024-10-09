<?php

namespace Ioc\Definition;

use Override;
use const PHP_EOL;

/**
 * ArrayDefinition 类实现了 Definition 接口
 * 用于描述一个由数组构成的定义对象
 */
class ArrayDefinition implements Definition
{

    /**
     * bean 名称
     * @var string
     */
    private string $name;

    /**
     * 定义的值数组
     * @var array
     */
    private array $values;

    /**
     * 构造函数，接受一个数组作为定义的值
     *
     * @param array $values 定义的值数组
     */
    public function __construct(array $values)
    {
        $this->values = $values;
    }

    /**
     * 获取定义的值数组
     *
     * @return array 返回定义的值数组
     */
    public function getValues(): array
    {
        return $this->values;
    }

    /**
     * 获取定义的名称
     *
     * @return string 返回定义的名称
     */
    #[Override] public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置定义的名称
     *
     * @param string $name 定义的名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 替换定义中的嵌套定义
     * 使用传入的回调函数对定义中的嵌套值进行替换
     *
     * @param callable $replacer 用于替换嵌套定义的回调函数
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 使用 array_map 函数对数组中的每个元素应用 $replacer 函数
        $this->values = array_map($replacer, $this->values);
    }

    /**
     * 将定义转换为字符串
     *
     * @return string 返回定义的字符串表示，包含值数组的格式化输出
     */
    #[Override] public function __toString(): string
    {
        // 开始格式化数组为字符串
        $str = '[' . PHP_EOL;

        // 遍历数组中的每个键值对
        foreach ($this->values as $key => $value) {
            // 如果键是字符串，给它加上引号
            if (is_string($key)) {
                $key = "'" . $key . "'";
            }

            // 添加键和值的字符串表示
            $str .= '    ' . $key . ' => ';

            // 如果值是一个 Definition 实例，则递归调用其 __toString 方法
            if ($value instanceof Definition) {
                $str .= str_replace(PHP_EOL, PHP_EOL . '    ', (string)$value);
            } else {
                // 否则，使用 var_export 导出值
                $str .= var_export($value, true);
            }

            // 添加数组元素后的逗号和换行符
            $str .= ',' . PHP_EOL;
        }

        // 返回格式化好的数组字符串
        return $str . ']';
    }
}
