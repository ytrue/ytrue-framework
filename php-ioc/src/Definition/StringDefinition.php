<?php

namespace Ioc\Definition;

use Ioc\DependencyException;
use Psr\Container\ContainerExceptionInterface;
use Psr\Container\ContainerInterface;
use Psr\Container\NotFoundExceptionInterface;

/**
 * 字符串定义类
 *
 * 此类表示一个字符串定义，可以在解析时替换其中的占位符。字符串定义允许
 * 在容器中使用表达式来生成实际值。
 */
class StringDefinition implements Definition, SelfResolvingDefinition
{
    private string $name = '';

    /**
     * @param string $expression 表达式，可能包含占位符
     */
    public function __construct(private string $expression)
    {
    }

    /**
     * 获取定义名称
     *
     * @return string 返回定义名称
     */
    public function getName(): string
    {
        return $this->name;
    }

    /**
     * 设置定义名称
     *
     * @param string $name 定义名称
     */
    public function setName(string $name): void
    {
        $this->name = $name;
    }

    /**
     * 获取字符串表达式
     *
     * @return string 返回字符串表达式
     */
    public function getExpression(): string
    {
        return $this->expression;
    }

    /**
     * 替换嵌套定义
     *
     * 该方法未实现任何操作，保持为空。
     *
     * @param callable $replacer 用于替换嵌套定义的回调函数
     */
    public function replaceNestedDefinitions(callable $replacer): void
    {
    }

    /**
     * 将字符串表示形式返回
     *
     * @return string 返回字符串表达式
     */
    public function __toString(): string
    {
        return $this->expression;
    }

    /**
     * 解析当前字符串表达式
     *
     * @param ContainerInterface $container 容器实例
     * @return mixed 返回解析后的表达式结果
     *
     * @throws ContainerExceptionInterface
     * @throws NotFoundExceptionInterface
     */
    public function resolve(ContainerInterface $container): mixed
    {
        return self::resolveExpression($this->name, $this->expression, $container);
    }

    /**
     * 检查当前定义是否可解析
     *
     * @param ContainerInterface $container 容器实例
     * @return bool 返回 true，表示该定义始终可解析
     */
    public function isResolvable(ContainerInterface $container): bool
    {
        return true;
    }

    /**
     *
     *
     *
     * 解析字符串表达式中的占位符
     *
     * 该方法通过查找表达式中的占位符（以花括号包围的部分），
     * 并用容器中相应条目的值替换它们，生成最终的字符串。
     *
     * @param string $entryName 定义的名称，用于在异常消息中标识
     * @param string $expression 字符串表达式，可能包含占位符
     * @param ContainerInterface $container 容器实例，用于解析占位符
     * @return string 返回解析后的字符串，所有占位符均已替换
     *
     * @throws ContainerExceptionInterface 如果在解析过程中出现容器异常
     * @throws DependencyException 如果占位符对应的条目在容器中未找到
     * @throws \RuntimeException 当解析过程中出现未知错误时抛出
     */
    public static function resolveExpression(
        string             $entryName,
        string             $expression,
        ContainerInterface $container
    ): string
    {
        // 定义一个回调函数，用于替换占位符
        $callback = function (array $matches) use ($entryName, $container) {
            /** @psalm-suppress InvalidCatch */
            try {
                // 获取占位符对应的容器条目
                return $container->get($matches[1]);
            } catch (NotFoundExceptionInterface $e) {
                // 如果条目未找到，抛出 DependencyException
                throw new DependencyException(sprintf(
                    "Error while parsing string expression for entry '%s': %s",
                    $entryName,
                    $e->getMessage()
                ), 0, $e);
            }
        };

        // 使用正则表达式查找并替换占位符
        $result = preg_replace_callback('#\{([^{}]+)}#', $callback, $expression);

        // 如果结果为 null，说明解析过程中出现了未知错误
        if ($result === null) {
            throw new \RuntimeException(sprintf('An unknown error occurred while parsing the string definition: \'%s\'', $expression));
        }

        // 返回解析后的字符串
        return $result;
    }

    // use Psr\Container\ContainerInterface;
    //use Ioc\Definition\StringDefinition;
    //
    //// 假设有一个容器实现
    //class MyContainer implements ContainerInterface {
    //    private array $services = [];
    //
    //    public function __construct() {
    //        // 注册一些服务
    //        $this->services['db_host'] = 'localhost';
    //        $this->services['db_user'] = 'root';
    //        $this->services['db_password'] = 'password';
    //    }
    //
    //    public function get($id) {
    //        if (!isset($this->services[$id])) {
    //            throw new NotFoundException("Service not found: $id");
    //        }
    //        return $this->services[$id];
    //    }
    //
    //    public function has($id) {
    //        return isset($this->services[$id]);
    //    }
    //}
    //
    //// 创建容器实例
    //$container = new MyContainer();
    //
    //// 定义一个字符串表达式，其中包含占位符
    //$expression = 'Database connection details: {db_host}, {db_user}, {db_password}';
    //
    //// 使用 `StringDefinition` 类
    //$stringDefinition = new StringDefinition($expression);
    //
    //// 使用 resolveExpression 方法解析表达式
    //try {
    //    $resolvedString = StringDefinition::resolveExpression('db_connection', $stringDefinition->getExpression(), $container);
    //    echo $resolvedString;  // 输出: Database connection details: localhost, root, password
    //} catch (Exception $e) {
    //    echo 'Error: ' . $e->getMessage();
    //}

}
