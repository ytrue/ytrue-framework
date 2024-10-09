<?php


namespace Ioc\Definition\Resolver;


use Invoker\Exception\InvocationException;
use Invoker\Exception\NotCallableException;
use Invoker\Exception\NotEnoughParametersException;
use Invoker\Invoker;
use Invoker\ParameterResolver\AssociativeArrayResolver;
use Invoker\ParameterResolver\DefaultValueResolver;
use Invoker\ParameterResolver\NumericArrayResolver;
use Invoker\ParameterResolver\ResolverChain;
use Ioc\Definition\Definition;
use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\FactoryDefinition;
use Ioc\Invoker\FactoryParameterResolver;
use Override;
use Psr\Container\ContainerInterface;

/**
 * 解析工厂定义为一个值。
 */
class FactoryResolver implements DefinitionResolver
{
    /**
     * @var Invoker|null 用于调用工厂的调用器对象
     */
    private ?Invoker $invoker = null;

    /**
     * 解析器需要一个容器。该容器会作为参数传递给工厂，
     * 使得工厂可以访问容器中的其他条目。
     *
     * @param ContainerInterface $container 依赖注入容器
     * @param DefinitionResolver $resolver 用于解析嵌套定义的解析器
     */
    public function __construct(
        private readonly ContainerInterface $container,
        private readonly DefinitionResolver $resolver,
    )
    {
    }

    // $f = new FactoryDefinition(
    //            'test',
    //            function ($ioc, $objDef) {
    //                print_r($ioc);
    //                print_r($objDef);
    //            },
    //            []
    //        );
    //
    //        $decoratorResolver = new FactoryResolver($container, $resolverDispatcher);
    //        $decoratorResolver->resolve($f);
    /**
     *
     * 解析工厂定义为一个值。
     *
     * 该方法将调用定义中的可调用项（工厂）。
     *
     * @param FactoryDefinition $definition 要解析的工厂定义
     * @param array $parameters 传递给工厂的参数（默认空数组）
     * @return mixed 工厂的返回值
     * @throws InvalidDefinition|InvocationException 如果工厂不可调用或参数不足，抛出该异常
     */
    #[Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {

        // 如果 invoker 为空，初始化调用器及参数解析链
        if (!$this->invoker) {
            $parameterResolver = new ResolverChain([
                new AssociativeArrayResolver,
                new FactoryParameterResolver($this->container),
                new NumericArrayResolver,
                new DefaultValueResolver,
            ]);

            // 创建调用器
            $this->invoker = new Invoker($parameterResolver, $this->container);
        }

        // 获取定义中的可调用工厂
        $callable = $definition->getCallable();

        try {
            // 初始参数：容器和定义对象
            $providedParams = [$this->container, $definition];
            // 解析额外参数
            $extraParams = $this->resolveExtraParams($definition->getParameters());
            // 合并额外参数与传递的参数
            $providedParams = array_merge($providedParams, $extraParams, $parameters);


            // 调用工厂并返回结果
            return $this->invoker->call($callable, $providedParams);
        } catch (NotCallableException $e) {
            // 如果工厂不可调用，抛出自定义异常信息
            if (is_string($callable) && class_exists($callable) && method_exists($callable, '__invoke')) {
                throw new InvalidDefinition(sprintf(
                    '条目 "%s" 无法解析：工厂 %s。可调用类无法在禁用自动装配的容器中自动解析，您需要启用自动装配或手动定义条目。',
                    $definition->getName(),
                    $e->getMessage()
                ));
            }

            throw new InvalidDefinition(sprintf(
                '条目 "%s" 无法解析：工厂 %s',
                $definition->getName(),
                $e->getMessage()
            ));
        } catch (NotEnoughParametersException $e) {
            throw new InvalidDefinition(sprintf(
                '条目 "%s" 无法解析：%s',
                $definition->getName(),
                $e->getMessage()
            ));
        }
    }

    /**
     * 检查定义是否可以被解析。
     *
     * @param Definition $definition 要检查的定义
     * @param array $parameters 传递的参数（默认空数组）
     * @return bool 返回 true 表示可以解析
     */
    #[Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        return true;
    }

    /**
     * 解析额外参数。
     *
     * @param array $params 要解析的参数数组
     * @return array 解析后的参数数组
     */
    private function resolveExtraParams(array $params): array
    {
        $resolved = [];
        foreach ($params as $key => $value) {
            // 如果参数是嵌套的定义，先解析
            if ($value instanceof Definition) {
                $value = $this->resolver->resolve($value);
            }
            $resolved[$key] = $value;
        }

        return $resolved;
    }
}
