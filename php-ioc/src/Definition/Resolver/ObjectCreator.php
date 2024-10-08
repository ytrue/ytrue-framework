<?php

namespace Ioc\Definition\Resolver;

use Exception;
use RuntimeException;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\PropertyInjection;
use Ioc\DependencyException;
use Ioc\Proxy\ProxyFactory;
use Override;
use Psr\Container\NotFoundExceptionInterface;
use ReflectionClass;
use ReflectionException;
use ReflectionMethod;
use ReflectionProperty;
use const PHP_VERSION_ID;

/**
 * ObjectCreator 类用于创建对象实例和处理属性、方法注入。
 * 它实现了 DefinitionResolver 接口，用于解析和创建依赖注入容器中的对象。
 */
readonly class ObjectCreator implements DefinitionResolver
{

    // 用于解析方法参数的参数解析器
    private ParameterResolver $parameterResolver;

    /**
     * 构造函数
     *
     * @param DefinitionResolver $definitionResolver 依赖定义解析器，用于解析依赖项
     * @param ProxyFactory $proxyFactory 代理工厂，用于生成懒加载对象的代理
     */
    public function __construct(
        private DefinitionResolver $definitionResolver,
        private ProxyFactory       $proxyFactory
    )
    {
        // 初始化参数解析器，用于解析方法的参数
        $this->parameterResolver = new ParameterResolver($definitionResolver);
    }

    /**
     * resolve 方法根据给定的定义解析并创建对象实例。如果定义是懒加载的，则会创建代理对象。
     *
     * @param ObjectDefinition $definition 对象定义，包含如何创建对象的信息
     * @param array $parameters 参数数组，调用构造函数时所需的参数
     * @return mixed 返回创建的对象实例或其代理
     * @throws DependencyException
     * @throws InvalidDefinition
     * @throws ReflectionException
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {
        // 如果定义是懒加载的，则创建代理对象
        if ($definition->isLazy()) {
            throw new RuntimeException("暂未实现....");
            //  return $this->createProxy($definition, $parameters);
        }

        // 否则创建普通的对象实例
        return $this->createInstance($definition, $parameters);
    }


    /**
     * 判断对象定义是否可解析，即判断该对象是否可实例化。
     *
     * @param ObjectDefinition $definition 对象定义
     * @param array $parameters 参数数组
     * @return bool 如果定义可实例化则返回 true，否则返回 false
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        return $definition->isInstantiable();
    }

    /**
     * 创建对象实例，根据定义的类名及构造函数参数进行实例化。
     *
     * @param ObjectDefinition $definition 对象定义
     * @param array $parameters 参数数组，传递给构造函数
     * @return object
     * @throws InvalidDefinition 如果类不可实例化或不存在
     * @throws ReflectionException
     * @throws DependencyException
     */
    private function createInstance(Definition $definition, array $parameters): object
    {
        // 检查类是否可实例化
        if (!$definition->isInstantiable()) {
            // 如果类不存在，抛出异常
            if (!$definition->classExists()) {
                throw InvalidDefinition::create($definition, sprintf(
                    'Entry "%s" cannot be resolved: the class doesn\'t exist',
                    $definition->getName()
                ));
            }

            // 如果类存在但不可实例化，抛出异常
            throw InvalidDefinition::create($definition, sprintf(
                'Entry "%s" cannot be resolved: the class is not instantiable',
                $definition->getName()
            ));
        }

        // 获取类名并使用反射获取类的构造方法
        $classname = $definition->getClassName();
        $classReflection = new ReflectionClass($classname);

        // 获取构造方法的注入定义
        $constructorInjection = $definition->getConstructorInjection();

        try {
            // 使用参数解析器解析构造函数参数
            $args = $this->parameterResolver->resolveParameters(
                $constructorInjection,
                $classReflection->getConstructor(),
                $parameters
            );



            // 使用解析后的参数创建类的实例
            $object = new $classname(...$args);

            // 注入属性和方法
            $this->injectMethodsAndProperties($object, $definition);
        } catch (NotFoundExceptionInterface $e) {
            throw new DependencyException(sprintf(
                'Error while injecting dependencies into %s: %s',
                $classReflection->getName(),
                $e->getMessage()
            ), 0, $e);
        } catch (InvalidDefinition $e) {
            throw InvalidDefinition::create($definition, sprintf(
                'Entry "%s" cannot be resolved: %s',
                $definition->getName(),
                $e->getMessage()
            ));
        }

        return $object;
    }

    /**
     * 注入对象的属性和方法。根据定义，依赖项会通过反射注入到对象的私有或保护属性和方法中。
     *
     * @param object $object 需要注入的对象实例
     * @param ObjectDefinition $objectDefinition 对象定义
     * @return void
     * @throws ReflectionException 如果反射失败
     * @throws InvalidDefinition|DependencyException 如果注入过程出错
     */
    protected function injectMethodsAndProperties(object $object, ObjectDefinition $objectDefinition): void
    {
        // 属性注入
        foreach ($objectDefinition->getPropertyInjections() as $propertyInjection) {
            $this->injectProperty($object, $propertyInjection);
        }

        // 方法注入
        foreach ($objectDefinition->getMethodInjections() as $methodInjection) {

            $methodReflection = new ReflectionMethod($object, $methodInjection->getMethodName());
            $args = $this->parameterResolver->resolveParameters($methodInjection, $methodReflection);

            // 通过反射调用注入的方法
            $methodReflection->invokeArgs($object, $args);
        }
    }

    /**
     * 注入对象的属性值。使用反射将依赖注入到私有或受保护的属性中。
     *
     * @param object $object 需要注入属性的对象实例
     * @param PropertyInjection $propertyInjection 属性注入定义，包含属性名和注入值
     * @return void
     * @throws DependencyException
     * @throws ReflectionException
     */
    private function injectProperty(object $object, PropertyInjection $propertyInjection): void
    {
        $propertyName = $propertyInjection->getPropertyName();
        $value = $propertyInjection->getValue();

        // 如果属性值是一个定义对象，则需要解析该定义
        if ($value instanceof Definition) {
            try {
                $value = $this->definitionResolver->resolve($value);
            } catch (DependencyException $e) {
                throw $e;
            } catch (Exception $e) {
                throw new DependencyException(sprintf(
                    'Error while injecting in %s::%s. %s',
                    $object::class,
                    $propertyName,
                    $e->getMessage()
                ), 0, $e);
            }
        }

        // 通过反射设置属性值，支持私有和受保护的属性
        self::setPrivatePropertyValue($propertyInjection->getClassName(), $object, $propertyName, $value);
    }

    /**
     * 使用反射设置私有或受保护属性的值。
     *
     * @param string|null $className 类名，如果为空则使用对象的类名
     * @param object $object 需要注入属性的对象实例
     * @param string $propertyName 属性名
     * @param mixed $propertyValue 属性值
     * @return void
     * @throws ReflectionException
     */
    public static function setPrivatePropertyValue(?string $className, object $object, string $propertyName, mixed $propertyValue): void
    {
        // 如果未指定类名，则使用对象的类名
        $className = $className ?: $object::class;

        // 通过反射获取属性
        $property = new ReflectionProperty($className, $propertyName);
        if (!$property->isPublic() && PHP_VERSION_ID < 80100) {
            // 设置为可访问
            $property->setAccessible(true);
        }
        // 设置属性值
        $property->setValue($object, $propertyValue);
    }
}
