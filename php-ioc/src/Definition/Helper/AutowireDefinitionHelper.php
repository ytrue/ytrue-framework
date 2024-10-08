<?php

namespace Ioc\Definition\Helper;

use Ioc\Definition\AutowireDefinition;

/**
 * 自动装配定义助手类。
 *
 * 该类用于辅助定义自动装配的对象，允许为构造函数参数和方法参数定义特定值。
 * 它扩展了 CreateDefinitionHelper 类，并提供了更灵活的参数设置功能。
 */
class AutowireDefinitionHelper extends CreateDefinitionHelper
{
    /**
     * 定义类名常量。
     */
    public const DEFINITION_CLASS = AutowireDefinition::class;

    /**
     * 为构造函数的特定参数定义值。
     *
     * 此方法通常与属性或自动装配一起使用，当参数未被（或无法）类型提示时使用。
     * 使用此方法而不是 constructor() 可以避免定义所有参数（允许通过属性或自动装配解析），
     * 而只需定义一个参数。
     *
     * @param string|int $parameter 要设置值的参数名称或位置。
     * @param mixed $value 要提供给该参数的值。
     *
     * @return $this
     */
    public function constructorParameter(string|int $parameter, mixed $value): self
    {
        $this->constructor[$parameter] = $value; // 将参数值存储到构造函数参数数组中

        return $this; // 返回当前实例以支持链式调用
    }

    /**
     * 定义要调用的方法及其特定参数的值。
     *
     * 此方法通常与属性或自动装配一起使用，当参数未被（或无法）类型提示时使用。
     * 使用此方法而不是 method() 可以避免定义所有参数（允许通过属性或自动装配解析），
     * 而只需定义一个参数。
     *
     * 如果已经为该方法配置了多个调用（例如，在先前的定义中），
     * 此方法仅会覆盖*第一个*调用的参数。
     *
     * @param string $method 要调用的方法名称。
     * @param string|int $parameter 要设置值的参数名称或位置。
     * @param mixed $value 要提供给该参数的值。
     *
     * @return $this
     */
    public function methodParameter(string $method, string|int $parameter, mixed $value): self
    {
        // 特殊情况：处理构造函数
        if ($method === '__construct') {
            $this->constructor[$parameter] = $value; // 如果是构造函数，直接设置参数值

            return $this; // 返回当前实例以支持链式调用
        }

        // 如果该方法尚未被配置，则初始化
        if (!isset($this->methods[$method])) {
            $this->methods[$method] = [0 => []]; // 为方法添加一个新的调用数组
        }

        // 设置指定方法的参数值
        $this->methods[$method][0][$parameter] = $value;

        return $this; // 返回当前实例以支持链式调用
    }
}
