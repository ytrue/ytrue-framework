<?php

namespace Ioc\Compiler;

use ArrayIterator;
use Closure;
use InvalidArgumentException;
use Ioc\Definition\ArrayDefinition;
use Ioc\Definition\DecoratorDefinition;
use Ioc\Definition\Definition;
use Ioc\Definition\EnvironmentVariableDefinition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\FactoryDefinition;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\Reference;
use Ioc\Definition\Source\DefinitionSource;
use Ioc\Definition\StringDefinition;
use Ioc\Definition\ValueDefinition;
use Ioc\DependencyException;
use Ioc\Proxy\ProxyFactory;
use Laravel\SerializableClosure\Support\ReflectionClosure;
use ReflectionException;


/**
 * Compiler 类用于编译依赖注入容器。
 */
class Compiler
{
    // 容器类名
    private string $containerClass;

    // 容器父类名
    private string $containerParentClass;

    /**
     * 按条目名称索引的定义。
     * 键为字符串（条目名称），值为 `Definition` 对象或 null（如果定义需要动态获取）。
     */
    private ArrayIterator $entriesToCompile;

    /**
     * 定义的递增计数器。
     * 每个条目都被定义为 'SubEntry' + 计数器，
     * 如果 PHP-DI 配置不变，则在编译的容器中保持相同的键。
     */
    private int $subEntryCounter = 0;

    /**
     * `CompiledContainer` 中 get 方法的递增计数器。
     * 每个 `CompiledContainer` 方法名称都定义为 'get' + 计数器，
     * 并且如果 PHP-DI 配置不变，在每次重新编译后保持不变。
     */
    private int $methodMappingCounter = 0;

    /**
     * 条目名称到方法名称的映射。
     * 用于在编译过程中，记录条目与其对应的方法。
     *
     * @var string[]
     */
    private array $entryToMethodMapping = [];

    /**
     * 存储生成的每个方法的代码。
     *
     * @var string[]
     */
    private array $methods = [];

    // 自动注入功能是否启用的标志
    private bool $autowiringEnabled;

    /**
     * 构造函数，接收一个代理工厂类的实例。
     *
     * @param ProxyFactory $proxyFactory 用于处理依赖注入中代理对象的工厂。
     */
    public function __construct(
        private readonly ProxyFactory $proxyFactory,
    )
    {
    }

    /**
     * 获取 ProxyFactory 实例。
     *
     * @return ProxyFactory 返回当前的代理工厂实例。
     */
    public function getProxyFactory(): ProxyFactory
    {
        return $this->proxyFactory;
    }

    /**
     * 编译依赖注入容器，将定义生成并保存为 PHP 文件。
     *
     * @param DefinitionSource $definitionSource 定义的来源（可能是配置文件或其他来源）。
     * @param string $directory 文件保存的目录。
     * @param string $className 生成的容器类的类名。
     * @param string $parentClassName 容器的父类名。
     * @param bool $autowiringEnabled 是否启用自动注入（根据类的构造函数参数自动解决依赖）。
     * @return string 返回生成的容器文件名。
     * @throws DependencyException
     * @throws InvalidDefinition|ReflectionException
     */
    public function compile(
        DefinitionSource $definitionSource, // 定义源
        string           $directory,                 // 保存容器的目录
        string           $className,                 // 容器的类名
        string           $parentClassName,           // 容器的父类名
        bool             $autowiringEnabled            // 是否启用自动注入
    ): string
    {
        // 生成文件路径，确保目录末尾没有 '/'
        $fileName = rtrim($directory, '/') . '/' . $className . '.php';

        // 如果容器已经存在，则直接返回文件名
        if (file_exists($fileName)) {
            return $fileName;
        }

        // 设置自动注入标志
        $this->autowiringEnabled = $autowiringEnabled;

        // 验证类名是否合法（必须符合 PHP 的命名规则）
        $validClassName = preg_match('/^[a-zA-Z_][a-zA-Z0-9_]*$/', $className);
        if (!$validClassName) {
            throw new InvalidArgumentException("容器无法编译：`$className` 不是一个合法的PHP类名");
        }

        // 从定义源中获取所有定义并存储在迭代器中
        $this->entriesToCompile = new ArrayIterator($definitionSource->getDefinitions());

        // 遍历所有需要编译的定义
        foreach ($this->entriesToCompile as $entryName => $definition) {
            $silenceErrors = false; // 错误静默开关

            // 如果定义为空（表示可能是通过引用自动注入时发现的），则尝试获取该条目的定义
            if (!$definition) {
                $definition = $definitionSource->getDefinition($entryName);
                // 对于可能引用接口或抽象类的条目，不抛出错误，因为它们可能稍后被定义
                // 或者在运行时未实际使用，不阻止编译继续进行
                $silenceErrors = true;
            }

            // 如果定义仍然为空，则跳过此条目，因为可能在运行时动态定义
            if (!$definition) {
                continue;
            }

            // 检查定义是否可以被编译
            $errorMessage = $this->isCompilable($definition);
            if ($errorMessage !== true) {
                continue; // 如果定义不可编译，继续下一个条目
            }

            try {
                // 编译当前定义
                $this->compileDefinition($entryName, $definition);
            } catch (InvalidDefinition $e) {
                // 如果设置了错误静默，则跳过当前条目的编译
                if ($silenceErrors) {
                    unset($this->entryToMethodMapping[$entryName]);
                } else {
                    throw $e; // 如果没有静默，抛出异常
                }
            }
        }

        // 设置容器类名和父类名
        $this->containerClass = $className;
        $this->containerParentClass = $parentClassName;

        // 使用缓冲区捕获生成的 PHP 模板内容
        ob_start();
        require __DIR__ . '/Template.php'; // 引入模板文件
        $fileContent = ob_get_clean(); // 获取模板输出

        // 在文件开头添加 PHP 开始标签
        $fileContent = "<?php\n" . $fileContent;

        // 创建用于保存编译文件的目录
        $this->createCompilationDirectory(dirname($fileName));

        // 使用原子写入方式将文件内容写入到目标路径中
        $this->writeFileAtomic($fileName, $fileContent);

        // 返回生成的文件名
        return $fileName;
    }


