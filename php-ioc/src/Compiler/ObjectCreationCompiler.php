<?php

namespace Ioc\Compiler;

use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use ReflectionClass;
use ReflectionException;
use ReflectionMethod;
use ReflectionParameter;
use ReflectionProperty;
use const PHP_VERSION_ID;

/**
 * ObjectCreationCompiler 类用于编译对象创建的逻辑
 *
 * 此类负责从 ObjectDefinition 定义中生成对象创建的 PHP 代码，包括构造函数、属性注入和方法注入的处理。
 */
readonly class ObjectCreationCompiler
{
    /**
     * 构造函数
     *
     * @param Compiler $compiler 编译器实例，用于生成依赖项和代理类。
     */
    public function __construct(
        private Compiler $compiler,
    ) {
    }

    /**
     * 编译对象定义，生成 PHP 代码以创建对象
     *
     * @param ObjectDefinition $definition 对象的定义信息，包括构造函数、属性和方法注入等。
     * @return string 生成的 PHP 代码
     * @throws InvalidDefinition|ReflectionException 如果定义无效则抛出异常。
     */
    public function compile(ObjectDefinition $definition) : string
    {
        // 检查类是否为匿名类
        $this->assertClassIsNotAnonymous($definition);
        // 检查类是否可实例化
        $this->assertClassIsInstantiable($definition);
        /** @var class-string $className 此时已经检查类是有效的 */
        $className = $definition->getClassName();

        // 判断是否是懒加载
        if ($definition->isLazy()) {
            return $this->compileLazyDefinition($definition);
        }

        try {
            // 获取类的反射信息
            $classReflection = new ReflectionClass($className);
            // 解析构造函数的参数
            $constructorArguments = $this->resolveParameters($definition->getConstructorInjection(), $classReflection->getConstructor());

            // 编译构造函数的参数值
            $dumpedConstructorArguments = array_map(function ($value) {
                return $this->compiler->compileValue($value);
            }, $constructorArguments);

            $code = [];
            // 生成对象的构造代码
            $code[] = sprintf(
                '$object = new %s(%s);',
                $className,
                implode(', ', $dumpedConstructorArguments)
            );

            // 属性注入
            foreach ($definition->getPropertyInjections() as $propertyInjection) {
                $value = $propertyInjection->getValue();
                $value = $this->compiler->compileValue($value);

                // 获取属性的类名
                $propertyClassName = $propertyInjection->getClassName() ?: $className;
                $property = new ReflectionProperty($propertyClassName, $propertyInjection->getPropertyName());

                // 判断属性是否为公有且不是只读属性
                if ($property->isPublic() && !(PHP_VERSION_ID >= 80100 && $property->isReadOnly())) {
                    $code[] = sprintf('$object->%s = %s;', $propertyInjection->getPropertyName(), $value);
                } else {
                    // 处理私有、受保护或只读属性
                    $code[] = sprintf(
                        '\Ioc\Definition\Resolver\ObjectCreator::setPrivatePropertyValue(%s, $object, \'%s\', %s);',
                        var_export($propertyInjection->getClassName(), true),
                        $propertyInjection->getPropertyName(),
                        $value
                    );
                }
            }

            // 方法注入
            foreach ($definition->getMethodInjections() as $methodInjection) {
                // 获取方法反射信息
                $methodReflection = new ReflectionMethod($className, $methodInjection->getMethodName());
                // 解析方法的参数
                $parameters = $this->resolveParameters($methodInjection, $methodReflection);

                // 编译方法的参数值
                $dumpedParameters = array_map(function ($value) {
                    return $this->compiler->compileValue($value);
                }, $parameters);

                // 生成方法调用代码
                $code[] = sprintf(
                    '$object->%s(%s);',
                    $methodInjection->getMethodName(),
                    implode(', ', $dumpedParameters)
                );
            }
        } catch (InvalidDefinition $e) {
            // 捕获无效定义的异常并抛出更详细的错误信息
            throw InvalidDefinition::create($definition, sprintf(
                '依赖项 "%s" 无法编译: %s',
                $definition->getName(),
                $e->getMessage()
            ));
        }

        // 返回生成的 PHP 代码
        return implode("\n        ", $code);
    }

    /**
     * 解析方法或构造函数的参数
     *
     * @param ?MethodInjection $definition 方法注入的定义
     * @param ?ReflectionMethod $method 反射方法对象
     * @return array 解析后的参数列表
     * @throws InvalidDefinition 如果参数无法解析，则抛出异常。
     */
    public function resolveParameters(?MethodInjection $definition, ?ReflectionMethod $method) : array
    {
        $args = [];

        if (! $method) {
            return $args;
        }

        // 获取定义中的参数
        $definitionParameters = $definition ? $definition->getParameters() : [];

        // 遍历方法的参数
        foreach ($method->getParameters() as $index => $parameter) {
            if (array_key_exists($index, $definitionParameters)) {
                // 如果定义中有参数值，则使用它
                $value = &$definitionParameters[$index];
            } elseif ($parameter->isOptional()) {
                // 如果参数是可选的并且没有定义值，则使用其默认值
                $args[] = $this->getParameterDefaultValue($parameter, $method);
                continue;
            } else {
                // 参数无法解析，抛出异常
                throw new InvalidDefinition(sprintf(
                    '参数 $%s 在 %s 中没有定义或无法推测的值',
                    $parameter->getName(),
                    $this->getFunctionName($method)
                ));
            }

            $args[] = &$value;
        }

        return $args;
    }

    /**
     * 编译懒加载定义，生成代理类的代码
     *
     * @param ObjectDefinition $definition 对象的定义
     * @return string 生成的代理类 PHP 代码
     * @throws InvalidDefinition
     */
    private function compileLazyDefinition(ObjectDefinition $definition) : string
    {
        // 克隆定义，并禁用懒加载
        $subDefinition = clone $definition;
        $subDefinition->setLazy(false);
        $subDefinition = $this->compiler->compileValue($subDefinition);

        /** @var class-string $className 此时已经检查类是有效的 */
        $className = $definition->getClassName();

        // 生成代理类
        $this->compiler->getProxyFactory()->generateProxyClass($className);

        // 返回代理类的 PHP 代码
        return <<<STR
                    \$object = \$this->proxyFactory->createProxy(
                        '{$definition->getClassName()}',
                        function (&\$wrappedObject, \$proxy, \$method, \$params, &\$initializer) {
                            \$wrappedObject = $subDefinition;
                            \$initializer = null; // 关闭进一步的懒加载初始化
                            return true;
                        }
                    );
            STR;
    }

    /**
     * 获取方法参数的默认值
     *
     * @param ReflectionParameter $parameter 参数的反射对象
     * @param ReflectionMethod $function 函数或方法的反射对象
     * @return mixed 参数的默认值
     * @throws InvalidDefinition 如果无法读取默认值，则抛出异常
     */
    private function getParameterDefaultValue(ReflectionParameter $parameter, ReflectionMethod $function) : mixed
    {
        try {
            // 返回参数的默认值
            return $parameter->getDefaultValue();
        } catch (ReflectionException) {
            // 如果无法通过反射读取默认值，抛出异常
            throw new InvalidDefinition(sprintf(
                '参数 "%s" 在 %s 中没有定义或无法推测的类型。虽然它有默认值，但无法通过反射读取，因为它是 PHP 内部类。',
                $parameter->getName(),
                $this->getFunctionName($function)
            ));
        }
    }

    /**
     * 获取方法的名称
     *
     * @param ReflectionMethod $method 方法的反射对象
     * @return string 方法的名称
     */
    private function getFunctionName(ReflectionMethod $method) : string
    {
        return $method->getName() . '()';
    }

    /**
     * 检查类是否为匿名类
     *
     * @param ObjectDefinition $definition 对象的定义
     * @throws InvalidDefinition 如果类是匿名类，则抛出异常。
     */
    private function assertClassIsNotAnonymous(ObjectDefinition $definition) : void
    {
        if (str_contains($definition->getClassName(), '@')) {
            throw InvalidDefinition::create($definition, sprintf(
                '依赖项 "%s" 无法编译: 匿名类不能被编译',
                $definition->getName()
            ));
        }
    }

    /**
     * 检查类是否可实例化
     *
     * @param ObjectDefinition $definition 对象的定义
     * @throws InvalidDefinition 如果类不可实例化或类不存在，则抛出异常。
     */
    private function assertClassIsInstantiable(ObjectDefinition $definition) : void
    {
        if ($definition->isInstantiable()) {
            return;
        }

        // 如果类不存在或不可实例化，生成对应的错误信息
        $message = ! $definition->classExists()
            ? '依赖项 "%s" 无法编译: 类不存在'
            : '依赖项 "%s" 无法编译: 类不可实例化';

        throw InvalidDefinition::create($definition, sprintf($message, $definition->getName()));
    }
}

