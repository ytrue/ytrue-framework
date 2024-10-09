<?php

namespace Ioc\Definition;

use Ioc\Definition\Dumper\ObjectDefinitionDumper;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use Ioc\Definition\ObjectDefinition\PropertyInjection;
use Ioc\Definition\Source\DefinitionArray;
use Override;
use ReflectionClass;
use ReflectionException;

class ObjectDefinition implements Definition
{

    /**
     * 定义的名称
     * @var string
     */
    private string $name;

    /**
     * 类名，可为空
     * @var string|null
     */
    protected ?string $className = null;

    /**
     * 构造方法的依赖注入
     * @var MethodInjection|null
     */
    protected ?MethodInjection $constructorInjection = null;

    /**
     * 属性注入的数组，包含多个属性注入对象
     * @var array
     */
    protected array $propertyInjections = [];

    /**
     * 方法注入的数组，包含多个方法注入对象
     * @var array
     */
    protected array $methodInjections = [];

    /**
     * 是否懒加载，null 表示未定义，默认为 false
     * @var bool|null
     */
    protected ?bool $lazy = null;

    /**
     * 缓存类是否存在的状态
     * @var bool
     */
    private bool $classExists;

    /**
     * 缓存类是否可实例化的状态
     * @var bool
     */
    private bool $isInstantiable;

    /**
     * 构造函数，初始化定义的名称和类名
     *
     * @param string $name 定义的名称
     * @param string|null $className 类名（可选）
     * @throws ReflectionException
     */
    public function __construct(string $name, ?string $className = null)
    {
        $this->name = $name;
        $this->setClassName($className);
    }

    /**
     * 获取定义的名称
     *
     * @return string 返回名称
     */
    #[Override] public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置定义的名称
     *
     * @param string $name 新的名称
     */
    #[Override] public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 获取类名，如果未定义则使用名称作为类名
     *
     * @return string|null 返回类名
     */
    public function getClassName(): ?string
    {
        return $this->className ?? $this->name;
    }

    /**
     * 设置类名，并更新缓存状态
     *
     * @param string|null $className 类名
     * @throws ReflectionException
     */
    public function setClassName(?string $className): void
    {
        $this->className = $className;

        // 更新类的缓存状态
        $this->updateCache();
    }

    /**
     * 获取构造函数的依赖注入
     *
     * @return MethodInjection|null 返回构造函数注入对象
     */
    public function getConstructorInjection(): ?MethodInjection
    {
        return $this->constructorInjection;
    }

    /**
     * 设置构造函数的依赖注入
     *
     * @param MethodInjection $constructorInjection 构造函数的依赖注入
     */
    public function setConstructorInjection(MethodInjection $constructorInjection): void
    {
        $this->constructorInjection = $constructorInjection;
    }

    /**
     * 合并或设置构造函数的依赖注入
     *
     * @param MethodInjection $injection 构造函数注入对象
     */
    public function completeConstructorInjection(MethodInjection $injection): void
    {
        if ($this->constructorInjection !== null) {
            // 合并注入
            $this->constructorInjection->merge($injection);
        } else {
            // 设置新的注入
            $this->constructorInjection = $injection;
        }
    }

    /**
     * 获取属性注入的数组
     *
     * @return array 返回属性注入对象数组
     */
    public function getPropertyInjections(): array
    {
        return $this->propertyInjections;
    }

    /**
     * 添加一个属性注入
     *
     * @param PropertyInjection $propertyInjection 属性注入对象
     */
    public function addPropertyInjection(PropertyInjection $propertyInjection): void
    {
        $className = $propertyInjection->getClassName();
        if ($className) {
            // className::propertyName
            $key = $className . '::' . $propertyInjection->getPropertyName();
        } else {
            $key = $propertyInjection->getPropertyName();
        }

        // 将属性注入对象添加到数组
        $this->propertyInjections[$key] = $propertyInjection;
    }

    /**
     * 获取方法注入的数组
     *
     * @return array 返回方法注入对象数组
     */
    public function getMethodInjections(): array
    {
        $injections = [];
        array_walk_recursive($this->methodInjections, function ($injection) use (&$injections) {
            $injections[] = $injection;
        });

        return $injections;
    }

    /**
     * 添加一个方法注入
     *
     * @param MethodInjection $methodInjection 方法注入对象
     */
    public function addMethodInjection(MethodInjection $methodInjection): void
    {
        $method = $methodInjection->getMethodName();
        if (!isset($this->methodInjections[$method])) {
            $this->methodInjections[$method] = [];
        }

        // 将方法注入对象添加到数组
        $this->methodInjections[$method][] = $methodInjection;
    }