    /**
     * 原子性地将内容写入文件。
     *
     * 该方法会创建一个临时文件，将内容写入该临时文件，
     * 然后将临时文件重命名为目标文件名，确保在写入过程中
     * 不会导致目标文件处于不完整的状态。
     *
     * @param string $fileName 目标文件的文件名。
     * @param string $content 要写入目标文件的内容。
     * @throws InvalidArgumentException 如果在创建临时文件、写入内容或重命名文件时发生错误。
     */
    private function writeFileAtomic(string $fileName, string $content): void
    {
        // 创建一个临时文件，临时文件将位于目标文件所在的目录中，文件名前缀为 'swap-compile'
        $tmpFile = @tempnam(dirname($fileName), 'swap-compile');
        if ($tmpFile === false) {
            // 如果临时文件创建失败，则抛出异常
            throw new InvalidArgumentException(
                sprintf('在 %s 创建临时文件时出错', dirname($fileName))
            );
        }

        // 设置临时文件的权限为 0666（可读可写）
        @chmod($tmpFile, 0666);

        // 将内容写入临时文件
        $written = file_put_contents($tmpFile, $content);
        if ($written === false) {
            // 如果写入失败，删除临时文件并抛出异常
            @unlink($tmpFile);

            throw new InvalidArgumentException(sprintf('写入 %s 时出错', $tmpFile));
        }

        // 再次设置临时文件的权限为 0666
        @chmod($tmpFile, 0666);

        // 尝试将临时文件重命名为目标文件名
        $renamed = @rename($tmpFile, $fileName);
        if (!$renamed) {
            // 如果重命名失败，删除临时文件并抛出异常
            @unlink($tmpFile);

            throw new InvalidArgumentException(sprintf('将 %s 重命名为 %s 时出错', $tmpFile, $fileName));
        }
    }


