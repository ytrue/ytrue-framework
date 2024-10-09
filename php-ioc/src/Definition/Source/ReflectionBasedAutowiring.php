<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\Definition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use Ioc\Definition\Reference;
use Override;
use ReflectionClass;
use ReflectionException;
use ReflectionFunctionAbstract;
use ReflectionNamedType;

/**
 * Class ReflectionBasedAutowiring
 *
 * 该类实现了自动装配功能，使用反射机制根据类或接口的定义来自动注入依赖。
 * 它根据类的构造函数参数的类型自动生成依赖关系并将其注入到对象定义中。
 */
class ReflectionBasedAutowiring implements DefinitionSource, Autowiring
{
    /**
     * 自动装配给定的定义。
     *
     * @param string $name 要自动装配的类名或接口名。
     * @param ObjectDefinition|null $definition 可选的对象定义。如果不提供，将根据名称创建一个新的对象定义。
     * @return ObjectDefinition|null 返回填充了构造函数注入的对象定义，或者如果类或接口不存在，则返回 null。
     * @throws ReflectionException 如果反射过程中出现异常。
     */
    #[Override] public function autowire(string $name, ?ObjectDefinition $definition = null): ObjectDefinition|null
    {
        // 如果提供了对象定义，则获取类名，否则使用名称作为类名
        $className = $definition ? $definition->getClassName() : $name;

        // 检查类或接口是否存在
        if (!class_exists($className) && !interface_exists($className)) {
            return $definition; // 返回 null，因为类或接口不存在
        }

        // 如果没有提供对象定义，则创建一个新的对象定义
        $definition = $definition ?: new ObjectDefinition($name);

        // 使用反射获取类的构造函数
        $class = new ReflectionClass($className);
        $constructor = $class->getConstructor();

        // 如果构造函数存在且是公共的
        if ($constructor && $constructor->isPublic()) {
            // 获取构造函数的参数定义，并创建 MethodInjection 对象
            $constructorInjection = MethodInjection::constructor($this->getParametersDefinition($constructor));
            // 完成对象定义的构造函数注入
            $definition->completeConstructorInjection($constructorInjection);
        }

        return $definition; // 返回填充后的对象定义
    }

    /**
     * 获取构造函数参数的定义。
     *
     * @param ReflectionFunctionAbstract $constructor 构造函数的反射对象。
     * @return array 返回构造函数参数的定义，包含参数索引和相应的 Reference 实例。
     */
    private function getParametersDefinition(ReflectionFunctionAbstract $constructor): array
    {
        // 定义一个空数组，用于存储参数定义
        $parameters = [];

        // 遍历构造函数的参数
        foreach ($constructor->getParameters() as $index => $parameter) {
            // 如果参数是可选的，则跳过此参数
            if ($parameter->isOptional()) {
                continue;
            }

            // 获取参数的类型
            $parameterType = $parameter->getType();

            // 如果参数没有类型，则跳过此参数
            if (!$parameterType) {
                continue;
            }

            // 如果参数类型不是 ReflectionNamedType 的实例，则跳过此参数
            if (!$parameterType instanceof ReflectionNamedType) {
                continue;
            }

            // 如果参数类型是内置类型（例如 int、string 等），则跳过此参数
            if ($parameterType->isBuiltin()) {
                continue;
            }

            // 将参数类型的名称作为 Reference 实例存储到参数数组中
            $parameters[$index] = new Reference($parameterType->getName());
        }

        return $parameters; // 返回参数定义数组
    }

    /**
     * 根据名称获取定义。
     *
     * @param string $name 要获取的定义名称。
     * @return Definition|null 返回对应的定义，或者如果没有找到，则返回 null。
     * @throws ReflectionException 如果反射过程中出现异常。
     */
    #[Override] public function getDefinition(string $name): ?Definition
    {
        return $this->autowire($name); // 调用 autowire 方法获取定义
    }

    /**
     * 获取所有的定义。
     *
     * @return array 返回一个空数组，因为当前实现不提供定义列表。
     */
    #[Override] public function getDefinitions(): array
    {
        return []; // 返回空数组
    }
}
