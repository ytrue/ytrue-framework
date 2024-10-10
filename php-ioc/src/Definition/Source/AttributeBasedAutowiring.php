<?php

namespace Ioc\Definition\Source;

use Ioc\Attribute\Inject;
use Ioc\Attribute\Injectable;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidAttribute;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use Ioc\Definition\ObjectDefinition\PropertyInjection;
use Ioc\Definition\Reference;
use Override;
use ReflectionClass;
use ReflectionException;
use ReflectionMethod;
use ReflectionNamedType;
use ReflectionParameter;
use ReflectionProperty;
use Throwable;

class AttributeBasedAutowiring implements Autowiring, DefinitionSource
{
    /**
     * @throws InvalidAttribute
     * @throws ReflectionException
     */
    #[Override]
    public function autowire(string $name, ?ObjectDefinition $definition = null): ObjectDefinition|null
    {
        // 根据提供的名称或定义获取类名
        $className = $definition ? $definition->getClassName() : $name;

        // 检查类或接口是否存在
        if (!class_exists($className) && !interface_exists($className)) {
            return $definition;
        }

        // 如果没有提供定义，则创建新的 ObjectDefinition 实例
        $definition = $definition ?: new ObjectDefinition($name);

        // 使用反射获取类信息
        $class = new ReflectionClass($className);

        // 读取可注入的属性
        $this->readInjectableAttribute($class, $definition);

        // 遍历类的属性，查找注解
        $this->readProperties($class, $definition);

        // 遍历类的方法，查找注解
        $this->readMethods($class, $definition);

        return $definition;
    }

    /**
     * @throws ReflectionException
     * @throws InvalidAttribute
     */
    #[Override]
    public function getDefinition(string $name): Definition|null
    {
        // 获取定义，主要调用 autowire 方法
        return $this->autowire($name);
    }

    #[Override]
    public function getDefinitions(): array
    {
        // 返回空数组，可能需要实现其他逻辑
        return [];
    }

    /**
     * 读取 #[Injectable] 属性并设置相关的定义
     *
     * @param ReflectionClass $class 反射类实例
     * @param ObjectDefinition $definition 对象定义实例
     *
     * @throws InvalidAttribute 如果读取属性时发生错误
     */
    private function readInjectableAttribute(ReflectionClass $class, ObjectDefinition $definition): void
    {
        try {
            // 获取 #[Injectable] 属性
            $attribute = $class->getAttributes(Injectable::class)[0] ?? null;
            // 如果没有注解则返回
            if (!$attribute) {
                return;
            }
            // 实例化注解
            /** @var Injectable $attributeInstance */
            $attributeInstance = $attribute->newInstance();

        } catch (Throwable $e) {
            // 处理读取注解时发生的错误
            throw new InvalidAttribute(sprintf(
                'Error while reading #[Injectable] on %s: %s',
                $class->getName(),
                $e->getMessage()
            ), 0, $e);
        }

        // 设置懒加载属性
        if ($attributeInstance->isLazy() !== null) {
            $definition->setLazy($attributeInstance->isLazy());
        }
    }

    /**
     * 读取类的属性并查找注解
     *
     * @param ReflectionClass $class 反射类实例
     * @param ObjectDefinition $definition 对象定义实例
     * @throws InvalidAttribute
     */
    private function readProperties(ReflectionClass $class, ObjectDefinition $definition): void
    {
        // 遍历类的所有属性
        foreach ($class->getProperties() as $property) {
            $this->readProperty($property, $definition);
        }

        // 读取父类的私有属性
        while ($class = $class->getParentClass()) {
            foreach ($class->getProperties(ReflectionProperty::IS_PRIVATE) as $property) {
                $this->readProperty($property, $definition, $class->getName());
            }
        }
    }