    /**
     * 编译给定的定义并生成一个唯一的方法名。
     *
     * @param string $entryName 定义的入口名称。
     * @param Definition $definition 需要编译的定义对象。
     *
     * @return string 生成的方法名。
     *
     * @throws DependencyException 当处理依赖时发生错误。
     * @throws InvalidDefinition 当定义无效时抛出异常。
     * @throws ReflectionException
     */
    private function compileDefinition(string $entryName, Definition $definition): string
    {
        // 生成一个唯一的方法名，例如 'get1', 'get2' 等
        $methodName = 'get' . (++$this->methodMappingCounter);
        $this->entryToMethodMapping[$entryName] = $methodName; // 记录入口名称与方法名的映射关系

        // 根据不同的定义类型进行相应的处理
        switch (true) {
            case $definition instanceof ValueDefinition:
                // 处理值定义
                $value = $definition->getValue(); // 获取值
                $code = 'return ' . $this->compileValue($value) . ';'; // 编译返回代码
                break;

            case $definition instanceof Reference:
                // 处理引用定义
                $targetEntryName = $definition->getTargetEntryName(); // 获取目标入口名称
                $code = 'return $this->delegateContainer->get(' . $this->compileValue($targetEntryName) . ');'; // 通过委托容器获取目标定义
                // 如果目标条目尚未编译，则添加到待编译列表中
                if (!isset($this->entriesToCompile[$targetEntryName])) {
                    $this->entriesToCompile[$targetEntryName] = null;
                }
                break;

            case $definition instanceof StringDefinition:
                // 处理字符串定义
                $entryName = $this->compileValue($definition->getName()); // 获取字符串定义的名称
                $expression = $this->compileValue($definition->getExpression()); // 获取表达式
                $code = 'return \Ioc\Definition\StringDefinition::resolveExpression(' . $entryName . ', ' . $expression . ', $this->delegateContainer);'; // 解析字符串表达式
                break;

            case $definition instanceof EnvironmentVariableDefinition:
                // 处理环境变量定义
                $variableName = $this->compileValue($definition->getVariableName()); // 获取变量名称
                $isOptional = $this->compileValue($definition->isOptional()); // 获取是否可选
                $defaultValue = $this->compileValue($definition->getDefaultValue()); // 获取默认值
                $code = <<<PHP
                \$value = \$_ENV[$variableName] ?? \$_SERVER[$variableName] ?? getenv($variableName);
                if (false !== \$value) return \$value; // 如果环境变量已定义则返回其值
                if (!$isOptional) {
                    throw new \Ioc\Definition\Exception\InvalidDefinition("环境变量 '{$definition->getVariableName()}' 未定义");
                }
                return $defaultValue; // 否则返回默认值
            PHP;
                break;

            case $definition instanceof ArrayDefinition:
                // 处理数组定义
                try {
                    $code = 'return ' . $this->compileValue($definition->getValues()) . ';'; // 编译返回数组值
                } catch (\Exception $e) {
                    throw new DependencyException(sprintf(
                        '编译 %s 时出错。 %s',
                        $definition->getName(),
                        $e->getMessage()
                    ), 0, $e);
                }
                break;

            case $definition instanceof ObjectDefinition:
                // 处理对象定义
                $compiler = new ObjectCreationCompiler($this);
                $code = $compiler->compile($definition); // 编译对象定义
                $code .= "\n        return \$object;"; // 返回创建的对象
                break;

            case $definition instanceof DecoratorDefinition:
                // 处理装饰器定义
                $decoratedDefinition = $definition->getDecoratedDefinition(); // 获取被装饰的定义
                if (!$decoratedDefinition instanceof Definition) {
                    if (!$definition->getName()) {
                        throw new InvalidDefinition('装饰器不能嵌套在另一个定义中');
                    }
                    throw new InvalidDefinition(sprintf(
                        '条目 "%s" 装饰了无定义：未找到先前的相同名称定义',
                        $definition->getName()
                    ));
                }
                // 返回装饰器调用结果
                $code = sprintf(
                    'return call_user_func(%s, %s, $this->delegateContainer);',
                    $this->compileValue($definition->getCallable()),
                    $this->compileValue($decoratedDefinition)
                );
                break;

            case $definition instanceof FactoryDefinition:
                // 处理工厂定义
                $value = $definition->getCallable(); // 获取可调用的值

                // 自定义错误消息帮助调试
                $isInvokableClass = is_string($value) && class_exists($value) && method_exists($value, '__invoke');
                if ($isInvokableClass && !$this->autowiringEnabled) {
                    throw new InvalidDefinition(sprintf(
                        '条目 "%s" 无法编译。 如果禁用容器的自动注入，无法自动解析可调用类，您需要启用自动注入或手动定义条目。',
                        $entryName
                    ));
                }

                $definitionParameters = '';
                if (!empty($definition->getParameters())) {
                    // 如果定义有参数，编译参数
                    $definitionParameters = ', ' . $this->compileValue($definition->getParameters());
                }

                $code = sprintf(
                    'return $this->resolveFactory(%s, %s%s);',
                    $this->compileValue($value), // 可调用值
                    var_export($entryName, true), // 入口名称
                    $definitionParameters // 传递参数
                );

                break;

            default:
                // 处理无法识别的定义类型（理论上不应该发生）
                throw new \Exception('无法编译类型为 ' . $definition::class . ' 的定义');
        }

        // 将生成的代码存储到方法列表中
        $this->methods[$methodName] = $code;

        return $methodName; // 返回生成的方法名
    }


