<?php


namespace Ioc\Definition;

/**
 * 实例定义类
 *
 * 该类用于表示一个实例的定义，包括实例对象和与该实例相关的对象定义。
 * 它实现了 Definition 接口，并提供了相应的 getter 和 setter 方法。
 */
class InstanceDefinition implements Definition
{
    /**
     * 构造函数
     *
     * @param object $instance 实例对象
     * @param ObjectDefinition $objectDefinition 与该实例相关的对象定义
     */
    public function __construct(
        private object           $instance,
        private ObjectDefinition $objectDefinition,
    )
    {
    }

    /**
     * 获取名称
     *
     * @return string 返回空字符串，因为此定义不具名
     */
    public function getName(): string
    {
        return '';
    }

    /**
     * 设置名称
     *
     * @param string $name 要设置的名称，但此定义不使用名称
     */
    public function setName(string $name): void
    {
        // 此定义不需要设置名称，因此此方法为空
    }

    /**
     * 获取实例对象
     *
     * @return object 返回实例对象
     */
    public function getInstance(): object
    {
        return $this->instance;
    }

    /**
     * 获取对象定义
     *
     * @return ObjectDefinition 返回与该实例相关的对象定义
     */
    public function getObjectDefinition(): ObjectDefinition
    {
        return $this->objectDefinition;
    }

    /**
     * 替换嵌套的定义
     *
     * 该方法接受一个回调函数，用于替换对象定义中的嵌套定义。
     *
     * @param callable $replacer 替换回调函数
     */
    public function replaceNestedDefinitions(callable $replacer): void
    {
        // 使用替换回调函数替换对象定义中的嵌套定义
        $this->objectDefinition->replaceNestedDefinitions($replacer);
    }

    /**
     * 将对象转换为字符串
     *
     * @return string 返回 'Instance' 字符串，表示该定义的类型
     */
    public function __toString(): string
    {
        return 'Instance';
    }
}
