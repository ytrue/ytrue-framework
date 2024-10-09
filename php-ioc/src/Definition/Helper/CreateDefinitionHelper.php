<?php

namespace Ioc\Definition\Helper;

use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use Ioc\Definition\ObjectDefinition\PropertyInjection;
use Override;
use ReflectionParameter;

/**
 * 创建定义助手类。
 *
 * 该类用于定义一个对象的创建过程，包括构造函数参数、属性注入和方法调用等。
 */
class CreateDefinitionHelper implements DefinitionHelper
{
    /**
     * 定义类名常量。
     */
    private const DEFINITION_CLASS = ObjectDefinition::class;

    /**
     * 类名。
     *
     * @var string|null
     */
    private ?string $className;

    /**
     * 是否懒加载。
     *
     * @var bool|null
     */
    private ?bool $lazy = null;

    /**
     * 构造函数参数数组。
     *
     * @var array
     */
    protected array $constructor = [];

    /**
     * 属性及其值的数组。
     *
     * @var array
     */
    private array $properties = [];

    /**
     * 方法及其参数的数组。
     *
     * @var array
     */
    protected array $methods = [];

    /**
     * 构造函数。
     *
     * @param string|null $className 对象的类名。
     *                               如果为 null，将使用容器中的条目名称作为类名。
     */
    public function __construct(?string $className = null)
    {
        $this->className = $className;
    }

    /**
     * 将条目定义为懒加载。
     *
     * 懒加载条目仅在使用时创建，而注入一个代理。
     *
     * @return $this
     */
    public function lazy(): self
    {
        $this->lazy = true; // 设置懒加载标志为 true

        return $this;
    }

    /**
     * 定义调用构造函数的参数。
     *
     * 此方法接受可变数量的参数，例如：
     *     ->constructor($param1, $param2, $param3)
     *
     * @param mixed ...$parameters 用于调用类构造函数的参数。
     *
     * @return $this
     */
    public function constructor(mixed ...$parameters): self
    {
        $this->constructor = $parameters; // 保存构造函数参数

        return $this;
    }

    /**
     * 定义要注入对象属性的值。
     *
     * @param string $property 要注入值的属性。
     * @param mixed $value 要注入到属性中的值。
     *
     * @return $this
     */
    public function property(string $property, mixed $value): self
    {
        $this->properties[$property] = $value; // 将属性及其值存储到数组中

        return $this;
    }

    /**
     * 定义要调用的方法及其参数。
     *
     * 此方法接受方法名之后的可变数量的参数，例如：
     *
     *     ->method('myMethod', $param1, $param2)
     *
     * 可以多次使用此方法声明多个调用。
     *
     * @param string $method 要调用的方法名称。
     * @param mixed ...$parameters 用于调用方法的参数。
     *
     * @return $this
     */
    public function method(string $method, mixed ...$parameters): self
    {
        // 检查方法名称是否已存在，如果不存在，则初始化为空数组
        if (!isset($this->methods[$method])) {
            $this->methods[$method] = [];
        }

        // 将参数添加到指定方法的调用中
        $this->methods[$method][] = $parameters;

        return $this;
    }

    /**
     * 获取对象定义。
     *
     * @param string $entryName 条目名称。
     * @return ObjectDefinition 返回生成的对象定义。
     *
     * @throws InvalidDefinition
     */
    #[Override] public function getDefinition(string $entryName): ObjectDefinition
    {
        // 获取定义类名常量
        $class = $this::DEFINITION_CLASS;

        // 创建对象定义
        /** @var ObjectDefinition $definition */
        $definition = new $class($entryName, $this->className);

        // 如果设置了懒加载，则将其应用到对象定义中
        if ($this->lazy !== null) {
            $definition->setLazy($this->lazy);
        }

        // 如果构造函数参数数组不为空，则进行设置
        if (!empty($this->constructor)) {
            // 修正构造函数参数，确保索引正确
            $parameters = $this->fixParameters($definition, '__construct', $this->constructor);
            // 创建构造函数注入对象
            $constructorInjection = MethodInjection::constructor($parameters);
            // 设置构造函数注入
            $definition->setConstructorInjection($constructorInjection);
        }

        // 如果属性数组不为空，则进行属性注入
        if (!empty($this->properties)) {
            foreach ($this->properties as $property => $value) {
                // 为每个属性添加属性注入
                $definition->addPropertyInjection(
                    new PropertyInjection($property, $value)
                );
            }
        }

        // 如果方法数组不为空，则进行方法注入
        if (!empty($this->methods)) {
            foreach ($this->methods as $method => $calls) {
                foreach ($calls as $parameters) {
                    // 修正方法参数
                    $parameters = $this->fixParameters($definition, $method, $parameters);
                    // 创建方法注入对象
                    $methodInjection = new MethodInjection($method, $parameters);
                    // 添加方法注入
                    $definition->addMethodInjection($methodInjection);
                }
            }
        }

        // 返回生成的对象定义
        return $definition;
    }

    /**
     * 修正参数，将基于参数名称的索引重新索引为位置索引。
     *
     * 这对于在源之间合并定义是必要的。
     *
     * @param ObjectDefinition $definition 对象定义。
     * @param string $method 方法名。
     * @param array $parameters 参数数组。
     *
     * @return array 修正后的参数数组。
     *
     * @throws InvalidDefinition
     */
    public function fixParameters(ObjectDefinition $definition, string $method, array $parameters): array
    {
        // 创建一个新的数组用于存储修正后的参数
        $fixedParameters = [];

        foreach ($parameters as $index => $parameter) {
            // 如果参数是通过名称索引的，将其索引修正为位置索引
            if (is_string($index)) {
                // 创建可调用数组
                $callable = [$definition->getClassName(), $method];
                try {
                    // 通过反射获取参数信息
                    $reflectionParameter = new ReflectionParameter($callable, $index);
                } catch (\ReflectionException $e) {
                    // 如果参数无法找到，则抛出无效定义异常
                    throw InvalidDefinition::create($definition, sprintf("Parameter with name '%s' could not be found. %s.", $index, $e->getMessage()));
                }

                // 获取参数位置索引
                $index = $reflectionParameter->getPosition();
            }

            // 将参数添加到修正后的参数数组中
            $fixedParameters[$index] = $parameter;
        }

        // 返回修正后的参数数组
        return $fixedParameters;
    }
}
