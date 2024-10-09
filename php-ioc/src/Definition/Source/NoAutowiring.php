<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\ObjectDefinition;
use Override;

/**
 * Class NoAutowiring
 *
 * 实现 Autowiring 接口，禁用自动装配功能。
 */
class NoAutowiring implements Autowiring
{
    /**
     * @throws InvalidDefinition
     */
    #[Override] public function autowire(string $name, ?ObjectDefinition $definition = null): ObjectDefinition|null
    {
        throw new InvalidDefinition(sprintf(
            'Cannot autowire entry "%s" because autowiring is disabled',
            $name
        ));
    }
}