    /**
     * 合并或设置第一个方法注入
     *
     * @param MethodInjection $injection 方法注入对象
     */
    public function completeFirstMethodInjection(MethodInjection $injection): void
    {
        $method = $injection->getMethodName();

        if (isset($this->methodInjections[$method][0])) {
            // 合并注入
            $this->methodInjections[$method][0]->merge($injection);
        } else {
            // 设置新的注入
            $this->addMethodInjection($injection);
        }
    }

    /**
     * 设置是否为懒加载
     *
     * @param bool|null $lazy 懒加载状态
     */
    public function setLazy(?bool $lazy = null): void
    {
        $this->lazy = $lazy;
    }

    /**
     * 获取懒加载状态
     *
     * @return bool 返回懒加载状态
     */
    public function isLazy(): bool
    {
        if ($this->lazy !== null) {
            return $this->lazy;
        }

        // 默认为非懒加载
        return false;
    }

    /**
     * 检查类是否存在
     *
     * @return bool 返回类是否存在
     */
    public function classExists(): bool
    {
        return $this->classExists;
    }

    /**
     * 检查类是否可实例化
     *
     * @return bool 返回类是否可实例化
     */
    public function isInstantiable(): bool
    {
        return $this->isInstantiable;
    }

    /**
     * 替换嵌套的定义对象
     *
     * 该方法接受一个回调函数作为参数，并使用该函数遍历和替换当前对象中的所有嵌套定义。
     * 它会遍历属性注入、构造函数注入和方法注入中的定义，并调用提供的回调函数进行替换。
     *
     * @param callable $replacer 替换回调函数，该函数接受一个定义对象并返回替换后的定义对象
     */
    #[Override] public function replaceNestedDefinitions(callable $replacer): void
    {
        // 遍历属性注入数组，并对每个属性注入调用替换函数
        array_walk($this->propertyInjections, function (PropertyInjection $propertyInjection) use ($replacer) {
            $propertyInjection->replaceNestedDefinition($replacer);
        });

        // 对构造函数注入调用替换函数（如果存在）
        $this->constructorInjection?->replaceNestedDefinitions($replacer);

        // 遍历方法注入数组，对每个方法注入调用替换函数
        array_walk($this->methodInjections, function ($injectionArray) use ($replacer) {
            array_walk($injectionArray, function (MethodInjection $methodInjection) use ($replacer) {
                $methodInjection->replaceNestedDefinitions($replacer);
            });
        });
    }

    /**
     * 该函数适用于处理类名模板中包含通配符的情况，比如你可能有一个包含通配符的类名（如 MyClass_*_Service），
     * 通过传入不同的替换值（如 User、Product），
     * 将通配符替换为实际的类名（如 MyClass_User_Service 或 MyClass_Product_Service）
     * @throws ReflectionException
     */
    public function replaceWildcards(array $replacements): void
    {
        // 获取当前的类名
        $className = $this->getClassName();

        // 遍历替换值数组
        foreach ($replacements as $replacement) {
            // 查找类名中通配符的位置，假设通配符是 DefinitionArray::WILDCARD
            $pos = strpos($className, DefinitionArray::WILDCARD);

            // 如果找到通配符的位置，则进行替换
            if ($pos !== false) {
                // 使用 replacement 替换 className 中的通配符，只替换第一个通配符
                $className = substr_replace($className, $replacement, $pos, 1);


            }
        }

        // 将替换后的类名设置回去
        $this->setClassName($className);
    }

    /**
     * 获取定义的字符串表示
     *
     * @return string 定义的字符串表示
     * @throws ReflectionException
     */
    #[Override] public function __toString(): string
    {
        return (new ObjectDefinitionDumper())->dump($this);
    }

    /**
     * 更新类的缓存状态
     *
     * 该方法用于检查当前定义的类名是否存在，并判断其是否可以实例化。
     * 它首先获取类名，然后通过 PHP 的内置函数检查该类或接口是否存在。
     * 如果类不存在，则将可实例化状态设置为 false，并提前返回。
     * 如果类存在，方法将创建一个 `ReflectionClass` 实例，并通过该实例的 `isInstantiable` 方法
     * 检查该类是否可实例化，并将结果缓存到属性中。
     * @throws ReflectionException
     */
    private function updateCache(): void
    {
        // 获取当前的类名
        $className = $this->getClassName();

        // 检查类或接口是否存在
        $this->classExists = class_exists($className) || interface_exists($className);

        // 如果类不存在，则设置为不可实例化
        if (!$this->classExists) {
            $this->isInstantiable = false;
            return;
        }

        // 创建 ReflectionClass 实例
        $class = new ReflectionClass($className);
        // 检查类是否可实例化
        $this->isInstantiable = $class->isInstantiable();
    }

}
