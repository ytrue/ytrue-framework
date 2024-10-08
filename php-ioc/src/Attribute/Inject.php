<?php


namespace Ioc\Attribute;

use Attribute;
use Ioc\Definition\Exception\InvalidAttribute;
use JsonException;
use const JSON_THROW_ON_ERROR;


/**
 * #[Inject] 属性。
 *
 * 将属性或方法标记为注入点。
 *
 */

/**
 * 该属性定义了 Inject 类的应用范围。
 *
 * 使用 Attribute::TARGET_PROPERTY、Attribute::TARGET_METHOD 和 Attribute::TARGET_PARAMETER
 * 常量，指定 Inject 属性可以应用于以下目标：
 *
 * - Attribute::TARGET_PROPERTY: 该属性可以用于类的属性。允许开发者在类的属性上使用
 *   #[Inject] 注解，指示该属性需要进行依赖注入。
 *
 * - Attribute::TARGET_METHOD: 该属性可以用于类的方法。允许开发者在类的方法上使用
 *   #[Inject] 注解，指示该方法需要进行依赖注入。
 *
 * - Attribute::TARGET_PARAMETER: 该属性可以用于方法参数。允许开发者在方法的参数
 *   上使用 #[Inject] 注解，指示该参数需要进行依赖注入。
 *
 * 通过这种方式，Inject 属性的灵活性得以增强，使得依赖注入可以在多个位置进行配置，
 */
#[Attribute(Attribute::TARGET_PROPERTY | Attribute::TARGET_METHOD | Attribute::TARGET_PARAMETER)]
final class Inject
{
    /**
     * 要注入的条目名称。
     */
    private ?string $name = null;

    /**
     * 参数数组，按参数编号（索引）或名称索引。
     *
     * 如果该属性应用于方法，则使用此参数。
     */
    private array $parameters = [];

    /**
     * @throws InvalidAttribute 如果参数不符合预期格式，则抛出异常。
     * @throws JsonException
     */
    public function __construct(string|array|null $name = null)
    {
        // 处理 #[Inject('foo')] 或 #[Inject(name: 'foo')]
        if (is_string($name)) {
            $this->name = $name;
        }

        // 处理 #[Inject([...])] 的情况，即用于方法的注入
        if (is_array($name)) {
            foreach ($name as $key => $value) {
                // 检查每个值是否为字符串
                if (!is_string($value)) {
                    throw new InvalidAttribute(sprintf(
                        "#[Inject(['param' => 'value'])] 期望 \"value\" 为字符串，%s 给定。",
                        json_encode($value, JSON_THROW_ON_ERROR)
                    ));
                }

                // 将参数存储在数组中
                $this->parameters[$key] = $value;
            }
        }
    }

    /**
     * 获取要注入的条目名称。
     *
     * @return string|null 要注入的条目名称
     */
    public function getName(): string|null
    {
        return $this->name;
    }

    /**
     * 获取参数数组，按参数编号（索引）或名称索引。
     *
     * @return array 参数数组
     */
    public function getParameters(): array
    {
        return $this->parameters;
    }
}