    /**
     * 编译给定的值，并返回其对应的代码字符串。
     *
     * @param mixed $value 需要编译的值，可以是基本类型、数组、闭包或定义对象。
     *
     * @return string 编译后的值的代码表示。
     *
     * @throws DependencyException
     * @throws InvalidDefinition 当值无法编译时抛出异常。
     * @throws ReflectionException
     */
    public function compileValue(mixed $value): string
    {
        // 检查值是否可以被编译
        $errorMessage = $this->isCompilable($value);
        if ($errorMessage !== true) {
            throw new InvalidDefinition($errorMessage); // 如果无法编译，抛出异常
        }

        // 如果值是定义对象
        if ($value instanceof Definition) {
            // 生成一个任意的唯一名称
            $subEntryName = 'subEntry' . (++$this->subEntryCounter);
            // 在另一个方法中编译子定义
            $methodName = $this->compileDefinition($subEntryName, $value);

            // 值现在变成对该方法的调用（返回实际值）
            return "\$this->$methodName()"; // 返回方法调用的字符串表示
        }

        // 如果值是数组
        if (is_array($value)) {
            // 遍历数组，编译每个值
            $value = array_map(function ($value, $key) {
                $compiledValue = $this->compileValue($value); // 编译数组中的值
                $key = var_export($key, true); // 将键值转为可导出的字符串

                return "            $key => $compiledValue,\n"; // 返回键值对的字符串表示
            }, $value, array_keys($value));
            $value = implode('', $value); // 将所有键值对合并为一个字符串

            return "[\n$value        ]"; // 返回数组的字符串表示
        }

        // 如果值是闭包
        if ($value instanceof Closure) {
            return $this->compileClosure($value); // 编译闭包并返回其字符串表示
        }

        // 返回值的可导出字符串表示（例如，字符串、整数、布尔值等）
        return var_export($value, true);
    }


    /**
     * 创建编译目录，如果目录不存在则尝试创建它。
     *
     * @param string $directory 需要创建或验证的目录路径。
     *
     * @throws InvalidArgumentException 如果目录无法创建或不可写。
     */
    private function createCompilationDirectory(string $directory): void
    {
        // 如果目录不存在，并且无法创建目录，抛出异常
        if (!is_dir($directory) && !@mkdir($directory, 0777, true) && !is_dir($directory)) {
            throw new InvalidArgumentException(sprintf('Compilation directory does not exist and cannot be created: %s.', $directory));
        }

        // 检查目录是否可写
        if (!is_writable($directory)) {
            throw new InvalidArgumentException(sprintf('Compilation directory is not writable: %s.', $directory));
        }
    }

    /**
     * 检查给定的值是否可以编译。
     *
     * @param mixed $value 需要检查的值。
     *
     * @return string|true 如果返回true，表示值是可编译的；如果返回字符串，则是错误信息。
     */
    private function isCompilable(mixed $value): string|bool
    {
        // 如果值是 ValueDefinition，检查其内部值是否可编译
        if ($value instanceof ValueDefinition) {
            return $this->isCompilable($value->getValue());
        }

        // 如果值是 DecoratorDefinition 并且名称为空，返回错误信息
        if (($value instanceof DecoratorDefinition) && empty($value->getName())) {
            return 'Decorators cannot be nested in another definition';
        }

        // 所有其他类型的 Definition 都被认为是可编译的
        if ($value instanceof Definition) {
            return true;
        }

        // 如果值是 Closure，返回可编译
        if ($value instanceof Closure) {
            return true;
        }

        /** @psalm-suppress UndefinedClass */
        // 如果 PHP 版本大于或等于 8.1，检查是否为 UnitEnum 类型
        if ((\PHP_VERSION_ID >= 80100) && ($value instanceof \UnitEnum)) {
            return true;
        }

        // 如果值是对象，返回错误信息
        if (is_object($value)) {
            return 'An object was found but objects cannot be compiled';
        }

        // 如果值是资源，返回错误信息
        if (is_resource($value)) {
            return 'A resource was found but resources cannot be compiled';
        }

        // 对于其他类型，返回可编译
        return true;
    }


    /**
     * 编译给定的闭包为字符串代码。
     *
     * @param Closure $closure 需要编译的闭包。
     *
     * @return string 返回编译后的闭包代码。
     * @throws InvalidDefinition 如果闭包导入变量或需要绑定 $this。
     *
     */
    private function compileClosure(Closure $closure): string
    {
        // 使用反射获取闭包的详细信息
        $reflector = new ReflectionClosure($closure);

        // 检查闭包是否使用了 `use` 导入外部变量
        if ($reflector->getUseVariables()) {
            throw new InvalidDefinition('Cannot compile closures which import variables using the `use` keyword');
        }

        // 检查闭包是否需要绑定或引用 $this、self、static 或 parent
        if ($reflector->isBindingRequired() || $reflector->isScopeRequired()) {
            throw new InvalidDefinition('Cannot compile closures which use $this or self/static/parent references');
        }

        // 强制所有闭包为静态（添加 `static` 关键字），即不能使用 $this
        // 这很有意义，因为闭包的代码将被复制到另一个类中
        $code = ($reflector->isStatic() ? '' : 'static ') . $reflector->getCode();

        // 去除字符串前后的空白字符，并返回编译后的代码
        return trim($code, "\t\n\r;");
    }

}