    /**
     * 读取单个属性并处理注解
     *
     * @param ReflectionProperty $property 反射属性实例
     * @param ObjectDefinition $definition 对象定义实例
     * @param string|null $classname 类名（如果存在）
     *
     * @throws InvalidAttribute 如果读取属性注解时发生错误
     */
    private function readProperty(ReflectionProperty $property, ObjectDefinition $definition, ?string $classname = null): void
    {
        // 检查属性是否为静态或通过构造函数提升的属性
        if ($property->isStatic() || $property->isPromoted()) {
            return;
        }

        // 查找 #[Inject] 属性
        try {
            $attribute = $property->getAttributes(Inject::class)[0] ?? null;
            if (!$attribute) {
                return;
            }
            /** @var Inject $inject */
            $inject = $attribute->newInstance();
        } catch (Throwable $e) {
            // 处理读取属性注解时的错误
            throw new InvalidAttribute(sprintf(
                '#[Inject] annotation on property %s::%s is malformed. %s',
                $property->getDeclaringClass()->getName(),
                $property->getName(),
                $e->getMessage()
            ), 0, $e);
        }

        // 尝试使用 #[Inject("name")] 或者查看属性类型
        $entryName = $inject->getName();

        // 使用类型属性
        $propertyType = $property->getType();
        if ($entryName === null && $propertyType instanceof ReflectionNamedType) {
            // 检查类型是否是有效的类或接口名
            if (!class_exists($propertyType->getName()) && !interface_exists($propertyType->getName())) {
                throw new InvalidAttribute(sprintf(
                    '#[Inject] found on property %s::%s but unable to guess what to inject, the type of the property does not look like a valid class or interface name',
                    $property->getDeclaringClass()->getName(),
                    $property->getName()
                ));
            }
            // 使用类型名称作为 entryName
            $entryName = $propertyType->getName();
        }

        // 如果无法确定 entryName，抛出异常
        if ($entryName === null) {
            throw new InvalidAttribute(sprintf(
                '#[Inject] found on property %s::%s but unable to guess what to inject, please add a type to the property',
                $property->getDeclaringClass()->getName(),
                $property->getName()
            ));
        }

        // 将属性注入添加到定义中
        $definition->addPropertyInjection(
            new PropertyInjection($property->getName(), new Reference($entryName), $classname)
        );
    }

    /**
     * 读取类的方法并查找注解
     *
     * @param ReflectionClass $class 反射类实例
     * @param ObjectDefinition $objectDefinition 对象定义实例
     */
    private function readMethods(ReflectionClass $class, ObjectDefinition $objectDefinition): void
    {
        // 遍历所有公共方法，包括父类的方法
        foreach ($class->getMethods(ReflectionMethod::IS_PUBLIC) as $method) {
            // 跳过静态方法
            if ($method->isStatic()) {
                continue;
            }

            // 获取方法注入信息
            $methodInjection = $this->getMethodInjection($method);

            if (!$methodInjection) {
                continue;
            }

            // 如果是构造函数，则完成构造方法的注入
            if ($method->isConstructor()) {
                $objectDefinition->completeConstructorInjection($methodInjection);
            } else {
                // 否则完成第一个方法的注入
                $objectDefinition->completeFirstMethodInjection($methodInjection);
            }
        }
    }

    /**
     * 获取方法的注入信息
     *
     * @param ReflectionMethod $method 反射方法实例
     * @return MethodInjection|null 方法注入信息，若无则返回 null
     */
    private function getMethodInjection(ReflectionMethod $method): ?MethodInjection
    {
        // 查找 #[Inject] 属性
        $attribute = $method->getAttributes(Inject::class)[0] ?? null;

        if ($attribute) {
            /** @var Inject $inject */
            $inject = $attribute->newInstance();
            $annotationParameters = $inject->getParameters();
        } elseif ($method->isConstructor()) {
            // 构造函数的 #[Inject] 是隐式的，继续处理
            $annotationParameters = [];
        } else {
            return null;
        }

        // 初始化参数数组
        $parameters = [];
        foreach ($method->getParameters() as $index => $parameter) {
            // 获取参数的 entryName
            $entryName = $this->getMethodParameter($index, $parameter, $annotationParameters);

            if ($entryName !== null) {
                $parameters[$index] = new Reference($entryName);
            }
        }

        // 如果是构造函数，返回构造方法的注入信息
        if ($method->isConstructor()) {
            return MethodInjection::constructor($parameters);
        }

        // 返回其他方法的注入信息
        return new MethodInjection($method->getName(), $parameters);
    }

    /**
     * 获取方法参数的注入名称
     *
     * @param int $parameterIndex 参数索引
     * @param ReflectionParameter $parameter 反射参数实例
     * @param array $annotationParameters 注解参数
     * @return string|null 返回参数的 entryName，如果没有则返回 null
     */
    private function getMethodParameter(int $parameterIndex, ReflectionParameter $parameter, array $annotationParameters): ?string
    {
        // 检查参数是否有 #[Inject] 属性
        $attribute = $parameter->getAttributes(Inject::class)[0] ?? null;
        if ($attribute) {
            /** @var Inject $inject */
            $inject = $attribute->newInstance();

            return $inject->getName();
        }

        // 检查注解参数
        if (isset($annotationParameters[$parameterIndex])) {
            return $annotationParameters[$parameterIndex];
        }
        if (isset($annotationParameters[$parameter->getName()])) {
            return $annotationParameters[$parameter->getName()];
        }

        // 如果参数是可选的且没有明确指定，则跳过
        if ($parameter->isOptional()) {
            return null;
        }

        // 检查参数类型
        $parameterType = $parameter->getType();
        if ($parameterType instanceof ReflectionNamedType && !$parameterType->isBuiltin()) {
            return $parameterType->getName();
        }

        return null;
    }
}
