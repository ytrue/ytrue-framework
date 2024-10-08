<?php

namespace Ioc\Definition\ObjectDefinition;

/**
 * Class PropertyInjection
 *
 * 该类表示一个属性注入定义，用于在对象构造后注入指定属性。
 */
class PropertyInjection
{
    /**
     * @var string 要注入的属性名称
     */
    private string $propertyName;

    /**
     * @var mixed 注入的值
     */
    private mixed $value;

    /**
     * @var string|null 属性所属的类名（可选）
     */
    private ?string $className;

    /**
     * PropertyInjection constructor.
     *
     * @param string $propertyName 要注入的属性名称。
     * @param mixed $value 要注入的值。
     * @param string|null $className 属性所属的类名（可选）。
     */
    public function __construct(string $propertyName, mixed $value, ?string $className = null)
    {
        $this->propertyName = $propertyName;
        $this->value = $value;
        $this->className = $className;
    }

    /**
     * 获取要注入的属性名称。
     *
     * @return string 返回属性名称。
     */
    public function getPropertyName(): string
    {
        return $this->propertyName;
    }

    /**
     * 获取注入的值。
     *
     * @return mixed 返回注入的值。
     */
    public function getValue(): mixed
    {
        return $this->value;
    }

    /**
     * 获取属性所属的类名。
     *
     * @return string|null 返回属性所属的类名，或 null（如果未设置）。
     */
    public function getClassName(): ?string
    {
        return $this->className;
    }

    /**
     * 替换嵌套定义中的值。
     *
     * @param callable $replacer 用于替换值的回调函数。
     */
    public function replaceNestedDefinition(callable $replacer): void
    {
        $this->value = $replacer($this->value);
    }
}
