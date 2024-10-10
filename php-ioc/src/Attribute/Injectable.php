<?php

namespace Ioc\Attribute;

use Attribute;

/**
 * 标记一个类为可注入的属性。
 *
 * 该属性用于指示某个类可以通过依赖注入容器进行实例化。
 *
 * @api
 */
#[Attribute(Attribute::TARGET_CLASS)]
final readonly class Injectable
{
    /**
     * 构造函数
     *
     * @param bool|null $lazy 指示对象是否应延迟加载。
     *  如果为 true，则对象在第一次使用时才会被实例化；
     *  如果为 false 或 null，则对象会在容器初始化时立即被创建。
     */
    public function __construct(
        private ?bool $lazy = null,
    )
    {
    }

    /**
     * 检查对象是否应延迟加载。
     *
     * @return bool|null 如果对象应延迟加载，返回 true；如果不应延迟加载，返回 false；
     * 如果未设置，返回 null。
     */
    public function isLazy(): bool|null
    {
        return $this->lazy;
    }
}
