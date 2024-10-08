<?php

namespace Ioc\Definition\Dumper;

use Ioc\Definition\Definition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\ObjectDefinition\MethodInjection;
use ReflectionException;
use ReflectionMethod;
use const PHP_EOL;

/**
 * Class ObjectDefinitionDumper
 *
 * 该类用于将 ObjectDefinition 的定义转化为字符串表示。
 * 它利用 PHP 反射机制来获取类及其构造函数、属性和方法的详细信息。
 */
class ObjectDefinitionDumper
{
    /**
     * 将 ObjectDefinition 转换为字符串表示。
     *
     * @param ObjectDefinition $definition 需要转换的对象定义
     * @return string 返回字符串表示
     * @throws ReflectionException 如果反射类失败
     */
    public function dump(ObjectDefinition $definition): string
    {
        $className = $definition->getClassName();
        // 检查类是否存在
        $classExist = class_exists($className) || interface_exists($className);

        // 处理类的存在性
        if (!$classExist) {
            $warning = '#UNKNOWN# '; // 类未知
        } else {
            $class = new \ReflectionClass($className);
            $warning = $class->isInstantiable() ? '' : '#NOT INSTANTIABLE# '; // 类不可实例化的警告
        }

        // 准备字符串表示的开头部分
        $str = sprintf('    class = %s%s', $warning, $className);

        // 懒加载属性
        $str .= PHP_EOL . '    lazy = ' . var_export($definition->isLazy(), true);

        // 如果类存在，继续处理构造函数、属性和方法
        if ($classExist) {
            $str .= $this->dumpConstructor($className, $definition);
            $str .= $this->dumpProperties($definition);
            $str .= $this->dumpMethods($className, $definition);
        }

        return sprintf('Object (' . PHP_EOL . '%s' . PHP_EOL . ')', $str);
    }

    /**
     * 转换构造函数的注入参数为字符串表示。
     *
     * @param string $className 类名
     * @param ObjectDefinition $definition 对象定义
     * @return string 返回构造函数的字符串表示
     * @throws ReflectionException 如果反射类失败
     */
    private function dumpConstructor(string $className, ObjectDefinition $definition): string
    {
        $str = '';
        $constructorInjection = $definition->getConstructorInjection();

        if ($constructorInjection !== null) {
            // 获取构造函数的参数
            $parameters = $this->dumpMethodParameters($className, $constructorInjection);
            $str .= sprintf(PHP_EOL . '    __construct(' . PHP_EOL . '        %s' . PHP_EOL . '    )', $parameters);
        }

        return $str;
    }

    /**
     * 转换属性注入为字符串表示。
     *
     * @param ObjectDefinition $definition 对象定义
     * @return string 返回属性注入的字符串表示
     */
    private function dumpProperties(ObjectDefinition $definition): string
    {
        $str = '';

        foreach ($definition->getPropertyInjections() as $propertyInjection) {
            $value = $propertyInjection->getValue();
            // 检查值的类型
            $valueStr = $value instanceof Definition ? (string)$value : var_export($value, true);
            $str .= sprintf(PHP_EOL . '    $%s = %s', $propertyInjection->getPropertyName(), $valueStr);
        }

        return $str;
    }

    /**
     * 转换方法注入为字符串表示。
     *
     * @param string $className 类名
     * @param ObjectDefinition $definition 对象定义
     * @return string 返回方法注入的字符串表示
     * @throws ReflectionException 如果反射类失败
     */
    private function dumpMethods(string $className, ObjectDefinition $definition): string
    {
        $str = '';

        foreach ($definition->getMethodInjections() as $methodInjection) {
            $parameters = $this->dumpMethodParameters($className, $methodInjection);
            $str .= sprintf(PHP_EOL . '    %s(' . PHP_EOL . '        %s' . PHP_EOL . '    )', $methodInjection->getMethodName(), $parameters);
        }

        return $str;
    }

    /**
     * 转换方法的参数为字符串表示。
     *
     * @param string $className 类名
     * @param MethodInjection $methodInjection 方法注入
     * @return string 返回方法参数的字符串表示
     * @throws ReflectionException 如果反射类失败
     */
    private function dumpMethodParameters(string $className, MethodInjection $methodInjection): string
    {
        $methodReflection = new ReflectionMethod($className, $methodInjection->getMethodName());
        $args = [];
        $definitionParameters = $methodInjection->getParameters();

        foreach ($methodReflection->getParameters() as $index => $parameter) {
            if (array_key_exists($index, $definitionParameters)) {
                $value = $definitionParameters[$index];
                $valueStr = $value instanceof Definition ? (string)$value : var_export($value, true);
                $args[] = sprintf('$%s = %s', $parameter->getName(), $valueStr);
                continue;
            }

            // 如果参数是可选的且未指定，取其默认值
            if ($parameter->isOptional()) {
                try {
                    $value = $parameter->getDefaultValue();
                    $args[] = sprintf(
                        '$%s = (default value) %s',
                        $parameter->getName(),
                        var_export($value, true)
                    );
                    continue;
                } catch (ReflectionException) {
                    // 处理反射无法读取默认值的异常
                }
            }

            $args[] = sprintf('$%s = #UNDEFINED#', $parameter->getName());
        }

        return implode(PHP_EOL . '        ', $args); // 返回参数的字符串表示
    }
}
