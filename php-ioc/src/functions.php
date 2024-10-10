<?php

declare(strict_types=1);

namespace Ioc;

use Ioc\Definition\ArrayDefinitionExtension;
use Ioc\Definition\EnvironmentVariableDefinition;
use Ioc\Definition\Helper\AutowireDefinitionHelper;
use Ioc\Definition\Helper\CreateDefinitionHelper;
use Ioc\Definition\Helper\FactoryDefinitionHelper;
use Ioc\Definition\Reference;
use Ioc\Definition\StringDefinition;
use Ioc\Definition\ValueDefinition;

// 检查 Ioc\value 函数是否已定义
if (!function_exists('Ioc\value')) {
    /**
     * 定义一个值的辅助函数。
     *
     * @param mixed $value 要定义的值。
     *
     * @return ValueDefinition 返回一个值定义实例。
     */
    function value(mixed $value): ValueDefinition
    {
        return new ValueDefinition($value);
    }
}

// 检查 Ioc\create 函数是否已定义
if (!function_exists('Ioc\create')) {
    /**
     * 定义一个对象的辅助函数。
     *
     * @param string|null $className 对象的类名。
     *                               如果为 null，则将使用作为容器条目的名称作为类名。
     *
     * @return CreateDefinitionHelper 返回一个创建定义辅助对象。
     */
    function create(?string $className = null): CreateDefinitionHelper
    {
        return new CreateDefinitionHelper($className);
    }
}

// 检查 Ioc\autowire 函数是否已定义
if (!function_exists('Ioc\autowire')) {
    /**
     * 自动注入对象的辅助函数。
     *
     * @param string|null $className 对象的类名。
     *                               如果为 null，则将使用作为容器条目的名称作为类名。
     *
     * @return AutowireDefinitionHelper 返回一个自动注入定义辅助对象。
     */
    function autowire(?string $className = null): AutowireDefinitionHelper
    {
        return new AutowireDefinitionHelper($className);
    }
}

// 检查 Ioc\factory 函数是否已定义
if (!function_exists('Ioc\factory')) {
    /**
     * 定义使用工厂函数/可调用的容器条目的辅助函数。
     *
     * @param callable|array|string $factory 工厂是一个可调用对象，接受容器作为参数并返回要注册到容器中的值。
     *
     * @return FactoryDefinitionHelper 返回一个工厂定义辅助对象。
     */
    function factory(callable|array|string $factory): FactoryDefinitionHelper
    {
        return new FactoryDefinitionHelper($factory);
    }
}

// 检查 Ioc\decorate 函数是否已定义
if (!function_exists('Ioc\decorate')) {
    /**
     * 使用可调用对象装饰先前的定义。
     *
     * 示例:
     *
     *     'foo' => decorate(function ($foo, $container) {
     *         return new CachedFoo($foo, $container->get('cache'));
     *     })
     *
     * @param callable $callable 可调用对象，接受被装饰对象作为第一个参数和容器作为第二个参数。
     *
     * @return FactoryDefinitionHelper 返回一个工厂定义辅助对象，标记为装饰器。
     */
    function decorate(callable|array|string $callable): FactoryDefinitionHelper
    {
        return new FactoryDefinitionHelper($callable, true);
    }
}

// 检查 Ioc\get 函数是否已定义
if (!function_exists('Ioc\get')) {
    /**
     * 参考其他容器条目的辅助函数。
     *
     * @param string $entryName 另一个容器条目的名称。
     *
     * @return Reference 返回一个引用实例。
     */
    function get(string $entryName): Reference
    {
        return new Reference($entryName);
    }
}

// 检查 Ioc\env 函数是否已定义
if (!function_exists('Ioc\env')) {
    /**
     * 参考环境变量的辅助函数。
     *
     * @param string $variableName 环境变量的名称。
     * @param mixed $defaultValue 如果未定义环境变量，则使用的默认值。
     *
     * @return EnvironmentVariableDefinition 返回一个环境变量定义实例。
     */
    function env(string $variableName, mixed $defaultValue = null): EnvironmentVariableDefinition
    {
        // 仅在显式提供默认值时标记为可选
        $isOptional = 2 === func_num_args();

        return new EnvironmentVariableDefinition($variableName, $isOptional, $defaultValue);
    }
}

// 检查 Ioc\add 函数是否已定义
if (!function_exists('Ioc\add')) {
    /**
     * 扩展另一个定义的辅助函数。
     *
     * 示例:
     *
     *     'log.backends' => Ioc\add(Ioc\get('My\Custom\LogBackend'))
     *
     * 或者:
     *
     *     'log.backends' => Ioc\add([
     *         Ioc\get('My\Custom\LogBackend')
     *     ])
     *
     * @param mixed|array $values 要添加到数组的值或值数组。
     *
     * @return ArrayDefinitionExtension 返回一个数组定义扩展实例。
     *
     * @since 5.0
     */
    function add($values): ArrayDefinitionExtension
    {
        if (!is_array($values)) {
            $values = [$values];
        }

        return new ArrayDefinitionExtension($values);
    }
}

// 检查 Ioc\string 函数是否已定义
if (!function_exists('Ioc\string')) {
    /**
     * 连接字符串的辅助函数。
     *
     * 示例:
     *
     *     'log.filename' => Ioc\string('{app.path}/app.log')
     *
     * @param string $expression 字符串表达式。使用 `{}` 占位符来引用其他容器条目。
     *
     * @return StringDefinition 返回一个字符串定义实例。
     *
     * @since 5.0
     */
    function string(string $expression): StringDefinition
    {
        return new StringDefinition($expression);
    }
}
